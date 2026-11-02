import { inject, Injectable } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { MatSnackBar } from '@angular/material/snack-bar';

@Injectable({ providedIn: 'root' })
export class NotifyService {
  private snack = inject(MatSnackBar);

  success(message: string): void {
    this.snack.open(message, 'OK', { duration: 3500, panelClass: 'success-snack' });
  }

  error(err: unknown, fallback = 'Une erreur est survenue'): void {
    this.snack.open(NotifyService.message(err, fallback), 'Fermer', { duration: 6000, panelClass: 'error-snack' });
  }

  /** Extrait un message lisible d'une erreur HTTP renvoyee par l'API. */
  static message(err: unknown, fallback = 'Une erreur est survenue'): string {
    if (err instanceof HttpErrorResponse) {
      if (err.status === 0) {
        return 'Serveur injoignable : vérifiez que le backend Spring Boot est démarré';
      }
      const body = err.error as { message?: string; erreurs?: Record<string, string> } | null;
      if (body?.erreurs) {
        return Object.entries(body.erreurs).map(([champ, msg]) => `${champ} : ${msg}`).join(' • ');
      }
      if (body?.message) {
        return body.message;
      }
      // Gateway ou microservice pas encore pret (demarrage des conteneurs, inscription dans Eureka)
      if (err.status === 502 || err.status === 503 || err.status === 504) {
        return 'Le serveur démarre encore : réessayez dans une minute';
      }
    }
    return fallback;
  }
}
