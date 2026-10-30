import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormBuilder } from '@angular/forms';
import { debounceTime } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { VoyageService } from '../../core/api.service';
import { NotifyService } from '../../core/notify.service';
import { DOMAINE_KEYS, DOMAINES, OBJET_KEYS, OBJETS, STATUTS, toIsoDate } from '../../core/labels';
import { DomaineActivite, ObjetVoyage, Voyage } from '../../core/models';
import { SHARED } from '../../shared/shared';

/** Consultation des programmes de voyages publics de toutes les entreprises. */
@Component({
  selector: 'app-programmes',
  imports: [SHARED],
  templateUrl: './programmes.component.html',
  styles: `
    .trip { padding: 18px; display: flex; flex-direction: column; gap: 10px; height: 100%; box-sizing: border-box; }
    .trip.mine { border-left: 4px solid var(--va-accent); }
    .dest { display: flex; align-items: center; gap: 8px; font-size: 18px; font-weight: 500; }
    .dest mat-icon { color: var(--va-primary); }
    .line { display: flex; align-items: center; gap: 6px; font-size: 14px; }
    .line mat-icon { font-size: 18px; width: 18px; height: 18px; color: var(--va-muted); }
    .foot { margin-top: auto; display: flex; gap: 6px; flex-wrap: wrap; align-items: center; }
    .filters { padding: 16px 20px 0; }
  `,
})
export class ProgrammesComponent implements OnInit {
  auth = inject(AuthService);
  private fb = inject(FormBuilder);
  private voyageService = inject(VoyageService);
  private notify = inject(NotifyService);

  readonly domaines = DOMAINE_KEYS;
  readonly libellesDomaines = DOMAINES;
  readonly objets = OBJET_KEYS;
  readonly libellesObjets = OBJETS;
  readonly statuts = STATUTS;

  readonly voyages = signal<Voyage[]>([]);
  readonly loading = signal(false);

  /** Statistiques rapides sur les resultats (destinations les plus demandees). */
  readonly topDestinations = computed(() => {
    const compte = new Map<string, number>();
    this.voyages().forEach(v => compte.set(v.villeDestination, (compte.get(v.villeDestination) ?? 0) + 1));
    return [...compte.entries()].sort((a, b) => b[1] - a[1]).slice(0, 5);
  });

  filtres = this.fb.group({
    destination: [''],
    objet: [null as ObjetVoyage | null],
    domaine: [null as DomaineActivite | null],
    du: [null as Date | null],
    au: [null as Date | null],
  });

  ngOnInit(): void {
    this.rechercher();
    this.filtres.valueChanges.pipe(debounceTime(300)).subscribe(() => this.rechercher());
  }

  rechercher(): void {
    const f = this.filtres.getRawValue();
    this.loading.set(true);
    this.voyageService.programmes({
      destination: f.destination ?? '',
      objet: f.objet,
      domaine: f.domaine,
      du: f.du ? toIsoDate(f.du) : undefined,
      au: f.au ? toIsoDate(f.au) : undefined,
    }).subscribe({
      next: v => {
        this.voyages.set(v);
        this.loading.set(false);
      },
      error: err => {
        this.loading.set(false);
        this.notify.error(err);
      },
    });
  }

  reinitialiser(): void {
    this.filtres.reset({ destination: '', objet: null, domaine: null, du: null, au: null });
  }

  estMonEntreprise(v: Voyage): boolean {
    const u = this.auth.user();
    return !!u?.entrepriseId && v.entrepriseId === u.entrepriseId;
  }
}
