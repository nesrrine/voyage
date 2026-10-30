import { Component, inject, input, OnInit, signal } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { NotifyService } from '../../core/notify.service';
import { InvitationInfo } from '../../core/models';
import { SHARED } from '../../shared/shared';
import { motsDePasseIdentiques } from './register-entreprise.component';

/** Inscription d'un employe a partir du lien d'invitation envoye par son entreprise. */
@Component({
  selector: 'app-register-employe',
  imports: [SHARED],
  templateUrl: './register-employe.component.html',
  styleUrl: './auth.scss',
})
export class RegisterEmployeComponent implements OnInit {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);
  private notify = inject(NotifyService);

  /** Lie au parametre ?token= de l'URL (withComponentInputBinding). */
  readonly token = input<string>();

  readonly invitation = signal<InvitationInfo | null>(null);
  readonly erreur = signal<string | null>(null);
  readonly loading = signal(false);

  form = this.fb.nonNullable.group({
    prenom: ['', Validators.required],
    nom: ['', Validators.required],
    telephone: [''],
    poste: [''],
    password: ['', [Validators.required, Validators.minLength(6)]],
    confirmation: ['', Validators.required],
  }, { validators: motsDePasseIdentiques });

  ngOnInit(): void {
    const token = this.token();
    if (!token) {
      this.erreur.set('Lien d\'invitation incomplet.');
      return;
    }
    this.auth.invitationInfo(token).subscribe({
      next: info => {
        this.invitation.set(info);
        this.form.patchValue({ poste: info.poste ?? '' });
      },
      error: err => this.erreur.set(NotifyService.message(err, 'Invitation invalide')),
    });
  }

  submit(): void {
    const token = this.token();
    if (this.form.invalid || !token) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading.set(true);
    const { confirmation: _ignore, ...valeurs } = this.form.getRawValue();
    this.auth.registerEmploye({ ...valeurs, token }).subscribe({
      next: res => {
        this.notify.success(`Bienvenue chez ${res.user.entrepriseNom}, ${res.user.prenom} !`);
        this.router.navigate(['/voyages/nouveau']);
      },
      error: err => {
        this.loading.set(false);
        this.notify.error(err);
      },
    });
  }
}
