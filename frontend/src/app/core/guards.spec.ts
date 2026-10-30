import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, provideRouter, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { authGuard, guestGuard, roleGuard } from './guards';
import { AuthService } from './auth.service';
import { Role } from './models';

describe('guards', () => {
  let connecte: boolean;
  let role: Role | null;

  beforeEach(() => {
    const auth: Partial<AuthService> = {
      isLoggedIn: (() => connecte) as AuthService['isLoggedIn'],
      hasRole: (...roles: Role[]) => role !== null && roles.includes(role),
    };
    TestBed.configureTestingModule({ providers: [provideRouter([]), { provide: AuthService, useValue: auth }] });
  });

  const etat = (url: string) => ({ url }) as RouterStateSnapshot;
  const route = (roles: Role[]) => ({ data: { roles } }) as unknown as ActivatedRouteSnapshot;
  const executer = (garde: typeof authGuard, r: ActivatedRouteSnapshot, s: RouterStateSnapshot) =>
    TestBed.runInInjectionContext(() => garde(r, s));
  const url = (resultat: unknown) => TestBed.inject(Router).serializeUrl(resultat as UrlTree);

  it('authGuard redirige vers la connexion en gardant la page demandee', () => {
    connecte = false;
    expect(url(executer(authGuard, route([]), etat('/voyages/3')))).toBe('/login?returnUrl=%2Fvoyages%2F3');
  });

  it('authGuard laisse passer un utilisateur connecte', () => {
    connecte = true;
    expect(executer(authGuard, route([]), etat('/voyages'))).toBeTrue();
  });

  it('guestGuard renvoie un utilisateur connecte vers le tableau de bord', () => {
    connecte = true;
    expect(url(executer(guestGuard, route([]), etat('/login')))).toBe('/dashboard');
  });

  it('roleGuard autorise uniquement les roles declares par la route', () => {
    role = 'ENTREPRISE';
    expect(executer(roleGuard, route(['ENTREPRISE']), etat('/employes'))).toBeTrue();

    role = 'EMPLOYE';
    expect(url(executer(roleGuard, route(['ENTREPRISE']), etat('/employes')))).toBe('/dashboard');
  });
});
