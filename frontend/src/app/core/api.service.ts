import { inject, Injectable, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, shareReplay } from 'rxjs';
import { API_URL } from './labels';
import {
  AuthResponse, Dashboard, DomaineActivite, Entreprise, Invitation, Match, ObjetVoyage, Post, PostRequest, TypeReaction,
  User, Ville, Voyage, VoyageRequest, VoyageResultat,
} from './models';

/** Module Gestion des voyages : voyages, matching, invitations de voyage. */
@Injectable({ providedIn: 'root' })
export class VoyageService {
  private http = inject(HttpClient);
  private url = `${API_URL}/voyages`;
  private villes$?: Observable<Ville[]>;

  planning(): Observable<Voyage[]> {
    return this.http.get<Voyage[]>(this.url);
  }

  programmes(f: { destination?: string; objet?: ObjetVoyage | null; domaine?: DomaineActivite | null; du?: string; au?: string }): Observable<Voyage[]> {
    let params = new HttpParams();
    Object.entries(f).forEach(([k, v]) => {
      if (v !== null && v !== undefined && v !== '') {
        params = params.set(k, String(v));
      }
    });
    return this.http.get<Voyage[]>(`${this.url}/programmes`, { params });
  }

  get(id: number): Observable<Voyage> {
    return this.http.get<Voyage>(`${this.url}/${id}`);
  }

  creer(req: VoyageRequest): Observable<VoyageResultat> {
    return this.http.post<VoyageResultat>(this.url, req);
  }

  modifier(id: number, req: VoyageRequest): Observable<VoyageResultat> {
    return this.http.put<VoyageResultat>(`${this.url}/${id}`, req);
  }

  supprimer(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }

  matchs(id: number): Observable<Match[]> {
    return this.http.get<Match[]>(`${this.url}/${id}/matchs`);
  }

  relancerMatching(id: number): Observable<{ nouveauxMatchs: number }> {
    return this.http.post<{ nouveauxMatchs: number }>(`${this.url}/${id}/matching`, {});
  }

  invitationsRecues(): Observable<Match[]> {
    return this.http.get<Match[]>(`${API_URL}/matchs/recues`);
  }

  invitationsEnvoyees(): Observable<Match[]> {
    return this.http.get<Match[]>(`${API_URL}/matchs/envoyees`);
  }

  repondre(id: number, accepter: boolean): Observable<Match> {
    return this.http.post<Match>(`${API_URL}/matchs/${id}/${accepter ? 'accepter' : 'refuser'}`, {});
  }

  villes(): Observable<Ville[]> {
    this.villes$ ??= this.http.get<Ville[]>(`${API_URL}/meta/villes`).pipe(shareReplay(1));
    return this.villes$;
  }
}

/** Module Gestion des utilisateurs : compte, employes, invitations, administration. */
@Injectable({ providedIn: 'root' })
export class UserService {
  private http = inject(HttpClient);

  moi(): Observable<User> {
    return this.http.get<User>(`${API_URL}/account/me`);
  }

  /** Renvoie un nouveau jeton : les microservices lisent le nom, le poste et l'entreprise dans le JWT. */
  modifierProfil(p: { nom: string; prenom: string; telephone: string | null; poste: string | null }): Observable<AuthResponse> {
    return this.http.put<AuthResponse>(`${API_URL}/account/me`, p);
  }

  changerMotDePasse(ancienMotDePasse: string, nouveauMotDePasse: string): Observable<void> {
    return this.http.put<void>(`${API_URL}/account/password`, { ancienMotDePasse, nouveauMotDePasse });
  }

  monEntreprise(): Observable<Entreprise> {
    return this.http.get<Entreprise>(`${API_URL}/account/entreprise`);
  }

  modifierEntreprise(e: Partial<Entreprise>): Observable<AuthResponse> {
    return this.http.put<AuthResponse>(`${API_URL}/account/entreprise`, e);
  }

  desactiverCompte(): Observable<void> {
    return this.http.delete<void>(`${API_URL}/account/me`);
  }

  // --- Entreprise ---
  employes(): Observable<User[]> {
    return this.http.get<User[]>(`${API_URL}/entreprise/employes`);
  }

  basculerEmploye(id: number): Observable<User> {
    return this.http.patch<User>(`${API_URL}/entreprise/employes/${id}/statut`, {});
  }

  invitations(): Observable<Invitation[]> {
    return this.http.get<Invitation[]>(`${API_URL}/invitations`);
  }

  inviter(email: string, poste: string | null): Observable<Invitation> {
    return this.http.post<Invitation>(`${API_URL}/invitations`, { email, poste });
  }

  renouvelerInvitation(id: number): Observable<Invitation> {
    return this.http.post<Invitation>(`${API_URL}/invitations/${id}/renouveler`, {});
  }

  annulerInvitation(id: number): Observable<void> {
    return this.http.delete<void>(`${API_URL}/invitations/${id}`);
  }

  // --- Administrateur ---
  tousLesUtilisateurs(): Observable<User[]> {
    return this.http.get<User[]>(`${API_URL}/admin/users`);
  }

  basculerUtilisateur(id: number): Observable<User> {
    return this.http.patch<User>(`${API_URL}/admin/users/${id}/statut`, {});
  }

  toutesLesEntreprises(): Observable<Entreprise[]> {
    return this.http.get<Entreprise[]>(`${API_URL}/admin/entreprises`);
  }

  dashboard(): Observable<Dashboard> {
    return this.http.get<Dashboard>(`${API_URL}/stats/dashboard`);
  }
}

export interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
}

/** Fonctionnalites IA (API Claude appelee par le backend). */
@Injectable({ providedIn: 'root' })
export class AiService {
  private http = inject(HttpClient);
  private url = `${API_URL}/ai`;

  /** null tant que le statut n'est pas connu. */
  readonly configure = signal<boolean | null>(null);

  chargerStatut(): void {
    this.http.get<{ configure: boolean }>(`${this.url}/statut`).subscribe({
      next: s => this.configure.set(s.configure),
      error: () => this.configure.set(false),
    });
  }

  chat(messages: ChatMessage[]): Observable<{ texte: string }> {
    return this.http.post<{ texte: string }>(`${this.url}/chat`, { messages });
  }

  expliquerMatch(id: number): Observable<{ explication: string; messageContact: string }> {
    return this.http.get<{ explication: string; messageContact: string }>(`${this.url}/matchs/${id}/explication`);
  }

  programme(req: {
    titre: string; villeDestination: string; paysDestination: string; dateDebut: string; dateFin: string;
    objet: ObjetVoyage; domainesRecherches: DomaineActivite[];
  }): Observable<{ texte: string }> {
    return this.http.post<{ texte: string }>(`${this.url}/programme`, req);
  }

  resumerPost(id: number): Observable<{ texte: string }> {
    return this.http.get<{ texte: string }>(`${this.url}/forum/posts/${id}/resume`);
  }

  moderer(titre: string, contenu: string): Observable<{ acceptable: boolean; raison: string | null }> {
    return this.http.post<{ acceptable: boolean; raison: string | null }>(`${this.url}/forum/moderation`, { titre, contenu });
  }

  recommandations(): Observable<{ texte: string }> {
    return this.http.get<{ texte: string }>(`${this.url}/recommandations`);
  }
}

/** Module Forum. */
@Injectable({ providedIn: 'root' })
export class ForumService {
  private http = inject(HttpClient);
  private url = `${API_URL}/forum`;

  categories(): Observable<string[]> {
    return this.http.get<string[]>(`${this.url}/categories`);
  }

  posts(q: string, categorie: string, tri: string): Observable<Post[]> {
    const params = new HttpParams().set('q', q).set('categorie', categorie).set('tri', tri);
    return this.http.get<Post[]>(`${this.url}/posts`, { params });
  }

  post(id: number): Observable<Post> {
    return this.http.get<Post>(`${this.url}/posts/${id}`);
  }

  creer(p: PostRequest): Observable<Post> {
    return this.http.post<Post>(`${this.url}/posts`, p);
  }

  modifier(id: number, p: PostRequest): Observable<Post> {
    return this.http.put<Post>(`${this.url}/posts/${id}`, p);
  }

  supprimer(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/posts/${id}`);
  }

  commenter(id: number, contenu: string): Observable<Post> {
    return this.http.post<Post>(`${this.url}/posts/${id}/commentaires`, { contenu });
  }

  supprimerCommentaire(id: number): Observable<Post> {
    return this.http.delete<Post>(`${this.url}/commentaires/${id}`);
  }

  reagir(id: number, type: TypeReaction): Observable<Post> {
    return this.http.post<Post>(`${this.url}/posts/${id}/reaction`, { type });
  }
}
