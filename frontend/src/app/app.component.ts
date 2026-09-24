import { AsyncPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth.service';

@Component({
  selector: 'fo-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, AsyncPipe],
  template: `
    @if (auth.isAuthenticated) {
      <header class="topbar">
        <a class="brand" routerLink="/" (click)="closeMenu()">✦ Figuras <span>Ocultas</span></a>
        <button
          class="menu-toggle"
          type="button"
          [attr.aria-expanded]="menuOpen"
          aria-controls="main-navigation"
          aria-label="Abrir menú de navegación"
          (click)="toggleMenu()"
        >
          <span aria-hidden="true">{{ menuOpen ? '×' : '☰' }}</span>
        </button>
        <nav id="main-navigation" [class.menu-open]="menuOpen" aria-label="Navegación principal">
          <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{exact:true}" (click)="closeMenu()">Inicio</a>
          <a routerLink="/cards" routerLinkActive="active" (click)="closeMenu()">Cartas</a>
          <a routerLink="/inventory" routerLinkActive="active" (click)="closeMenu()">Inventario</a>
          <a routerLink="/history" routerLinkActive="active" (click)="closeMenu()">Historial</a>
          @if (auth.user$ | async; as user) {
            @if (user.rol === 'ADMIN') {
              <a routerLink="/students" routerLinkActive="active" (click)="closeMenu()">Alumnos</a>
            }
          }
          <a routerLink="/profile" routerLinkActive="active" (click)="closeMenu()">Perfil</a>
          <button class="mobile-logout" type="button" (click)="logout()">Salir</button>
        </nav>
        <button class="user-button desktop-logout" type="button" (click)="logout()">Salir</button>
      </header>
    }
    <main class="app-main"><router-outlet /></main>
  `,
})
export class AppComponent {
  readonly auth = inject(AuthService);
  menuOpen = false;

  toggleMenu(): void {
    this.menuOpen = !this.menuOpen;
  }

  closeMenu(): void {
    this.menuOpen = false;
  }

  logout(): void {
    this.closeMenu();
    this.auth.logout();
  }
}
