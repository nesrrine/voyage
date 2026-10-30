import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ForumService, VoyageService } from './api.service';
import { API_URL } from './labels';

describe('VoyageService / ForumService', () => {
  let http: HttpTestingController;
  let voyages: VoyageService;
  let forum: ForumService;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    voyages = TestBed.inject(VoyageService);
    forum = TestBed.inject(ForumService);
  });

  afterEach(() => http.verify());

  it('n\'envoie que les filtres renseignes pour les programmes', () => {
    voyages.programmes({ destination: 'Paris', objet: null, domaine: 'FINANCE', du: '', au: undefined }).subscribe();

    const req = http.expectOne(r => r.url === `${API_URL}/voyages/programmes`);
    expect(req.request.params.keys().sort()).toEqual(['destination', 'domaine']);
    expect(req.request.params.get('domaine')).toBe('FINANCE');
    req.flush([]);
  });

  it('ne charge le referentiel des villes qu\'une seule fois (shareReplay)', () => {
    const recues: string[] = [];
    voyages.villes().subscribe(v => recues.push(v[0].nom));
    voyages.villes().subscribe(v => recues.push(v[0].nom));

    // expectOne echoue si deux requetes HTTP partent
    http.expectOne(`${API_URL}/meta/villes`).flush([{ id: 1, nom: 'Paris', pays: 'France', latitude: 48.85, longitude: 2.35 }]);
    expect(recues).toEqual(['Paris', 'Paris']);
  });

  it('envoie la reponse a une invitation de voyage sur la bonne route', () => {
    voyages.repondre(7, true).subscribe();
    voyages.repondre(8, false).subscribe();

    expect(http.expectOne(`${API_URL}/matchs/7/accepter`).request.method).toBe('POST');
    expect(http.expectOne(`${API_URL}/matchs/8/refuser`).request.method).toBe('POST');
  });

  it('envoie la reaction Like / Dislike du forum', () => {
    forum.reagir(3, 'DISLIKE').subscribe();

    const req = http.expectOne(`${API_URL}/forum/posts/3/reaction`);
    expect(req.request.body).toEqual({ type: 'DISLIKE' });
  });
});
