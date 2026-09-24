import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../core/auth.service';

@Component({ selector: 'fo-login', standalone: true, imports: [ReactiveFormsModule], template: `
  <section class="login-page"><div class="login-art"><div class="orb orb-one"></div><div class="orb orb-two"></div><p class="eyebrow">PLATAFORMA EDUCATIVA</p><h1>Aprender también<br><em>puede ser un juego.</em></h1><p class="muted">Convierte cada reto en progreso y cada progreso en una historia.</p></div>
  <div class="login-panel"><div class="login-card"><div class="logo-mark">✦</div><p class="eyebrow">BIENVENIDO DE NUEVO</p><h2>Inicia sesión</h2><p class="muted">Accede a tu espacio en Figuras Ocultas.</p><form [formGroup]="form" (ngSubmit)="submit()"><label>Correo electrónico<input type="email" formControlName="email" placeholder="tu@email.com"></label><label>Contraseña<input type="password" formControlName="password" placeholder="••••••••"></label>@if (error) { <p class="error">{{ error }}</p> }<button class="primary-button" type="submit" [disabled]="form.invalid || loading">{{ loading ? 'Entrando…' : 'Entrar en mi cuenta →' }}</button></form></div></div></section>
`, styles: [] })
export class LoginComponent {
  private readonly fb = inject(FormBuilder); private readonly auth = inject(AuthService); private readonly router = inject(Router);
  readonly form = this.fb.nonNullable.group({ email: ['', [Validators.required, Validators.email]], password: ['', Validators.required] }); loading = false; error = '';
  submit(): void { if (this.form.invalid) return; this.loading = true; this.error = ''; const { email, password } = this.form.getRawValue(); this.auth.login(email, password).subscribe({ next: () => void this.router.navigateByUrl('/'), error: (err: { error?: { detail?: string } }) => { this.error = err.error?.detail ?? 'No se pudo iniciar sesión.'; this.loading = false; } }); }
}
