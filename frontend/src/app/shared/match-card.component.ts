import { Component, inject, input, output, signal } from '@angular/core';
import { Clipboard } from '@angular/cdk/clipboard';
import { AiService } from '../core/api.service';
import { NotifyService } from '../core/notify.service';
import { DatePipe, DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Match } from '../core/models';
import { STATUTS } from '../core/labels';
import { LibellePipe } from './libelle.pipe';

/** Carte d'une invitation de voyage : partenaire, detail du score de matching et actions. */
@Component({
  selector: 'app-match-card',
  imports: [DatePipe, DecimalPipe, RouterLink, MatCardModule, MatButtonModule, MatIconModule,
    MatProgressBarModule, MatTooltipModule, LibellePipe],
  template: `
    @let m = match();
    <mat-card class="card match" appearance="outlined">
      <div class="head">
        <div class="score" [class.high]="m.score >= 75" [class.mid]="m.score >= 55 && m.score < 75"
             matTooltip="Score de compatibilité">{{ m.score | number: '1.0-0' }}<small>%</small></div>
        <div class="who">
          <strong>{{ m.partenaireNom }}</strong>
          <span class="muted small">{{ m.partenairePoste }} · {{ m.partenaireEntreprise }}</span>
          <span class="badge purple">{{ m.partenaireDomaine | libelle }}</span>
        </div>
        <span class="badge {{ statuts[m.statut].color }}">{{ statuts[m.statut].label }}</span>
      </div>

      <div class="trip">
        <mat-icon>flight</mat-icon>
        <div>
          <div><strong>{{ m.voyagePartenaire.titre }}</strong> — {{ m.voyagePartenaire.villeDestination }}</div>
          <div class="muted small">
            {{ m.voyagePartenaire.dateDebut | date: 'dd MMM' }} → {{ m.voyagePartenaire.dateFin | date: 'dd MMM yyyy' }}
            · {{ m.voyagePartenaire.objet | libelle }}
          </div>
          @if (afficherMonVoyage()) {
            <div class="muted small">
              Votre voyage : <a [routerLink]="['/voyages', m.monVoyage.id]">{{ m.monVoyage.titre }}</a>
            </div>
          }
        </div>
      </div>

      <div class="scores">
        <div class="score-line"><span>Domaine</span><mat-progress-bar [value]="m.scoreDomaine" /><span>{{ m.scoreDomaine | number: '1.0-0' }}%</span></div>
        <div class="score-line"><span>Dates ({{ m.joursCommuns }} j communs)</span><mat-progress-bar [value]="m.scoreDate" /><span>{{ m.scoreDate | number: '1.0-0' }}%</span></div>
        <div class="score-line"><span>Proximité ({{ m.distanceKm | number: '1.0-1' }} km)</span><mat-progress-bar [value]="m.scoreDistance" /><span>{{ m.scoreDistance | number: '1.0-0' }}%</span></div>
        <div class="score-line"><span>Même objet</span><mat-progress-bar [value]="m.scoreObjet" /><span>{{ m.scoreObjet | number: '1.0-0' }}%</span></div>
      </div>

      @if (analyse(); as a) {
        <div class="ia">
          <div class="ia-title"><mat-icon>auto_awesome</mat-icon> Pourquoi cette rencontre est intéressante</div>
          <p>{{ a.explication }}</p>
          @if (a.messageContact) {
            <div class="ia-title">Proposition de premier message</div>
            <p class="draft">{{ a.messageContact }}</p>
            <button mat-button (click)="copier(a.messageContact)"><mat-icon>content_copy</mat-icon>Copier le message</button>
          }
        </div>
      } @else {
        <button mat-stroked-button class="ia-btn" (click)="analyser()" [disabled]="analyseEnCours()">
          <mat-icon>auto_awesome</mat-icon>{{ analyseEnCours() ? 'Analyse en cours...' : 'Analyse IA de la rencontre' }}
        </button>
        @if (analyseEnCours()) { <mat-progress-bar mode="indeterminate" /> }
      }

      @if (m.statut === 'ACCEPTEE') {
        <div class="contact">
          <mat-icon>contact_mail</mat-icon>
          <div>
            <strong>Vous êtes en contact !</strong>
            <div><a href="mailto:{{ m.partenaireEmail }}">{{ m.partenaireEmail }}</a>
              @if (m.partenaireTelephone) { · {{ m.partenaireTelephone }} }</div>
          </div>
        </div>
      } @else if (m.statut === 'EN_ATTENTE') {
        @if (m.recue && peutRepondre()) {
          <div class="actions">
            <span class="muted small">{{ m.partenaireNom }} vous propose de voyager ensemble.</span>
            <span class="spacer"></span>
            <button mat-stroked-button (click)="repondre.emit(false)"><mat-icon>close</mat-icon>Refuser</button>
            <button mat-flat-button (click)="repondre.emit(true)"><mat-icon>check</mat-icon>Accepter</button>
          </div>
        } @else if (!m.recue) {
          <div class="muted small">Invitation envoyée — en attente de la réponse du partenaire.</div>
        } @else {
          <div class="muted small">En attente de la réponse de l'employé.</div>
        }
      }
    </mat-card>
  `,
  styles: `
    .match { padding: 16px; display: flex; flex-direction: column; gap: 14px; }
    .head { display: flex; align-items: center; gap: 12px; }
    .who { flex: 1; display: flex; flex-direction: column; gap: 3px; align-items: flex-start; }
    .score {
      width: 56px; height: 56px; border-radius: 50%; display: flex; align-items: center; justify-content: center;
      font-size: 20px; font-weight: 700; background: #fff3e0; color: #e65100; flex-shrink: 0;
      small { font-size: 11px; }
      &.mid { background: #e3f2fd; color: #0d47a1; }
      &.high { background: #e8f5e9; color: #1b5e20; }
    }
    .trip { display: flex; gap: 10px; align-items: flex-start; mat-icon { color: var(--va-primary); } }
    .scores { display: flex; flex-direction: column; gap: 6px; }
    .score-line { grid-template-columns: 170px 1fr 40px; }
    .contact {
      display: flex; gap: 10px; align-items: center; padding: 10px 12px;
      background: #e8f5e9; border-radius: 10px; color: #1b5e20;
    }
    .actions { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
    .spacer { flex: 1; }
    .ia-btn { align-self: flex-start; }
    .ia { background: #fff8e1; border-radius: 10px; padding: 10px 14px; font-size: 14px; }
    .ia p { white-space: pre-line; margin: 6px 0 10px; line-height: 1.5; }
    .ia-title { display: flex; align-items: center; gap: 6px; font-weight: 500; color: #e65100; }
    .draft { background: #fff; border-radius: 8px; padding: 8px 10px; border: 1px dashed #ffb74d; }
  `,
})
export class MatchCardComponent {
  private ai = inject(AiService);
  private notify = inject(NotifyService);
  private clipboard = inject(Clipboard);

  readonly match = input.required<Match>();
  readonly peutRepondre = input(false);
  readonly afficherMonVoyage = input(true);
  readonly repondre = output<boolean>();
  readonly statuts = STATUTS;

  readonly analyse = signal<{ explication: string; messageContact: string } | null>(null);
  readonly analyseEnCours = signal(false);

  analyser(): void {
    this.analyseEnCours.set(true);
    this.ai.expliquerMatch(this.match().id).subscribe({
      next: a => {
        this.analyse.set(a);
        this.analyseEnCours.set(false);
      },
      error: err => {
        this.analyseEnCours.set(false);
        this.notify.error(err);
      },
    });
  }

  copier(texte: string): void {
    if (this.clipboard.copy(texte)) {
      this.notify.success('Message copié dans le presse-papiers');
    }
  }
}
