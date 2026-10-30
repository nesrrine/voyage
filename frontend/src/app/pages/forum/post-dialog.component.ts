import { Component, inject, signal } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { AiService } from '../../core/api.service';
import { Post, PostRequest } from '../../core/models';
import { SHARED } from '../../shared/shared';

export interface PostDialogData {
  categories: string[];
  post?: Post;
}

/**
 * Creation / modification d'une publication du forum. Renvoie un PostRequest a la fermeture.
 * Si l'IA est configuree, le message est verifie par la moderation automatique avant publication.
 */
@Component({
  selector: 'app-post-dialog',
  imports: [SHARED],
  template: `
    <h2 mat-dialog-title>{{ data.post ? 'Modifier la publication' : 'Nouvelle publication' }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" id="post-form" (ngSubmit)="valider()" style="padding-top: 8px">
        <mat-form-field appearance="outline" class="full">
          <mat-label>Titre</mat-label>
          <input matInput formControlName="titre" maxlength="200">
          <mat-error>Le titre est obligatoire</mat-error>
        </mat-form-field>
        <mat-form-field appearance="outline" class="full">
          <mat-label>Catégorie</mat-label>
          <mat-select formControlName="categorie">
            @for (c of data.categories; track c) { <mat-option [value]="c">{{ c }}</mat-option> }
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline" class="full">
          <mat-label>Message</mat-label>
          <textarea matInput rows="7" formControlName="contenu" maxlength="5000"></textarea>
          <mat-hint align="end">{{ form.controls.contenu.value.length }} / 5000</mat-hint>
          <mat-error>Le message est obligatoire</mat-error>
        </mat-form-field>
      </form>
      @if (moderation()) { <mat-progress-bar mode="indeterminate" /> <p class="muted small">Vérification par la modération IA...</p> }
      @if (refus(); as raison) {
        <div class="badge red" style="padding: 10px 12px; border-radius: 8px; white-space: normal; display: flex">
          <mat-icon>gpp_bad</mat-icon>
          <span>Publication refusée par la modération automatique : {{ raison }}</span>
        </div>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Annuler</button>
      <button mat-flat-button type="submit" form="post-form" [disabled]="moderation()">Publier</button>
    </mat-dialog-actions>
  `,
})
export class PostDialogComponent {
  data = inject<PostDialogData>(MAT_DIALOG_DATA);
  private ref = inject(MatDialogRef<PostDialogComponent, PostRequest>);
  private fb = inject(FormBuilder);
  private ai = inject(AiService);

  readonly moderation = signal(false);
  readonly refus = signal<string | null>(null);

  form = this.fb.nonNullable.group({
    titre: [this.data.post?.titre ?? '', Validators.required],
    categorie: [this.data.post?.categorie ?? this.data.categories[0] ?? 'General'],
    contenu: [this.data.post?.contenu ?? '', Validators.required],
  });

  valider(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const valeur = this.form.getRawValue();
    if (!this.ai.configure()) {
      this.ref.close(valeur);
      return;
    }
    this.moderation.set(true);
    this.refus.set(null);
    this.ai.moderer(valeur.titre, valeur.contenu).subscribe({
      next: r => {
        this.moderation.set(false);
        if (r.acceptable) {
          this.ref.close(valeur);
        } else {
          this.refus.set(r.raison ?? 'contenu inapproprié');
        }
      },
      // Si le service IA est indisponible, on ne bloque pas la publication
      error: () => {
        this.moderation.set(false);
        this.ref.close(valeur);
      },
    });
  }
}
