import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, catchError, of, tap } from 'rxjs';
import { ApiService, User } from './api.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  private readonly userSubject = new BehaviorSubject<User | null>(null);
  readonly user$ = this.userSubject.asObservable();

  get token(): string | null { return localStorage.getItem('fo_token'); }
  get isAuthenticated(): boolean { return !!this.token; }
  login(email: string, password: string) { return this.api.login(email, password).pipe(tap((response) => { localStorage.setItem('fo_token', response.access_token); this.userSubject.next(response.user); })); }
  restore() { return this.api.me().pipe(tap((user) => this.userSubject.next(user)), catchError(() => { this.logout(false); return of(null); })); }
  logout(navigate = true): void { localStorage.removeItem('fo_token'); this.userSubject.next(null); if (navigate) void this.router.navigateByUrl('/login'); }
}
