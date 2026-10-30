import { Component, inject, OnInit, signal } from '@angular/core';
import { ChartType } from 'chart.js';
import { forkJoin, of } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { AiService, UserService, VoyageService } from '../../core/api.service';
import { NotifyService } from '../../core/notify.service';
import { Dashboard, Match, Voyage } from '../../core/models';
import { ChartCardComponent } from '../../shared/chart-card.component';
import { SHARED } from '../../shared/shared';

interface GraphiqueDef {
  cle: string;
  titre: string;
  type: ChartType;
  horizontal?: boolean;
}

/** Tableau de bord : indicateurs et graphiques d'aide a la decision adaptes au role. */
@Component({
  selector: 'app-dashboard',
  imports: [SHARED, ChartCardComponent],
  templateUrl: './dashboard.component.html',
  styles: `
    .stat { padding: 18px; display: flex; align-items: center; gap: 14px; }
    .stat .ico { width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center;
      background: #e3f2fd; color: var(--va-primary); }
    .stat:nth-child(2n) .ico { background: #fff3e0; color: var(--va-accent); }
    .stat:nth-child(3n) .ico { background: #e8f5e9; color: var(--va-success); }
    .stat .val { font-size: 28px; font-weight: 600; line-height: 1; }
    .stat .lbl { color: var(--va-muted); font-size: 13px; margin-top: 4px; }
    .list-item { display: flex; align-items: center; gap: 12px; padding: 10px 0; border-bottom: 1px solid var(--va-border); }
    .list-item:last-child { border-bottom: none; }
    .list-item .grow { flex: 1; }
    h3 { margin: 0 0 8px; font-size: 15px; font-weight: 500; }
    .reco { border-left: 4px solid var(--va-accent); }
    .reco-ico { color: var(--va-accent); }
    .reco-texte { white-space: pre-line; line-height: 1.55; margin: 14px 0 0; }
  `,
})
export class DashboardComponent implements OnInit {
  auth = inject(AuthService);
  private userService = inject(UserService);
  private voyageService = inject(VoyageService);
  private notify = inject(NotifyService);

  readonly dashboard = signal<Dashboard | null>(null);
  readonly prochainsVoyages = signal<Voyage[]>([]);
  readonly invitations = signal<Match[]>([]);
  readonly recommandations = signal<string | null>(null);
  readonly recoEnCours = signal(false);
  private ai = inject(AiService);

  genererRecommandations(): void {
    this.recoEnCours.set(true);
    this.ai.recommandations().subscribe({
      next: r => {
        this.recommandations.set(r.texte);
        this.recoEnCours.set(false);
      },
      error: err => {
        this.recoEnCours.set(false);
        this.notify.error(err);
      },
    });
  }

  readonly graphiques: GraphiqueDef[] = [
    { cle: 'voyagesParMois', titre: 'Voyages par mois de départ', type: 'line' },
    { cle: 'topDestinations', titre: 'Destinations les plus fréquentes', type: 'bar' },
    { cle: 'voyagesParObjet', titre: 'Répartition par objet de voyage', type: 'doughnut' },
    { cle: 'matchsParStatut', titre: 'Invitations de voyage par statut', type: 'pie' },
    { cle: 'entreprisesParDomaine', titre: 'Entreprises par domaine d\'activité', type: 'bar', horizontal: true },
    { cle: 'partenairesParDomaine', titre: 'Entreprises partenaires par domaine', type: 'doughnut' },
    { cle: 'forumParCategorie', titre: 'Publications du forum par catégorie', type: 'doughnut' },
  ];

  ngOnInit(): void {
    const estEmploye = this.auth.hasRole('EMPLOYE');
    forkJoin({
      dashboard: this.userService.dashboard(),
      voyages: this.auth.hasRole('ADMIN') ? of([] as Voyage[]) : this.voyageService.planning(),
      invitations: estEmploye ? this.voyageService.invitationsRecues() : of([] as Match[]),
    }).subscribe({
      next: ({ dashboard, voyages, invitations }) => {
        this.dashboard.set(dashboard);
        this.prochainsVoyages.set(voyages
          .filter(v => v.statutTemporel !== 'TERMINE')
          .sort((a, b) => a.dateDebut.localeCompare(b.dateDebut))
          .slice(0, 5));
        this.invitations.set(invitations.filter(m => m.statut === 'EN_ATTENTE').slice(0, 5));
      },
      error: err => this.notify.error(err),
    });
  }
}
