import { Component, computed, inject, input, OnInit, signal } from '@angular/core';
import { FormControl, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { AuthService } from '../../core/auth.service';
import { AiService, ForumService } from '../../core/api.service';
import { NotifyService } from '../../core/notify.service';
import { Commentaire, Post, PostRequest, TypeReaction } from '../../core/models';
import { confirmer } from '../../shared/confirm-dialog.component';
import { SHARED } from '../../shared/shared';
import { PostDialogComponent } from './post-dialog.component';
import { ReactionsComponent } from './reactions.component';

@Component({
  selector: 'app-post-detail',
  imports: [SHARED, ReactionsComponent],
  template: `
    <div class="page" style="max-width: 860px">
      <a mat-button routerLink="/forum" class="mb"><mat-icon>arrow_back</mat-icon>Retour au forum</a>
      @if (post(); as p) {
        <mat-card class="card card-pad mb" appearance="outlined">
          <div class="row small muted">
            <span class="badge blue">{{ p.categorie }}</span>
            <span><strong>{{ p.auteurNom }}</strong> · {{ p.auteurEntreprise }} · {{ p.dateCreation | date: 'dd MMMM yyyy à HH:mm' }}</span>
            <span class="spacer"></span>
            @if (estAuteur()) {
              <button mat-icon-button (click)="modifier()" matTooltip="Modifier"><mat-icon>edit</mat-icon></button>
            }
            @if (estAuteur() || auth.hasRole('ADMIN')) {
              <button mat-icon-button (click)="supprimer()" matTooltip="Supprimer"><mat-icon color="warn">delete</mat-icon></button>
            }
          </div>
          <h1 style="margin: 12px 0; font-weight: 500">{{ p.titre }}</h1>
          <p style="white-space: pre-line; line-height: 1.6">{{ p.contenu }}</p>
          <mat-divider />
          <div class="row mt">
            <app-reactions [post]="p" (reagir)="reagir($event)" />
            <span class="spacer"></span>
            <button mat-stroked-button (click)="resumer()" [disabled]="resumeEnCours()">
              <mat-icon>auto_awesome</mat-icon>{{ resumeEnCours() ? 'Résumé en cours...' : 'Résumer avec l\'IA' }}
            </button>
          </div>
          @if (resumeEnCours()) { <mat-progress-bar mode="indeterminate" class="mt" /> }
          @if (resume(); as r) {
            <div class="resume mt">
              <div class="resume-title"><mat-icon>auto_awesome</mat-icon> Résumé de la discussion</div>
              <p>{{ r }}</p>
            </div>
          }
        </mat-card>

        <h3>{{ p.nbCommentaires }} commentaire(s)</h3>
        <mat-card class="card card-pad" appearance="outlined">
          @for (c of p.commentaires ?? []; track c.id) {
            <div class="comment">
              <div class="avatar">{{ c.auteurNom.charAt(0) }}</div>
              <div style="flex: 1">
                <div class="small"><strong>{{ c.auteurNom }}</strong> <span class="muted">· {{ c.auteurEntreprise }} · {{ c.dateCreation | date: 'dd/MM/yyyy HH:mm' }}</span></div>
                <div style="white-space: pre-line">{{ c.contenu }}</div>
              </div>
              @if (c.auteurId === auth.user()?.id || auth.hasRole('ADMIN')) {
                <button mat-icon-button (click)="supprimerCommentaire(c)" matTooltip="Supprimer"><mat-icon>close</mat-icon></button>
              }
            </div>
          } @empty {
            <p class="muted">Soyez le premier à commenter.</p>
          }
          <div class="row mt">
            <mat-form-field appearance="outline" class="grow" subscriptSizing="dynamic">
              <mat-label>Votre commentaire</mat-label>
              <textarea matInput rows="2" [formControl]="commentaire" maxlength="2000"></textarea>
            </mat-form-field>
            <button mat-flat-button (click)="commenter()" [disabled]="commentaire.invalid"><mat-icon>send</mat-icon>Envoyer</button>
          </div>
        </mat-card>
      } @else {
        <mat-progress-bar mode="indeterminate" />
      }
    </div>
  `,
  styles: `
    .comment { display: flex; gap: 12px; padding: 12px 0; border-bottom: 1px solid var(--va-border); align-items: flex-start; }
    .avatar { width: 34px; height: 34px; border-radius: 50%; background: #e3f2fd; color: var(--va-primary);
      display: flex; align-items: center; justify-content: center; font-weight: 600; flex-shrink: 0; }
    .resume { background: #fff8e1; border-radius: 10px; padding: 10px 14px; }
    .resume p { white-space: pre-line; margin: 6px 0 0; line-height: 1.5; }
    .resume-title { display: flex; align-items: center; gap: 6px; font-weight: 500; color: #e65100; }
  `,
})
export class PostDetailComponent implements OnInit {
  auth = inject(AuthService);
  private forum = inject(ForumService);
  private notify = inject(NotifyService);
  private dialog = inject(MatDialog);
  private router = inject(Router);

  readonly id = input.required<string>();
  readonly post = signal<Post | null>(null);
  readonly estAuteur = computed(() => this.post()?.auteurId === this.auth.user()?.id);
  readonly commentaire = new FormControl('', { nonNullable: true, validators: [Validators.required] });
  private categories: string[] = [];
  private ai = inject(AiService);
  readonly resume = signal<string | null>(null);
  readonly resumeEnCours = signal(false);

  resumer(): void {
    this.resumeEnCours.set(true);
    this.ai.resumerPost(+this.id()).subscribe({
      next: r => {
        this.resume.set(r.texte);
        this.resumeEnCours.set(false);
      },
      error: err => {
        this.resumeEnCours.set(false);
        this.notify.error(err);
      },
    });
  }

  ngOnInit(): void {
    this.forum.categories().subscribe(c => (this.categories = c));
    this.forum.post(+this.id()).subscribe({
      next: p => this.post.set(p),
      error: err => {
        this.notify.error(err);
        this.router.navigate(['/forum']);
      },
    });
  }

  reagir(type: TypeReaction): void {
    this.forum.reagir(+this.id(), type).subscribe({
      next: maj => this.post.update(p => (p ? { ...p, likes: maj.likes, dislikes: maj.dislikes, maReaction: maj.maReaction } : p)),
      error: err => this.notify.error(err),
    });
  }

  commenter(): void {
    const texte = this.commentaire.value.trim();
    if (!texte) {
      return;
    }
    this.forum.commenter(+this.id(), texte).subscribe({
      next: p => {
        this.post.set(p);
        this.commentaire.reset();
      },
      error: err => this.notify.error(err),
    });
  }

  supprimerCommentaire(c: Commentaire): void {
    this.forum.supprimerCommentaire(c.id).subscribe({
      next: p => this.post.set(p),
      error: err => this.notify.error(err),
    });
  }

  modifier(): void {
    const p = this.post();
    this.dialog.open<PostDialogComponent, unknown, PostRequest>(PostDialogComponent, {
      data: { categories: this.categories, post: p }, width: '640px',
    }).afterClosed().subscribe(req => {
      if (req) {
        this.forum.modifier(+this.id(), req).subscribe({
          next: maj => {
            this.post.set(maj);
            this.notify.success('Publication modifiée');
          },
          error: err => this.notify.error(err),
        });
      }
    });
  }

  supprimer(): void {
    confirmer(this.dialog, { titre: 'Supprimer la publication', message: 'Cette action est définitive.', confirmer: 'Supprimer', danger: true })
      .subscribe(ok => {
        if (ok) {
          this.forum.supprimer(+this.id()).subscribe({
            next: () => {
              this.notify.success('Publication supprimée');
              this.router.navigate(['/forum']);
            },
            error: err => this.notify.error(err),
          });
        }
      });
  }
}
