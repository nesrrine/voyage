import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { LoginComponent } from './login.component';
import { AuthService } from '../../core/auth.service';
import { NotifyService } from '../../core/notify.service';
import { reponseConnexion } from '../../testing/fixtures';

describe('LoginComponent', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let composant: LoginComponent;
  let auth: jasmine.SpyObj<AuthService>;
  let router: Router;

  beforeEach(() => {
    auth = jasmine.createSpyObj<AuthService>('AuthService', ['login']);
    TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideNoopAnimations(), provideRouter([]),
        { provide: AuthService, useValue: auth },
        { provide: NotifyService, useValue: jasmine.createSpyObj('NotifyService', ['success', 'error']) },
      ],
    });
    fixture = TestBed.createComponent(LoginComponent);
    composant = fixture.componentInstance;
    router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl').and.resolveTo(true);
    fixture.detectChanges();
  });

  it('n\'appelle pas l\'API si le formulaire est invalide', () => {
    composant.form.setValue({ email: 'pas-un-email', password: '' });
    composant.submit();

    expect(auth.login).not.toHaveBeenCalled();
    expect(composant.form.controls.email.hasError('email')).toBeTrue();
  });

  it('remplit le formulaire avec un compte de demonstration', () => {
    composant.remplir('contact@techsoft.tn', 'password123');
    expect(composant.form.getRawValue()).toEqual({ email: 'contact@techsoft.tn', password: 'password123' });
  });

  it('connecte l\'utilisateur et ouvre le tableau de bord', () => {
    auth.login.and.returnValue(of(reponseConnexion()));
    composant.remplir('amine@techsoft.tn', 'password123');

    composant.submit();

    expect(auth.login).toHaveBeenCalledWith('amine@techsoft.tn', 'password123');
    expect(router.navigateByUrl).toHaveBeenCalledWith('/dashboard');
  });

  it('affiche le message d\'erreur renvoye par l\'API', () => {
    auth.login.and.returnValue(throwError(() =>
      new HttpErrorResponse({ status: 401, error: { message: 'Email ou mot de passe incorrect' } })));
    composant.remplir('amine@techsoft.tn', 'faux');

    composant.submit();
    fixture.detectChanges();

    expect(composant.erreur()).toBe('Email ou mot de passe incorrect');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Email ou mot de passe incorrect');
    expect(composant.loading()).toBeFalse();
  });
});
