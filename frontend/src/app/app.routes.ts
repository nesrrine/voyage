import { Routes } from '@angular/router';
import { authGuard, guestGuard, roleGuard } from './core/guards';
import { ShellComponent } from './layout/shell.component';

export const routes: Routes = [
  // ----- Pages publiques -----
  {
    path: 'login', canActivate: [guestGuard],
    loadComponent: () => import('./pages/auth/login.component').then(m => m.LoginComponent),
  },
  {
    path: 'inscription', canActivate: [guestGuard],
    loadComponent: () => import('./pages/auth/register-entreprise.component').then(m => m.RegisterEntrepriseComponent),
  },
  {
    path: 'inscription-employe',
    loadComponent: () => import('./pages/auth/register-employe.component').then(m => m.RegisterEmployeComponent),
  },

  // ----- Espace connecte -----
  {
    path: '', component: ShellComponent, canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        loadComponent: () => import('./pages/dashboard/dashboard.component').then(m => m.DashboardComponent),
      },
      // Module Gestion des voyages
      {
        path: 'voyages',
        loadComponent: () => import('./pages/voyages/voyage-list.component').then(m => m.VoyageListComponent),
      },
      {
        path: 'voyages/nouveau', canActivate: [roleGuard], data: { roles: ['EMPLOYE', 'ENTREPRISE'] },
        loadComponent: () => import('./pages/voyages/voyage-form.component').then(m => m.VoyageFormComponent),
      },
      {
        path: 'voyages/:id/modifier', canActivate: [roleGuard], data: { roles: ['EMPLOYE', 'ENTREPRISE'] },
        loadComponent: () => import('./pages/voyages/voyage-form.component').then(m => m.VoyageFormComponent),
      },
      {
        path: 'voyages/:id',
        loadComponent: () => import('./pages/voyages/voyage-detail.component').then(m => m.VoyageDetailComponent),
      },
      {
        path: 'programmes',
        loadComponent: () => import('./pages/voyages/programmes.component').then(m => m.ProgrammesComponent),
      },
      {
        path: 'invitations-voyage', canActivate: [roleGuard], data: { roles: ['EMPLOYE'] },
        loadComponent: () => import('./pages/voyages/invitations-voyage.component').then(m => m.InvitationsVoyageComponent),
      },
      // Module Gestion des utilisateurs
      {
        path: 'employes', canActivate: [roleGuard], data: { roles: ['ENTREPRISE'] },
        loadComponent: () => import('./pages/users/employes.component').then(m => m.EmployesComponent),
      },
      {
        path: 'compte',
        loadComponent: () => import('./pages/users/compte.component').then(m => m.CompteComponent),
      },
      {
        path: 'admin', canActivate: [roleGuard], data: { roles: ['ADMIN'] },
        loadComponent: () => import('./pages/users/admin.component').then(m => m.AdminComponent),
      },
      // Module Forum
      {
        path: 'forum',
        loadComponent: () => import('./pages/forum/forum-list.component').then(m => m.ForumListComponent),
      },
      {
        path: 'forum/:id',
        loadComponent: () => import('./pages/forum/post-detail.component').then(m => m.PostDetailComponent),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
