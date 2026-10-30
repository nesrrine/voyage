import { AfterViewInit, Component, inject, OnInit, ViewChild } from '@angular/core';
import { MatTableDataSource } from '@angular/material/table';
import { MatPaginator } from '@angular/material/paginator';
import { MatSort } from '@angular/material/sort';
import { UserService } from '../../core/api.service';
import { NotifyService } from '../../core/notify.service';
import { ROLES } from '../../core/labels';
import { Entreprise, Role, User } from '../../core/models';
import { SHARED } from '../../shared/shared';

/** Administration : tous les comptes utilisateurs et toutes les entreprises. */
@Component({
  selector: 'app-admin',
  imports: [SHARED],
  template: `
    <div class="page">
      <div class="page-header">
        <div><h1>Administration</h1><p>Gestion des utilisateurs et des entreprises inscrites.</p></div>
      </div>
      <mat-card class="card" appearance="outlined">
        <mat-tab-group animationDuration="150ms">
          <mat-tab label="Utilisateurs">
            <div class="row card-pad" style="padding-bottom: 0">
              <mat-form-field appearance="outline" class="grow" subscriptSizing="dynamic">
                <mat-label>Rechercher</mat-label>
                <mat-icon matPrefix>search</mat-icon>
                <input matInput #q (input)="users.filter = q.value.trim().toLowerCase()">
              </mat-form-field>
            </div>
            <div class="table-wrap mt">
              <table mat-table [dataSource]="users" matSort>
                <ng-container matColumnDef="nom">
                  <th mat-header-cell *matHeaderCellDef mat-sort-header>Nom</th>
                  <td mat-cell *matCellDef="let u"><strong>{{ u.prenom }} {{ u.nom }}</strong><div class="muted small">{{ u.email }}</div></td>
                </ng-container>
                <ng-container matColumnDef="role">
                  <th mat-header-cell *matHeaderCellDef mat-sort-header>Rôle</th>
                  <td mat-cell *matCellDef="let u"><span class="badge {{ couleurRole(u.role) }}">{{ libelleRole(u.role) }}</span></td>
                </ng-container>
                <ng-container matColumnDef="entrepriseNom">
                  <th mat-header-cell *matHeaderCellDef mat-sort-header>Entreprise</th>
                  <td mat-cell *matCellDef="let u">{{ u.entrepriseNom ?? '—' }}</td>
                </ng-container>
                <ng-container matColumnDef="dateCreation">
                  <th mat-header-cell *matHeaderCellDef mat-sort-header>Inscription</th>
                  <td mat-cell *matCellDef="let u">{{ u.dateCreation | date: 'dd/MM/yyyy' }}</td>
                </ng-container>
                <ng-container matColumnDef="actif">
                  <th mat-header-cell *matHeaderCellDef>Actif</th>
                  <td mat-cell *matCellDef="let u">
                    <mat-slide-toggle [checked]="u.actif" [disabled]="u.role === 'ADMIN'" (change)="basculer(u)" />
                  </td>
                </ng-container>
                <tr mat-header-row *matHeaderRowDef="colonnes"></tr>
                <tr mat-row *matRowDef="let row; columns: colonnes"></tr>
              </table>
            </div>
            <mat-paginator [pageSizeOptions]="[10, 25, 50]" showFirstLastButtons />
          </mat-tab>

          <mat-tab label="Entreprises">
            <div class="table-wrap">
              <table mat-table [dataSource]="entreprises">
                <ng-container matColumnDef="nom">
                  <th mat-header-cell *matHeaderCellDef>Entreprise</th>
                  <td mat-cell *matCellDef="let e"><strong>{{ e.nom }}</strong><div class="muted small">{{ e.siteWeb }}</div></td>
                </ng-container>
                <ng-container matColumnDef="domaine">
                  <th mat-header-cell *matHeaderCellDef>Domaine</th>
                  <td mat-cell *matCellDef="let e"><span class="badge purple">{{ e.domaineActivite | libelle }}</span></td>
                </ng-container>
                <ng-container matColumnDef="lieu">
                  <th mat-header-cell *matHeaderCellDef>Localisation</th>
                  <td mat-cell *matCellDef="let e">{{ e.ville }}, {{ e.pays }}</td>
                </ng-container>
                <ng-container matColumnDef="employes">
                  <th mat-header-cell *matHeaderCellDef>Employés</th>
                  <td mat-cell *matCellDef="let e">{{ e.nbEmployes }}</td>
                </ng-container>
                <tr mat-header-row *matHeaderRowDef="colonnesEntreprises"></tr>
                <tr mat-row *matRowDef="let row; columns: colonnesEntreprises"></tr>
              </table>
            </div>
          </mat-tab>
        </mat-tab-group>
      </mat-card>
    </div>
  `,
})
export class AdminComponent implements OnInit, AfterViewInit {
  private userService = inject(UserService);
  private notify = inject(NotifyService);

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  readonly roles = ROLES;
  readonly users = new MatTableDataSource<User>([]);
  entreprises: Entreprise[] = [];
  readonly colonnes = ['nom', 'role', 'entrepriseNom', 'dateCreation', 'actif'];
  readonly colonnesEntreprises = ['nom', 'domaine', 'lieu', 'employes'];

  ngOnInit(): void {
    this.users.filterPredicate = (u, f) => `${u.prenom} ${u.nom} ${u.email} ${u.entrepriseNom ?? ''} ${u.role}`.toLowerCase().includes(f);
    this.userService.tousLesUtilisateurs().subscribe({ next: u => (this.users.data = u), error: err => this.notify.error(err) });
    this.userService.toutesLesEntreprises().subscribe({ next: e => (this.entreprises = e), error: err => this.notify.error(err) });
  }

  ngAfterViewInit(): void {
    this.users.paginator = this.paginator;
    this.users.sort = this.sort;
  }

  libelleRole(role: Role): string {
    return ROLES[role];
  }

  couleurRole(role: Role): string {
    return role === 'ADMIN' ? 'red' : role === 'ENTREPRISE' ? 'orange' : 'blue';
  }

  basculer(u: User): void {
    this.userService.basculerUtilisateur(u.id).subscribe({
      next: maj => {
        this.users.data = this.users.data.map(x => (x.id === maj.id ? maj : x));
        this.notify.success(`Compte ${maj.actif ? 'activé' : 'désactivé'}`);
      },
      error: err => this.notify.error(err),
    });
  }
}
