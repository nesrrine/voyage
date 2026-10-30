import { Component, inject, signal } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { NotifyService } from '../../core/notify.service';
import { SHARED } from '../../shared/shared';

@Component({
  selector: 'app-login',
  imports: [SHARED],
  templateUrl: './login.component.html',
  styleUrl: './auth.scss',
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private notify = inject(NotifyService);

  readonly loading = signal(false);
  readonly erreur = signal<string | null>(null);
  readonly masquer = signal(true);

  /** Comptes du jeu de donnees de demonstration (cf. DataInitializer cote backend). */
  readonly demo = [
    { label: 'Admin', email: 'admin@voyages-affaires.com', password: 'admin123' },
    { label: 'Entreprise TechSoft', email: 'contact@techsoft.tn', password: 'password123' },
    { label: 'Employé Amine', email: 'amine@techsoft.tn', password: 'password123' },
    { label: 'Employé Youssef', email: 'youssef@greenpower.tn', password: 'password123' },
    { label: 'Employé Karim', email: 'karim@finconseil.fr', password: 'password123' },
  ];

  form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  remplir(email: string, password: string): void {
    this.form.setValue({ email, password });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    this.erreur.set(null);
    const { email, password } = this.form.getRawValue();
    this.auth.login(email, password).subscribe({
      next: res => {
        this.notify.success(`Bienvenue ${res.user.prenom} !`);
        this.router.navigateByUrl(this.route.snapshot.queryParamMap.get('returnUrl') ?? '/dashboard');
      },
      error: err => {
        this.loading.set(false);
        this.erreur.set(NotifyService.message(err, 'Connexion impossible'));
      },
    });
  }
}
