import { Component, inject, OnInit, signal } from '@angular/core';
import { FormControl } from '@angular/forms';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { debounceTime } from 'rxjs';
import { ForumService } from '../../core/api.service';
import { NotifyService } from '../../core/notify.service';
import { Post, PostRequest, TypeReaction } from '../../core/models';
import { SHARED } from '../../shared/shared';
import { PostDialogComponent } from './post-dialog.component';
import { ReactionsComponent } from './reactions.component';

@Component({
  selector: 'app-forum-list',
  imports: [SHARED, ReactionsComponent],
  templateUrl: './forum-list.component.html',
  styles: `
    .post { padding: 18px 20px; display: flex; flex-direction: column; gap: 8px; }
    .post h3 { margin: 0; font-size: 18px; font-weight: 500; }
    .post h3 a { color: inherit; text-decoration: none; }
    .post h3 a:hover { color: var(--va-primary); }
    .excerpt { margin: 0; color: #344054; display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; }
    .meta { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; font-size: 13px; color: var(--va-muted); }
    .foot { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
    .cats { display: flex; gap: 6px; flex-wrap: wrap; }
    .cat { cursor: pointer; border: none; }
    .cat.on { background: var(--va-primary); color: #fff; }
  `,
})
export class ForumListComponent implements OnInit {
  private forum = inject(ForumService);
  private notify = inject(NotifyService);
  private dialog = inject(MatDialog);
  private router = inject(Router);

  readonly posts = signal<Post[]>([]);
  readonly categories = signal<string[]>([]);
  readonly categorie = signal('');
  readonly tri = signal('recent');
  readonly loading = signal(true);
  readonly recherche = new FormControl('', { nonNullable: true });

  ngOnInit(): void {
    this.forum.categories().subscribe(c => this.categories.set(c));
    this.recherche.valueChanges.pipe(debounceTime(300)).subscribe(() => this.charger());
    this.charger();
  }

  charger(): void {
    this.loading.set(true);
    this.forum.posts(this.recherche.value, this.categorie(), this.tri()).subscribe({
      next: p => {
        this.posts.set(p);
        this.loading.set(false);
      },
      error: err => {
        this.loading.set(false);
        this.notify.error(err);
      },
    });
  }

  choisirCategorie(c: string): void {
    this.categorie.set(this.categorie() === c ? '' : c);
    this.charger();
  }

  trier(t: string): void {
    this.tri.set(t);
    this.charger();
  }

  nouvellePublication(): void {
    this.dialog.open<PostDialogComponent, unknown, PostRequest>(PostDialogComponent, {
      data: { categories: this.categories() }, width: '640px',
    }).afterClosed().subscribe(req => {
      if (req) {
        this.forum.creer(req).subscribe({
          next: p => {
            this.notify.success('Publication créée');
            this.router.navigate(['/forum', p.id]);
          },
          error: err => this.notify.error(err),
        });
      }
    });
  }

  reagir(p: Post, type: TypeReaction): void {
    this.forum.reagir(p.id, type).subscribe({
      next: maj => this.posts.update(list => list.map(x => (x.id === maj.id ? { ...x, ...maj, commentaires: null } : x))),
      error: err => this.notify.error(err),
    });
  }
}
