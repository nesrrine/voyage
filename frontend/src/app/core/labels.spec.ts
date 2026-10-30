import { fromIsoDate, libelle, toIsoDate } from './labels';
import { LibellePipe } from '../shared/libelle.pipe';

describe('labels', () => {
  it('traduit les cles d\'enum (domaine, objet, statut)', () => {
    expect(libelle('ENERGIE')).toBe('Énergie');
    expect(libelle('VISITE_ENTREPRISE')).toBe('Visite d\'entreprise');
    expect(libelle('ACCEPTEE')).toBe('Acceptée');
  });

  it('renvoie la cle telle quelle si elle est inconnue', () => {
    expect(libelle('INCONNU')).toBe('INCONNU');
  });

  it('convertit une date locale en yyyy-MM-dd sans decalage de fuseau', () => {
    expect(toIsoDate(new Date(2026, 0, 5, 23, 30))).toBe('2026-01-05');
  });

  it('fait l\'aller-retour date ISO -> Date -> ISO', () => {
    expect(toIsoDate(fromIsoDate('2026-12-31'))).toBe('2026-12-31');
  });
});

describe('LibellePipe', () => {
  const pipe = new LibellePipe();

  it('affiche le libelle francais', () => {
    expect(pipe.transform('CONFERENCE')).toBe('Conférence');
  });

  it('gere les valeurs vides', () => {
    expect(pipe.transform(null)).toBe('');
    expect(pipe.transform(undefined)).toBe('');
  });
});
