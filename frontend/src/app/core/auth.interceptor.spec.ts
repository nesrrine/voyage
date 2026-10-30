import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';
import { API_URL } from './labels';

describe('authInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let auth: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    auth = jasmine.createSpyObj<AuthService>('AuthService', ['logout'], { token: 'jeton-123' });
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: auth },
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('ajoute le jeton JWT aux appels de l\'API (gateway)', () => {
    http.get(`${API_URL}/voyages`).subscribe();
    const req = backend.expectOne(`${API_URL}/voyages`);
    expect(req.request.headers.get('Authorization')).toBe('Bearer jeton-123');
    req.flush([]);
  });

  it('n\'envoie pas le jeton a un autre domaine', () => {
    http.get('https://fonts.googleapis.com/css').subscribe();
    const req = backend.expectOne('https://fonts.googleapis.com/css');
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush('');
  });

  it('deconnecte l\'utilisateur quand la gateway repond 401 (jeton expire)', () => {
    http.get(`${API_URL}/voyages`).subscribe({ error: () => undefined });
    backend.expectOne(`${API_URL}/voyages`).flush({ message: 'Jeton invalide ou expire' }, { status: 401, statusText: 'Unauthorized' });
    expect(auth.logout).toHaveBeenCalled();
  });

  it('ne deconnecte pas sur un echec de connexion (mauvais mot de passe)', () => {
    http.post(`${API_URL}/auth/login`, {}).subscribe({ error: () => undefined });
    backend.expectOne(`${API_URL}/auth/login`).flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(auth.logout).not.toHaveBeenCalled();
  });

  it('ne deconnecte pas sur un 403 (droits insuffisants)', () => {
    http.get(`${API_URL}/admin/users`).subscribe({ error: () => undefined });
    backend.expectOne(`${API_URL}/admin/users`).flush({}, { status: 403, statusText: 'Forbidden' });
    expect(auth.logout).not.toHaveBeenCalled();
  });
});
