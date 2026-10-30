import { AuthResponse, Match, User, Voyage } from '../core/models';

/** Donnees de test partagees par les specs. */

export function jetonFactice(expDansSecondes: number): string {
  const encode = (o: object) => btoa(JSON.stringify(o)).replace(/=+$/, '').replace(/\+/g, '-').replace(/\//g, '_');
  return `${encode({ alg: 'HS512' })}.${encode({ sub: 'amine@techsoft.tn', exp: Math.floor(Date.now() / 1000) + expDansSecondes })}.signature`;
}

export const AMINE: User = {
  id: 6, nom: 'Ben Ali', prenom: 'Amine', email: 'amine@techsoft.tn', telephone: '+216 70 000 005',
  poste: 'Chef de projet', role: 'EMPLOYE', actif: true, entrepriseId: 1, entrepriseNom: 'TechSoft',
  domaineActivite: 'INFORMATIQUE', dateCreation: '2026-10-30T10:00:00',
};

export function reponseConnexion(user: User = AMINE): AuthResponse {
  return { token: jetonFactice(3600), user };
}

export function voyage(partiel: Partial<Voyage> = {}): Voyage {
  return {
    id: 1, titre: 'Salon VivaTech', villeDestination: 'Paris', paysDestination: 'France', latitude: 48.85, longitude: 2.35,
    rayonKm: 50, dateDebut: '2026-11-19', dateFin: '2026-11-22', dureeJours: 4, objet: 'SALON', description: null,
    domainesRecherches: ['FINANCE'], visible: true, employeId: 6, employeNom: 'Amine Ben Ali', employePoste: 'Chef de projet',
    entrepriseId: 1, entrepriseNom: 'TechSoft', entrepriseDomaine: 'INFORMATIQUE', statutTemporel: 'A_VENIR',
    dateCreation: '2026-10-30T10:00:00', ...partiel,
  };
}

export function match(partiel: Partial<Match> = {}): Match {
  return {
    id: 7, score: 92.5, scoreDomaine: 100, scoreDate: 75, scoreDistance: 100, scoreObjet: 100, distanceKm: 0,
    joursCommuns: 3, statut: 'EN_ATTENTE', dateCreation: '2026-10-30T10:00:00', dateReponse: null, recue: true,
    monVoyage: voyage({ id: 2, employeId: 8, employeNom: 'Youssef Gharbi' }),
    voyagePartenaire: voyage(),
    partenaireNom: 'Amine Ben Ali', partenairePoste: 'Chef de projet', partenaireEntreprise: 'TechSoft',
    partenaireDomaine: 'INFORMATIQUE', partenaireEmail: null, partenaireTelephone: null, ...partiel,
  };
}
