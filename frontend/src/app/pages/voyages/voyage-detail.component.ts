import { Component, computed, inject, input, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { AuthService } from '../../core/auth.service';
import { VoyageService } from '../../core/api.service';
import { NotifyService } from '../../core/notify.service';
import { STATUTS } from '../../core/labels';
import { Match, Voyage } from '../../core/models';
import { confirmer } from '../../shared/confirm-dialog.component';
import { MatchCardComponent } from '../../shared/match-card.component';
import { SHARED } from '../../shared/shared';

@Component({
  selector: 'app-voyage-detail',
  imports: [SHARED, MatchCardComponent],
  templateUrl: './voyage-detail.component.html',
  styles: `
    .info { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 16px; margin-top: 16px; }
    .info div { display: flex; gap: 10px; align-items: flex-start; }
    .info mat-icon { color: var(--va-primary); }
    .info small { display: block; color: var(--va-muted); }
    .chips { display: flex; gap: 6px; flex-wrap: wrap; }
  `,
})
export class VoyageDetailComponent implements OnInit {
  auth = inject(AuthService);
  private voyageService = inject(VoyageService);
  private notify = inject(NotifyService);
  private router = inject(Router);
  private dialog = inject(MatDialog);

  readonly id = input.required<string>();
  readonly statuts = STATUTS;
  readonly voyage = signal<Voyage | null>(null);
  readonly matchs = signal<Match[] | null>(null);
  readonly recherche = signal(false);

  /** Le proprietaire du voyage ou son entreprise peuvent le gerer et voir ses partenaires. */
  readonly gerable = computed(() => {
    const v = this.voyage();
    const u = this.auth.user();
    if (!v || !u) {
      return false;
    }
    return v.employeId === u.id || (u.role === 'ENTREPRISE' && v.entrepriseId === u.entrepriseId) || u.role === 'ADMIN';
  });

  readonly estProprietaire = computed(() => this.voyage()?.employeId === this.auth.user()?.id);

  ngOnInit(): void {
    this.voyageService.get(+this.id()).subscribe({
      next: v => {
        this.voyage.set(v);
        if (this.gerable()) {
          this.chargerMatchs();
        }
      },
      error: err => {
        this.notify.error(err);
        this.router.navigate(['/voyages']);
      },
    });
  }

  chargerMatchs(): void {
    this.voyageService.matchs(+this.id()).subscribe({
      next: m => this.matchs.set(m),
      error: err => this.notify.error(err),
    });
  }

  relancer(): void {
    this.recherche.set(true);
    this.voyageService.relancerMatching(+this.id()).subscribe({
      next: r => {
        this.recherche.set(false);
        this.notify.success(r.nouveauxMatchs ? `${r.nouveauxMatchs} nouveau(x) partenaire(s) trouvé(s) !` : 'Aucun nouveau partenaire pour le moment');
        this.chargerMatchs();
      },
      error: err => {
        this.recherche.set(false);
        this.notify.error(err);
      },
    });
  }

  repondre(m: Match, accepter: boolean): void {
    this.voyageService.repondre(m.id, accepter).subscribe({
      next: () => {
        this.notify.success(accepter ? 'Invitation acceptée : vous êtes maintenant en contact !' : 'Invitation refusée');
        this.chargerMatchs();
      },
      error: err => this.notify.error(err),
    });
  }

  supprimer(): void {
    const v = this.voyage();
    if (!v) {
      return;
    }
    confirmer(this.dialog, { titre: 'Supprimer le voyage', message: `Supprimer « ${v.titre} » ?`, confirmer: 'Supprimer', danger: true })
      .subscribe(ok => {
        if (ok) {
          this.voyageService.supprimer(v.id).subscribe({
            next: () => {
              this.notify.success('Voyage supprimé');
              this.router.navigate(['/voyages']);
            },
            error: err => this.notify.error(err),
          });
        }
      });
  }
}
