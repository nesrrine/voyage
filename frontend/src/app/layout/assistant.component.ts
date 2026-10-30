import { Component, ElementRef, inject, signal, ViewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AiService, ChatMessage } from '../core/api.service';
import { NotifyService } from '../core/notify.service';

/** Assistant IA de voyage : bulle flottante disponible sur toutes les pages de l'espace connecte. */
@Component({
  selector: 'app-assistant',
  imports: [FormsModule, MatButtonModule, MatIconModule, MatProgressBarModule, MatTooltipModule],
  template: `
    @if (ouvert()) {
      <div class="panel">
        <div class="head">
          <mat-icon>auto_awesome</mat-icon>
          <div class="title"><strong>Assistant IA</strong><span>Propulsé par Claude</span></div>
          <button mat-icon-button (click)="reinitialiser()" matTooltip="Nouvelle conversation"><mat-icon>refresh</mat-icon></button>
          <button mat-icon-button (click)="ouvert.set(false)" matTooltip="Fermer"><mat-icon>close</mat-icon></button>
        </div>

        <div class="messages" #liste>
          @if (ai.configure() === false) {
            <div class="info">L'IA n'est pas configurée : définissez la variable d'environnement
              <code>ANTHROPIC_API_KEY</code> puis redémarrez le backend.</div>
          }
          @if (!messages().length) {
            <div class="info">Bonjour ! Je connais vos voyages et les programmes de la plateforme. Essayez :</div>
            @for (s of suggestions; track s) {
              <button class="suggestion" (click)="envoyer(s)">{{ s }}</button>
            }
          }
          @for (m of messages(); track $index) {
            <div class="msg" [class.user]="m.role === 'user'">{{ m.content }}</div>
          }
          @if (attente()) { <mat-progress-bar mode="indeterminate" /> }
        </div>

        <form class="input" (ngSubmit)="envoyer(saisie)">
          <textarea [(ngModel)]="saisie" name="saisie" rows="2" placeholder="Posez votre question..."
                    (keydown.enter)="$event.preventDefault(); envoyer(saisie)" maxlength="4000"></textarea>
          <button mat-icon-button type="submit" [disabled]="attente() || !saisie.trim()"><mat-icon>send</mat-icon></button>
        </form>
      </div>
    }
    <button class="fab" mat-fab extended (click)="ouvert.set(!ouvert())" aria-label="Assistant IA">
      <mat-icon>{{ ouvert() ? 'close' : 'auto_awesome' }}</mat-icon>{{ ouvert() ? 'Fermer' : 'Assistant IA' }}
    </button>
  `,
  styles: `
    .fab { position: fixed; right: 24px; bottom: 24px; z-index: 1000; }
    .panel {
      position: fixed; right: 24px; bottom: 92px; z-index: 1000; width: 380px; max-width: calc(100vw - 32px);
      height: 540px; max-height: calc(100vh - 120px); background: #fff; border-radius: 16px;
      box-shadow: 0 12px 40px rgba(15, 42, 74, .25); display: flex; flex-direction: column; overflow: hidden;
    }
    .head { display: flex; align-items: center; gap: 10px; padding: 10px 8px 10px 16px; background: #0f2a4a; color: #fff;
      mat-icon { color: #ffb74d; } button { color: #fff; } }
    .title { flex: 1; display: flex; flex-direction: column; line-height: 1.2; span { font-size: 11px; opacity: .7; } }
    .messages { flex: 1; overflow-y: auto; padding: 14px; display: flex; flex-direction: column; gap: 10px; background: #f7f9fc; }
    .msg { max-width: 85%; padding: 10px 12px; border-radius: 12px; background: #fff; border: 1px solid var(--va-border);
      white-space: pre-line; font-size: 14px; line-height: 1.45; align-self: flex-start; }
    .msg.user { align-self: flex-end; background: var(--va-primary); color: #fff; border: none; }
    .info { font-size: 13px; color: var(--va-muted); }
    .suggestion { text-align: left; border: 1px solid var(--va-border); background: #fff; border-radius: 10px;
      padding: 8px 10px; cursor: pointer; font: inherit; font-size: 13px; }
    .suggestion:hover { border-color: var(--va-primary); }
    .input { display: flex; align-items: center; gap: 4px; padding: 8px; border-top: 1px solid var(--va-border);
      textarea { flex: 1; resize: none; border: none; outline: none; font: inherit; font-size: 14px; padding: 6px; } }
  `,
})
export class AssistantComponent {
  ai = inject(AiService);
  private notify = inject(NotifyService);

  @ViewChild('liste') liste?: ElementRef<HTMLElement>;

  readonly ouvert = signal(false);
  readonly attente = signal(false);
  readonly messages = signal<ChatMessage[]>([]);
  saisie = '';

  readonly suggestions = [
    'Qui part dans la même ville que moi prochainement ?',
    'Donne-moi des conseils pour mon prochain voyage d\'affaires.',
    'Comment améliorer mes chances de trouver des partenaires ?',
  ];

  envoyer(texte: string): void {
    const contenu = texte.trim();
    if (!contenu || this.attente()) {
      return;
    }
    this.saisie = '';
    // On n'envoie que les 20 derniers messages pour limiter la taille de la requete
    const historique = [...this.messages(), { role: 'user' as const, content: contenu }].slice(-20);
    this.messages.set(historique);
    this.attente.set(true);
    this.defiler();
    this.ai.chat(historique).subscribe({
      next: r => {
        this.messages.update(m => [...m, { role: 'assistant', content: r.texte }]);
        this.attente.set(false);
        this.defiler();
      },
      error: err => {
        // On retire la question restee sans reponse pour garder un historique alterne user/assistant
        this.messages.update(m => m.slice(0, -1));
        this.saisie = contenu;
        this.attente.set(false);
        this.notify.error(err);
      },
    });
  }

  reinitialiser(): void {
    this.messages.set([]);
  }

  private defiler(): void {
    setTimeout(() => this.liste?.nativeElement.scrollTo({ top: this.liste.nativeElement.scrollHeight, behavior: 'smooth' }));
  }
}
