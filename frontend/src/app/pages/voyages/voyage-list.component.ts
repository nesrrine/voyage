import { AfterViewInit, Component, inject, OnInit, signal, ViewChild } from '@angular/core';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { MatDialog } from '@angular/material/dialog';
import { AuthService } from '../../core/auth.service';
import { VoyageService } from '../../core/api.service';
import { NotifyService } from '../../core/notify.service';
import { STATUTS } from '../../core/labels';
import { StatutTemporel, Voyage } from '../../core/models';
import { confirmer } from '../../shared/confirm-dialog.component';
import { SHARED } from '../../shared/shared';

/** Planning des voyages (CRUD) : mes voyages, ceux de l'entreprise, ou tous pour l'admin. */
@Component({
  selector: 'app-voyage-list',
  imports: [SHARED],
  templateUrl: './voyage-list.component.html',
})
export class VoyageListComponent implements OnInit, AfterViewInit {
  auth = inject(AuthService);
  private voyageService = inject(VoyageService);
  private notify = inject(NotifyService);
  private dialog = inject(MatDialog);

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  readonly statuts = STATUTS;
  readonly loading = signal(true);
  readonly filtreStatut = signal<StatutTemporel | 'TOUS'>('TOUS');
  readonly dataSource = new MatTableDataSource<Voyage>([]);
  private tous: Voyage[] = [];
  private recherche = '';

  get colonnes(): string[] {
    const cols = ['titre', 'villeDestination', 'dateDebut', 'objet'];
    if (!this.auth.hasRole('EMPLOYE')) {
      cols.push('employeNom');
    }
    return [...cols, 'statutTemporel', 'visible', 'actions'];
  }

  ngOnInit(): void {
    this.dataSource.filterPredicate = (v, filtre) =>
      `${v.titre} ${v.villeDestination} ${v.paysDestination} ${v.employeNom} ${v.objet}`.toLowerCase().includes(filtre);
    this.charger();
  }

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }

  charger(): void {
    this.loading.set(true);
    this.voyageService.planning().subscribe({
      next: voyages => {
        this.tous = voyages;
        this.appliquerStatut();
        this.loading.set(false);
      },
      error: err => {
        this.loading.set(false);
        this.notify.error(err);
      },
    });
  }

  rechercher(valeur: string): void {
    this.recherche = valeur.trim().toLowerCase();
    this.dataSource.filter = this.recherche;
  }

  changerStatut(statut: StatutTemporel | 'TOUS'): void {
    this.filtreStatut.set(statut);
    this.appliquerStatut();
  }

  compter(statut: StatutTemporel): number {
    return this.tous.filter(v => v.statutTemporel === statut).length;
  }

  supprimer(v: Voyage): void {
    confirmer(this.dialog, {
      titre: 'Supprimer le voyage',
      message: `Supprimer « ${v.titre} » ? Les invitations de voyage associées seront également supprimées.`,
      confirmer: 'Supprimer',
      danger: true,
    }).subscribe(ok => {
      if (ok) {
        this.voyageService.supprimer(v.id).subscribe({
          next: () => {
            this.notify.success('Voyage supprimé');
            this.charger();
          },
          error: err => this.notify.error(err),
        });
      }
    });
  }

  private appliquerStatut(): void {
    const s = this.filtreStatut();
    this.dataSource.data = s === 'TOUS' ? this.tous : this.tous.filter(v => v.statutTemporel === s);
    this.dataSource.filter = this.recherche;
  }
}
