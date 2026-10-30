import { computed, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { API_URL } from './labels';
import {
  AuthResponse, InvitationInfo, RegisterEmployeRequest, RegisterEntrepriseRequest, Role, User,
} from './models';

const TOKEN_KEY = 'va_token';
const USER_KEY = 'va_user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  private readonly _user = signal<User | null>(this.restaurerSession());
  readonly user = this._user.asReadonly();
  readonly isLoggedIn = computed(() => this._user() !== null);
  readonly role = computed<Role | null>(() => this._user()?.role ?? null);

  get token(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${API_URL}/auth/login`, { email, password })
      .pipe(tap(res => this.ouvrirSession(res)));
  }

  registerEntreprise(req: RegisterEntrepriseRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${API_URL}/auth/register-entreprise`, req)
      .pipe(tap(res => this.ouvrirSession(res)));
  }

  invitationInfo(token: string): Observable<InvitationInfo> {
    return this.http.get<InvitationInfo>(`${API_URL}/auth/invitation/${token}`);
  }

  registerEmploye(req: RegisterEmployeRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${API_URL}/auth/register-employe`, req)
      .pipe(tap(res => this.ouvrirSession(res)));
  }

  /** Met a jour l'utilisateur en memoire apres une modification de profil. */
  updateUser(user: User): void {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
    this._user.set(user);
  }

  hasRole(...roles: Role[]): boolean {
    const r = this.role();
    return r !== null && roles.includes(r);
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    this._user.set(null);
    this.router.navigate(['/login']);
  }

  /** Enregistre un nouveau jeton (connexion, ou modification du profil qui change les informations du jeton). */
  ouvrirSession(res: AuthResponse): void {
    localStorage.setItem(TOKEN_KEY, res.token);
    this.updateUser(res.user);
  }

  private restaurerSession(): User | null {
    const token = localStorage.getItem(TOKEN_KEY);
    const user = localStorage.getItem(USER_KEY);
    if (!token || !user || this.estExpire(token)) {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
      return null;
    }
    return JSON.parse(user) as User;
  }

  private estExpire(token: string): boolean {
    try {
      const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
      return typeof payload.exp === 'number' && payload.exp * 1000 < Date.now();
    } catch {
      return true;
    }
  }
}
