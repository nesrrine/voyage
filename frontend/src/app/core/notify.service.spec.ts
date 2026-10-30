import { HttpErrorResponse } from '@angular/common/http';
import { NotifyService } from './notify.service';

describe('NotifyService.message', () => {
  it('signale un serveur injoignable (statut 0)', () => {
    const err = new HttpErrorResponse({ status: 0 });
    expect(NotifyService.message(err)).toContain('Serveur injoignable');
  });

  it('assemble les erreurs de validation renvoyees par l\'API', () => {
    const err = new HttpErrorResponse({
      status: 400,
      error: { message: 'Donnees invalides', erreurs: { titre: 'Le titre est obligatoire', dateDebut: 'obligatoire' } },
    });
    expect(NotifyService.message(err)).toBe('titre : Le titre est obligatoire • dateDebut : obligatoire');
  });

  it('reprend le message metier de l\'API (gateway ou microservice)', () => {
    const err = new HttpErrorResponse({ status: 403, error: { message: 'Acces refuse : droits insuffisants' } });
    expect(NotifyService.message(err)).toBe('Acces refuse : droits insuffisants');
  });

  it('utilise le message par defaut pour une erreur inconnue', () => {
    expect(NotifyService.message(new Error('x'), 'Echec')).toBe('Echec');
  });
});
