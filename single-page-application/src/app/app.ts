import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';

import { SessionManager } from './session-manager/session-manager';

@Component({
  selector: 'app-root',
  templateUrl: './app.html',
  standalone: false,
  styleUrl: './app.css',
})
export class App {
  protected readonly sesion = inject(SessionManager);
  private readonly router = inject(Router);

  cerrarSesion(): void {
    this.sesion.cerrarSesion();
    this.router.navigateByUrl('/login');
  }
}
