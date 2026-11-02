import { DomaineActivite, ObjetVoyage, Role } from './models';

/** Chemin relatif : nginx (Docker / Kubernetes) ou le proxy de ng serve redirigent vers l'API Gateway. */
export const API_URL = '/api';

export const DOMAINES: Record<DomaineActivite, string> = {
  INFORMATIQUE: 'Informatique',
  TELECOM: 'Télécommunications',
  FINANCE: 'Finance',
  INDUSTRIE: 'Industrie',
  ENERGIE: 'Énergie',
  SANTE: 'Santé',
  TOURISME: 'Tourisme',
  AGRICULTURE: 'Agriculture',
  COMMERCE: 'Commerce',
  BTP: 'BTP',
  TRANSPORT: 'Transport & logistique',
  EDUCATION: 'Éducation',
  CONSEIL: 'Conseil',
  AUTRE: 'Autre',
};

export const OBJETS: Record<ObjetVoyage, string> = {
  SEMINAIRE: 'Séminaire',
  VISITE_ENTREPRISE: 'Visite d\'entreprise',
  SALON: 'Salon professionnel',
  CONFERENCE: 'Conférence',
  FORMATION: 'Formation',
  REUNION: 'Réunion',
  PROSPECTION: 'Prospection',
  AUTRE: 'Autre',
};

export const ROLES: Record<Role, string> = {
  ADMIN: 'Administrateur',
  ENTREPRISE: 'Entreprise',
  EMPLOYE: 'Employé',
};

/** Libelle + couleur de badge pour les differents statuts. */
export const STATUTS: Record<string, { label: string; color: string }> = {
  EN_ATTENTE: { label: 'En attente', color: 'orange' },
  ACCEPTEE: { label: 'Acceptée', color: 'green' },
  REFUSEE: { label: 'Refusée', color: 'red' },
  ANNULEE: { label: 'Annulée', color: 'grey' },
  EXPIREE: { label: 'Expirée', color: 'grey' },
  A_VENIR: { label: 'À venir', color: 'blue' },
  EN_COURS: { label: 'En cours', color: 'green' },
  TERMINE: { label: 'Terminé', color: 'grey' },
};

export const DOMAINE_KEYS = Object.keys(DOMAINES) as DomaineActivite[];
export const OBJET_KEYS = Object.keys(OBJETS) as ObjetVoyage[];

/** Traduit une cle d'enum (domaine, objet, statut) en libelle lisible. */
export function libelle(key: string): string {
  return (DOMAINES as Record<string, string>)[key]
    ?? (OBJETS as Record<string, string>)[key]
    ?? STATUTS[key]?.label
    ?? key;
}

/** Date locale -> 'yyyy-MM-dd' (sans decalage de fuseau horaire). */
export function toIsoDate(d: Date): string {
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const j = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${m}-${j}`;
}

/** 'yyyy-MM-dd' -> Date locale. */
export function fromIsoDate(s: string): Date {
  const [y, m, d] = s.split('-').map(Number);
  return new Date(y, m - 1, d);
}
