import { Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Post, TypeReaction } from '../../core/models';

/** Boutons Like / Dislike d'une publication. */
@Component({
  selector: 'app-reactions',
  imports: [MatButtonModule, MatIconModule, MatTooltipModule],
  template: `
    <button mat-button class="r" [class.like]="post().maReaction === 'LIKE'" (click)="reagir.emit('LIKE')" matTooltip="J'aime">
      <mat-icon>{{ post().maReaction === 'LIKE' ? 'thumb_up' : 'thumb_up_off_alt' }}</mat-icon>{{ post().likes }}
    </button>
    <button mat-button class="r" [class.dislike]="post().maReaction === 'DISLIKE'" (click)="reagir.emit('DISLIKE')" matTooltip="Je n'aime pas">
      <mat-icon>{{ post().maReaction === 'DISLIKE' ? 'thumb_down' : 'thumb_down_off_alt' }}</mat-icon>{{ post().dislikes }}
    </button>
  `,
  styles: `
    :host { display: inline-flex; gap: 4px; }
    .r { --mdc-text-button-label-text-color: #475467; min-width: 0; }
    .like { --mdc-text-button-label-text-color: #1565c0; }
    .dislike { --mdc-text-button-label-text-color: #c62828; }
  `,
})
export class ReactionsComponent {
  readonly post = input.required<Post>();
  readonly reagir = output<TypeReaction>();
}
