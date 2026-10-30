import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { VoyageService } from '../../core/api.service';
import { NotifyService } from '../../core/notify.service';
import { Match, StatutMatch } from '../../core/models';
import { MatchCardComponent } from '../../shared/match-card.component';
import { SHARED } from '../../shared/shared';

/** Processus d'invitation aux voyages : invitations recues / envoyees par l'algorithme de matching. */
@Component({
  selector: 'app-invitations-voyage',
  imports: [SHARED, MatchCardComponent],
  template: `
    <div class="page">
      <div class="page-header">
        <div>
          <h1>Invitations de voyage</h1>
          <p>Lorsqu'un voyage compatible est détecté, une invitation est envoyée automatiquement.
            Acceptez-la pour échanger vos coordonnées et voyager ensemble.</p>
        </div>
        <mat-button-toggle-group [value]="filtre()" (change)="filtre.set($event.value)">
          <mat-button-toggle value="TOUS">Toutes</mat-button-toggle>
          <mat-button-toggle value="EN_ATTENTE">En attente</mat-button-toggle>
          <mat-button-toggle value="ACCEPTEE">Acceptées</mat-button-toggle>
          <mat-button-toggle value="REFUSEE">Refusées</mat-button-toggle>
        </mat-button-toggle-group>
      </div>

      @if (loading()) { <mat-progress-bar mode="indeterminate" class="mb" /> }

      <mat-tab-group animationDuration="150ms">
        <mat-tab [label]="'Reçues (' + aTraiter() + ' à traiter)'">
          <div class="grid grid-2 mt">
            @for (m of recuesFiltrees(); track m.id) {
              <app-match-card [match]="m" [peutRepondre]="true" (repondre)="repondre(m, $event)" />
            } @empty {
              <div class="empty"><mat-icon>inbox</mat-icon><p>Aucune invitation reçue.</p></div>
            }
          </div>
        </mat-tab>
        <mat-tab [label]="'Envoyées (' + envoyees().length + ')'">
          <div class="grid grid-2 mt">
            @for (m of envoyeesFiltrees(); track m.id) {
              <app-match-card [match]="m" />
            } @empty {
              <div class="empty"><mat-icon>outbox</mat-icon><p>Aucune invitation envoyée.</p></div>
            }
          </div>
        </mat-tab>
      </mat-tab-group>
    </div>
  `,
})
export class InvitationsVoyageComponent implements OnInit {
  private voyageService = inject(VoyageService);
  private notify = inject(NotifyService);

  readonly recues = signal<Match[]>([]);
  readonly envoyees = signal<Match[]>([]);
  readonly loading = signal(true);
  readonly filtre = signal<StatutMatch | 'TOUS'>('TOUS');

  readonly aTraiter = computed(() => this.recues().filter(m => m.statut === 'EN_ATTENTE').length);
  readonly recuesFiltrees = computed(() => this.filtrer(this.recues()));
  readonly envoyeesFiltrees = computed(() => this.filtrer(this.envoyees()));

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    forkJoin({ r: this.voyageService.invitationsRecues(), e: this.voyageService.invitationsEnvoyees() }).subscribe({
      next: ({ r, e }) => {
        this.recues.set(r);
        this.envoyees.set(e);
        this.loading.set(false);
      },
      error: err => {
        this.loading.set(false);
        this.notify.error(err);
      },
    });
  }

  repondre(m: Match, accepter: boolean): void {
    this.voyageService.repondre(m.id, accepter).subscribe({
      next: maj => {
        this.recues.update(list => list.map(x => (x.id === maj.id ? maj : x)));
        this.notify.success(accepter ? `Vous êtes en contact avec ${maj.partenaireNom} !` : 'Invitation refusée');
      },
      error: err => this.notify.error(err),
    });
  }

  private filtrer(list: Match[]): Match[] {
    const f = this.filtre();
    return f === 'TOUS' ? list : list.filter(m => m.statut === f);
  }
}
