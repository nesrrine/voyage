import { Pipe, PipeTransform } from '@angular/core';
import { libelle } from '../core/labels';

/** Affiche le libelle francais d'une cle d'enum : {{ voyage.objet | libelle }}. */
@Pipe({ name: 'libelle' })
export class LibellePipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    return value ? libelle(value) : '';
  }
}
