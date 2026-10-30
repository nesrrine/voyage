import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { Clipboard } from '@angular/cdk/clipboard';
import { MatDialog } from '@angular/material/dialog';
import { UserService } from '../../core/api.service';
import { NotifyService } from '../../core/notify.service';
import { STATUTS } from '../../core/labels';
import { Invitation, User } from '../../core/models';
import { confirmer } from '../../shared/confirm-dialog.component';
import { SHARED } from '../../shared/shared';

/** Gestion des employes et des invitations a rejoindre l'application (role ENTREPRISE). */
@Component({
  selector: 'app-employes',
  imports: [SHARED],
  templateUrl: './employes.component.html',
})
export class EmployesComponent implements OnInit {
  private fb = inject(FormBuilder);
  private userService = inject(UserService);
  private notify = inject(NotifyService);
  private clipboard = inject(Clipboard);
  private dialog = inject(MatDialog);

  readonly statuts = STATUTS;
  readonly employes = signal<User[]>([]);
  readonly invitations = signal<Invitation[]>([]);
  readonly envoi = signal(false);
  readonly dernierLien = signal<string | null>(null);

  readonly colonnesEmployes = ['nom', 'email', 'poste', 'dateCreation', 'actif'];
  readonly colonnesInvitations = ['email', 'poste', 'statut', 'dateExpiration', 'actions'];

  form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    poste: [''],
  });

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.userService.employes().subscribe({ next: e => this.employes.set(e), error: err => this.notify.error(err) });
    this.userService.invitations().subscribe({ next: i => this.invitations.set(i), error: err => this.notify.error(err) });
  }

  inviter(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.envoi.set(true);
    const { email, poste } = this.form.getRawValue();
    this.userService.inviter(email, poste || null).subscribe({
      next: inv => {
        this.envoi.set(false);
        this.form.reset();
        this.dernierLien.set(inv.lien);
        this.notify.success(`Invitation créée pour ${inv.email}`);
        this.charger();
      },
      error: err => {
        this.envoi.set(false);
        this.notify.error(err);
      },
    });
  }

  copier(lien: string | null): void {
    if (lien && this.clipboard.copy(lien)) {
      this.notify.success('Lien d\'invitation copié dans le presse-papiers');
    }
  }

  renouveler(inv: Invitation): void {
    this.userService.renouvelerInvitation(inv.id).subscribe({
      next: maj => {
        this.dernierLien.set(maj.lien);
        this.notify.success('Invitation renouvelée pour 7 jours');
        this.charger();
      },
      error: err => this.notify.error(err),
    });
  }

  annuler(inv: Invitation): void {
    confirmer(this.dialog, { titre: 'Annuler l\'invitation', message: `Annuler l'invitation envoyée à ${inv.email} ?`, danger: true })
      .subscribe(ok => {
        if (ok) {
          this.userService.annulerInvitation(inv.id).subscribe({
            next: () => {
              this.notify.success('Invitation annulée');
              this.charger();
            },
            error: err => this.notify.error(err),
          });
        }
      });
  }

  basculer(e: User): void {
    this.userService.basculerEmploye(e.id).subscribe({
      next: maj => {
        this.employes.update(list => list.map(x => (x.id === maj.id ? maj : x)));
        this.notify.success(`${maj.prenom} ${maj.nom} ${maj.actif ? 'réactivé' : 'désactivé'}`);
      },
      error: err => this.notify.error(err),
    });
  }
}
