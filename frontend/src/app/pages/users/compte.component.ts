import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { AuthService } from '../../core/auth.service';
import { UserService } from '../../core/api.service';
import { NotifyService } from '../../core/notify.service';
import { DOMAINE_KEYS, DOMAINES, ROLES } from '../../core/labels';
import { DomaineActivite, Entreprise } from '../../core/models';
import { confirmer } from '../../shared/confirm-dialog.component';
import { SHARED } from '../../shared/shared';

/** Gestion du compte : profil, mot de passe, informations entreprise, desactivation. */
@Component({
  selector: 'app-compte',
  imports: [SHARED],
  templateUrl: './compte.component.html',
})
export class CompteComponent implements OnInit {
  auth = inject(AuthService);
  private fb = inject(FormBuilder);
  private userService = inject(UserService);
  private notify = inject(NotifyService);
  private dialog = inject(MatDialog);

  readonly roles = ROLES;
  readonly domaines = DOMAINE_KEYS;
  readonly libellesDomaines = DOMAINES;
  readonly entreprise = signal<Entreprise | null>(null);

  profil = this.fb.nonNullable.group({
    prenom: ['', Validators.required],
    nom: ['', Validators.required],
    telephone: [''],
    poste: [''],
  });

  motDePasse = this.fb.nonNullable.group({
    ancien: ['', Validators.required],
    nouveau: ['', [Validators.required, Validators.minLength(6)]],
  });

  entrepriseForm = this.fb.nonNullable.group({
    nom: ['', Validators.required],
    domaineActivite: ['INFORMATIQUE' as DomaineActivite, Validators.required],
    pays: ['', Validators.required],
    ville: ['', Validators.required],
    adresse: [''],
    telephone: [''],
    siteWeb: [''],
    description: [''],
  });

  ngOnInit(): void {
    this.userService.moi().subscribe(u => {
      this.auth.updateUser(u);
      this.profil.patchValue({ prenom: u.prenom, nom: u.nom, telephone: u.telephone ?? '', poste: u.poste ?? '' });
    });
    if (!this.auth.hasRole('ADMIN')) {
      this.userService.monEntreprise().subscribe(e => {
        this.entreprise.set(e);
        this.entrepriseForm.patchValue({
          nom: e.nom, domaineActivite: e.domaineActivite, pays: e.pays, ville: e.ville,
          adresse: e.adresse ?? '', telephone: e.telephone ?? '', siteWeb: e.siteWeb ?? '', description: e.description ?? '',
        });
        if (!this.auth.hasRole('ENTREPRISE')) {
          this.entrepriseForm.disable();
        }
      });
    }
  }

  enregistrerProfil(): void {
    if (this.profil.invalid) {
      return;
    }
    const p = this.profil.getRawValue();
    this.userService.modifierProfil({ ...p, telephone: p.telephone || null, poste: p.poste || null }).subscribe({
      next: res => {
        this.auth.ouvrirSession(res);
        this.notify.success('Profil mis à jour');
      },
      error: err => this.notify.error(err),
    });
  }

  changerMotDePasse(): void {
    if (this.motDePasse.invalid) {
      this.motDePasse.markAllAsTouched();
      return;
    }
    const { ancien, nouveau } = this.motDePasse.getRawValue();
    this.userService.changerMotDePasse(ancien, nouveau).subscribe({
      next: () => {
        this.motDePasse.reset();
        this.notify.success('Mot de passe modifié');
      },
      error: err => this.notify.error(err),
    });
  }

  enregistrerEntreprise(): void {
    if (this.entrepriseForm.invalid) {
      return;
    }
    this.userService.modifierEntreprise(this.entrepriseForm.getRawValue()).subscribe({
      next: res => {
        this.auth.ouvrirSession(res);
        this.userService.monEntreprise().subscribe(e => this.entreprise.set(e));
        this.notify.success('Informations de l\'entreprise mises à jour');
      },
      error: err => this.notify.error(err),
    });
  }

  desactiver(): void {
    confirmer(this.dialog, {
      titre: 'Désactiver mon compte',
      message: 'Votre compte sera désactivé et vous ne pourrez plus vous connecter. Continuer ?',
      confirmer: 'Désactiver',
      danger: true,
    }).subscribe(ok => {
      if (ok) {
        this.userService.desactiverCompte().subscribe({
          next: () => {
            this.notify.success('Compte désactivé');
            this.auth.logout();
          },
          error: err => this.notify.error(err),
        });
      }
    });
  }
}
