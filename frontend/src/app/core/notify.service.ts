import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { problemMessage } from './http/problem';

@Injectable({ providedIn: 'root' })
export class NotifyService {
  private readonly snackBar = inject(MatSnackBar);

  success(message: string): void {
    this.snackBar.open(message, 'OK', { duration: 4000, panelClass: 'fl-snack-success' });
  }

  error(error: unknown): void {
    this.snackBar.open(problemMessage(error), 'Fechar', { duration: 7000, panelClass: 'fl-snack-error' });
  }
}
