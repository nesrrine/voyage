import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { API_URL } from './labels';
import { AMINE, jetonFactice, reponseConnexion } from '../testing/fixtures';

describe('AuthService', () => {
  let http: HttpTestingController;

  function creer(): AuthService {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    return TestBed.inject(AuthService);
  }

  beforeEach(() => localStorage.clear());
  afterEach(() => http?.verify());

  it('ouvre la session apres une connexion reussie', () => {
    const auth = creer();
    auth.login('amine@techsoft.tn', 'password123').subscribe();

    const req = http.expectOne(`${API_URL}/auth/login`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'amine@techsoft.tn', password: 'password123' });
    req.flush(reponseConnexion());

    expect(auth.isLoggedIn()).toBeTrue();
    expect(auth.role()).toBe('EMPLOYE');
    expect(auth.token).toBeTruthy();
    expect(auth.hasRole('EMPLOYE', 'ENTREPRISE')).toBeTrue();
    expect(auth.hasRole('ADMIN')).toBeFalse();
  });

  it('restaure la session au demarrage si le jeton est encore valide', () => {
    localStorage.setItem('va_token', jetonFactice(3600));
    localStorage.setItem('va_user', JSON.stringify(AMINE));

    expect(creer().user()?.prenom).toBe('Amine');
  });

  it('ignore une session dont le jeton a expire', () => {
    localStorage.setItem('va_token', jetonFactice(-60));
    localStorage.setItem('va_user', JSON.stringify(AMINE));

    const auth = creer();
    expect(auth.isLoggedIn()).toBeFalse();
    expect(localStorage.getItem('va_token')).toBeNull();
  });

  it('remplace le jeton apres une modification du profil (nouveau JWT du user-service)', () => {
    const auth = creer();
    const nouveau = reponseConnexion({ ...AMINE, poste: 'Directeur technique' });

    auth.ouvrirSession(nouveau);

    expect(auth.token).toBe(nouveau.token);
    expect(auth.user()?.poste).toBe('Directeur technique');
  });

  it('vide la session et revient a la connexion lors de la deconnexion', () => {
    const auth = creer();
    auth.ouvrirSession(reponseConnexion());
    const router = TestBed.inject(Router);
    spyOn(router, 'navigate');

    auth.logout();

    expect(auth.isLoggedIn()).toBeFalse();
    expect(localStorage.getItem('va_token')).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });
});
