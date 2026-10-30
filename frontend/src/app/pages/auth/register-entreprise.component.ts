import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ValidationErrors, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatStepperModule } from '@angular/material/stepper';
import { AuthService } from '../../core/auth.service';
import { NotifyService } from '../../core/notify.service';
import { DOMAINE_KEYS, DOMAINES } from '../../core/labels';
import { DomaineActivite } from '../../core/models';
import { SHARED } from '../../shared/shared';

export function motsDePasseIdentiques(group: AbstractControl): ValidationErrors | null {
  const p = group.get('password')?.value;
  const c = group.get('confirmation')?.value;
  return p && c && p !== c ? { differents: true } : null;
}

@Component({
  selector: 'app-register-entreprise',
  imports: [SHARED, MatStepperModule],
  templateUrl: './register-entreprise.component.html',
  styleUrl: './auth.scss',
})
export class RegisterEntrepriseComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);
  private notify = inject(NotifyService);

  readonly domaines = DOMAINE_KEYS;
  readonly libellesDomaines = DOMAINES;
  readonly loading = signal(false);

  entreprise = this.fb.nonNullable.group({
    nomEntreprise: ['', Validators.required],
    domaineActivite: ['' as DomaineActivite | '', Validators.required],
    pays: ['Tunisie', Validators.required],
    ville: ['', Validators.required],
    adresse: [''],
    telephone: [''],
    siteWeb: [''],
    description: ['', Validators.maxLength(2000)],
  });

  responsable = this.fb.nonNullable.group({
    prenom: ['', Validators.required],
    nom: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    confirmation: ['', Validators.required],
  }, { validators: motsDePasseIdentiques });

  submit(): void {
    if (this.entreprise.invalid || this.responsable.invalid) {
      this.entreprise.markAllAsTouched();
      this.responsable.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    const e = this.entreprise.getRawValue();
    const { confirmation: _ignore, ...r } = this.responsable.getRawValue();
    this.auth.registerEntreprise({ ...e, ...r, domaineActivite: e.domaineActivite as DomaineActivite }).subscribe({
      next: () => {
        this.notify.success('Entreprise inscrite ! Invitez maintenant vos employés.');
        this.router.navigate(['/employes']);
      },
      error: err => {
        this.loading.set(false);
        this.notify.error(err);
      },
    });
  }
}
