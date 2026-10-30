export type Role = 'ADMIN' | 'ENTREPRISE' | 'EMPLOYE';

export type DomaineActivite =
  | 'INFORMATIQUE' | 'TELECOM' | 'FINANCE' | 'INDUSTRIE' | 'ENERGIE' | 'SANTE' | 'TOURISME'
  | 'AGRICULTURE' | 'COMMERCE' | 'BTP' | 'TRANSPORT' | 'EDUCATION' | 'CONSEIL' | 'AUTRE';

export type ObjetVoyage =
  | 'SEMINAIRE' | 'VISITE_ENTREPRISE' | 'SALON' | 'CONFERENCE' | 'FORMATION' | 'REUNION' | 'PROSPECTION' | 'AUTRE';

export type StatutMatch = 'EN_ATTENTE' | 'ACCEPTEE' | 'REFUSEE';
export type StatutInvitation = 'EN_ATTENTE' | 'ACCEPTEE' | 'ANNULEE' | 'EXPIREE';
export type StatutTemporel = 'A_VENIR' | 'EN_COURS' | 'TERMINE';
export type TypeReaction = 'LIKE' | 'DISLIKE';

// ---------- Utilisateurs ----------

export interface User {
  id: number;
  nom: string;
  prenom: string;
  email: string;
  telephone: string | null;
  poste: string | null;
  role: Role;
  actif: boolean;
  entrepriseId: number | null;
  entrepriseNom: string | null;
  domaineActivite: DomaineActivite | null;
  dateCreation: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface RegisterEntrepriseRequest {
  nomEntreprise: string;
  domaineActivite: DomaineActivite;
  pays: string;
  ville: string;
  adresse?: string;
  telephone?: string;
  siteWeb?: string;
  description?: string;
  nom: string;
  prenom: string;
  email: string;
  password: string;
}

export interface RegisterEmployeRequest {
  token: string;
  nom: string;
  prenom: string;
  telephone?: string;
  poste?: string;
  password: string;
}

export interface InvitationInfo {
  email: string;
  entrepriseNom: string;
  poste: string | null;
}

export interface Entreprise {
  id: number;
  nom: string;
  domaineActivite: DomaineActivite;
  pays: string;
  ville: string;
  adresse: string | null;
  telephone: string | null;
  siteWeb: string | null;
  description: string | null;
  nbEmployes: number;
  dateCreation: string;
}

export interface Invitation {
  id: number;
  email: string;
  poste: string | null;
  statut: StatutInvitation;
  dateCreation: string;
  dateExpiration: string;
  lien: string | null;
}

// ---------- Voyages ----------

export interface Ville {
  id: number;
  nom: string;
  pays: string;
  latitude: number;
  longitude: number;
}

export interface Voyage {
  id: number;
  titre: string;
  villeDestination: string;
  paysDestination: string;
  latitude: number;
  longitude: number;
  rayonKm: number;
  dateDebut: string;
  dateFin: string;
  dureeJours: number;
  objet: ObjetVoyage;
  description: string | null;
  domainesRecherches: DomaineActivite[];
  visible: boolean;
  employeId: number;
  employeNom: string;
  employePoste: string | null;
  entrepriseId: number | null;
  entrepriseNom: string | null;
  entrepriseDomaine: DomaineActivite | null;
  statutTemporel: StatutTemporel;
  dateCreation: string;
}

export interface VoyageRequest {
  titre: string;
  villeDestination: string;
  paysDestination: string;
  latitude: number;
  longitude: number;
  rayonKm: number;
  dateDebut: string;
  dateFin: string;
  objet: ObjetVoyage;
  description?: string | null;
  domainesRecherches: DomaineActivite[];
  visible: boolean;
  employeId?: number | null;
}

export interface VoyageResultat {
  voyage: Voyage;
  nouveauxMatchs: number;
}

export interface Match {
  id: number;
  score: number;
  scoreDomaine: number;
  scoreDate: number;
  scoreDistance: number;
  scoreObjet: number;
  distanceKm: number;
  joursCommuns: number;
  statut: StatutMatch;
  dateCreation: string;
  dateReponse: string | null;
  recue: boolean;
  monVoyage: Voyage;
  voyagePartenaire: Voyage;
  partenaireNom: string;
  partenairePoste: string | null;
  partenaireEntreprise: string | null;
  partenaireDomaine: DomaineActivite | null;
  partenaireEmail: string | null;
  partenaireTelephone: string | null;
}

// ---------- Forum ----------

export interface Commentaire {
  id: number;
  contenu: string;
  auteurId: number;
  auteurNom: string;
  auteurEntreprise: string;
  dateCreation: string;
}

export interface Post {
  id: number;
  titre: string;
  contenu: string;
  categorie: string;
  auteurId: number;
  auteurNom: string;
  auteurEntreprise: string;
  dateCreation: string;
  likes: number;
  dislikes: number;
  maReaction: TypeReaction | null;
  nbCommentaires: number;
  commentaires: Commentaire[] | null;
}

export interface PostRequest {
  titre: string;
  contenu: string;
  categorie: string;
}

// ---------- Statistiques ----------

export interface CarteStat {
  label: string;
  valeur: number;
  icone: string;
}

export interface Serie {
  labels: string[];
  data: number[];
}

export interface Dashboard {
  cartes: CarteStat[];
  graphiques: Record<string, Serie>;
}
