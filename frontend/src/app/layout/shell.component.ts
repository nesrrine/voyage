import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { BreakpointObserver } from '@angular/cdk/layout';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatBadgeModule } from '@angular/material/badge';
import { MatDividerModule } from '@angular/material/divider';
import { AuthService } from '../core/auth.service';
import { AiService, VoyageService } from '../core/api.service';
import { AssistantComponent } from './assistant.component';
import { ROLES } from '../core/labels';
import { Role } from '../core/models';

interface MenuItem {
  label: string;
  icon: string;
  route: string;
  roles?: Role[];
  module?: string;
}

@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatSidenavModule, MatToolbarModule, MatListModule,
    MatIconModule, MatButtonModule, MatMenuModule, MatBadgeModule, MatDividerModule, AssistantComponent],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
})
export class ShellComponent implements OnInit {
  auth = inject(AuthService);
  private voyageService = inject(VoyageService);
  private ai = inject(AiService);
  private router = inject(Router);

  readonly roles = ROLES;
  readonly isMobile = toSignal(
    inject(BreakpointObserver).observe('(max-width: 960px)').pipe(map(r => r.matches)),
    { initialValue: false });
  readonly invitationsEnAttente = signal(0);

  private readonly items: MenuItem[] = [
    { label: 'Tableau de bord', icon: 'dashboard', route: '/dashboard' },
    { label: 'Planning des voyages', icon: 'flight_takeoff', route: '/voyages', module: 'Voyages' },
    { label: 'Programmes de voyages', icon: 'travel_explore', route: '/programmes', module: 'Voyages' },
    { label: 'Invitations de voyage', icon: 'handshake', route: '/invitations-voyage', roles: ['EMPLOYE'], module: 'Voyages' },
    { label: 'Employés & invitations', icon: 'groups', route: '/employes', roles: ['ENTREPRISE'], module: 'Utilisateurs' },
    { label: 'Administration', icon: 'admin_panel_settings', route: '/admin', roles: ['ADMIN'], module: 'Utilisateurs' },
    { label: 'Mon compte', icon: 'manage_accounts', route: '/compte', module: 'Utilisateurs' },
    { label: 'Forum', icon: 'forum', route: '/forum', module: 'Forum' },
  ];

  readonly menu = computed(() => {
    const role = this.auth.role();
    return this.items.filter(i => !i.roles || (role !== null && i.roles.includes(role)));
  });

  readonly initiales = computed(() => {
    const u = this.auth.user();
    return u ? ((u.prenom?.[0] ?? '') + (u.nom?.[0] ?? '')).toUpperCase() : '';
  });

  ngOnInit(): void {
    this.ai.chargerStatut();
    this.rafraichirCompteur();
    // Le compteur est mis a jour a chaque navigation (ex : apres avoir repondu a une invitation)
    this.router.events.pipe(filter(e => e instanceof NavigationEnd)).subscribe(() => this.rafraichirCompteur());
  }

  private rafraichirCompteur(): void {
    if (!this.auth.hasRole('EMPLOYE')) {
      return;
    }
    this.voyageService.invitationsRecues().subscribe({
      next: list => this.invitationsEnAttente.set(list.filter(m => m.statut === 'EN_ATTENTE').length),
      error: () => this.invitationsEnAttente.set(0),
    });
  }
}
