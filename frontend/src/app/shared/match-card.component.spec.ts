import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { LOCALE_ID } from '@angular/core';
import { of } from 'rxjs';
import { MatchCardComponent } from './match-card.component';
import { AiService } from '../core/api.service';
import { NotifyService } from '../core/notify.service';
import { Match } from '../core/models';
import { match } from '../testing/fixtures';

describe('MatchCardComponent', () => {
  let fixture: ComponentFixture<MatchCardComponent>;
  let ai: jasmine.SpyObj<AiService>;

  beforeAll(() => registerLocaleData(localeFr));

  function afficher(m: Match, peutRepondre = false): HTMLElement {
    fixture.componentRef.setInput('match', m);
    fixture.componentRef.setInput('peutRepondre', peutRepondre);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  beforeEach(() => {
    ai = jasmine.createSpyObj<AiService>('AiService', ['expliquerMatch']);
    TestBed.configureTestingModule({
      imports: [MatchCardComponent],
      providers: [
        provideNoopAnimations(), provideRouter([]),
        { provide: LOCALE_ID, useValue: 'fr-FR' },
        { provide: AiService, useValue: ai },
        { provide: NotifyService, useValue: jasmine.createSpyObj('NotifyService', ['success', 'error']) },
      ],
    });
    fixture = TestBed.createComponent(MatchCardComponent);
  });

  it('affiche le partenaire, son entreprise et le score de compatibilite', () => {
    const el = afficher(match());
    expect(el.textContent).toContain('Amine Ben Ali');
    expect(el.textContent).toContain('TechSoft');
    expect(el.querySelector('.score')?.textContent).toContain('93');
    expect(el.textContent).toContain('Informatique');
  });

  it('cache les coordonnees tant que l\'invitation n\'est pas acceptee', () => {
    const el = afficher(match());
    expect(el.querySelector('.contact')).toBeNull();
  });

  it('affiche les coordonnees une fois l\'invitation acceptee (mise en contact)', () => {
    const el = afficher(match({ statut: 'ACCEPTEE', partenaireEmail: 'amine@techsoft.tn' }));
    expect(el.querySelector('.contact')?.textContent).toContain('amine@techsoft.tn');
  });

  it('emet la reponse quand le destinataire accepte', () => {
    const reponses: boolean[] = [];
    fixture.componentInstance.repondre.subscribe(r => reponses.push(r));
    const el = afficher(match(), true);

    const boutons = Array.from(el.querySelectorAll('button')) as HTMLButtonElement[];
    boutons.find(b => b.textContent?.includes('Accepter'))!.click();

    expect(reponses).toEqual([true]);
  });

  it('ne propose pas de repondre a l\'expediteur de l\'invitation', () => {
    const el = afficher(match({ recue: false }), true);
    expect(el.textContent).not.toContain('Accepter');
    expect(el.textContent).toContain('en attente de la réponse du partenaire');
  });

  it('affiche l\'analyse de l\'IA et le message de premier contact', () => {
    ai.expliquerMatch.and.returnValue(of({ explication: 'Domaines complementaires.', messageContact: 'Bonjour Amine' }));
    const el = afficher(match());

    (Array.from(el.querySelectorAll('button')) as HTMLButtonElement[])
      .find(b => b.textContent?.includes('Analyse IA'))!.click();
    fixture.detectChanges();

    expect(ai.expliquerMatch).toHaveBeenCalledWith(7);
    expect(el.querySelector('.ia')?.textContent).toContain('Domaines complementaires.');
    expect(el.querySelector('.draft')?.textContent).toContain('Bonjour Amine');
  });
});
