import { Component, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService, Dashboard } from '../core/api.service';

@Component({ selector: 'fo-dashboard', standalone: true, imports: [DatePipe, RouterLink], template: `
  @if (data; as d) {
    <section class="page-shell">
      <div class="hero"><div><p class="eyebrow">PANEL DE CONTROL</p><h1>Hola, {{ d.user.nombre.split(' ')[0] }} <span class="wave">✦</span></h1><p class="muted">Todo lo que necesitas para seguir haciendo historia.</p></div><div class="hero-chip"><span class="status-dot"></span> Sistema operativo</div></div>
      <div class="metric-grid"><article class="metric-card accent"><span class="metric-icon">⚡</span><p>Electronios</p><strong>{{ d.user.electronios ?? 0 }}</strong><small>Tu saldo actual</small></article><article class="metric-card"><span class="metric-icon">◎</span><p>Nivel</p><strong>{{ d.user.nivel ?? 0 }}</strong><small>{{ d.user.exp ?? 0 }} XP acumulados</small></article><article class="metric-card"><span class="metric-icon">▣</span><p>Cartas disponibles</p><strong>{{ d.cards_available }}</strong><small>En la tienda</small></article><article class="metric-card"><span class="metric-icon">♢</span><p>Mi colección</p><strong>{{ d.owned_cards }}</strong><small>Cartas adquiridas</small></article></div>
      <div class="content-grid"><article class="surface activity"><div class="section-heading"><div><p class="eyebrow">ÚLTIMOS MOVIMIENTOS</p><h2>Tu actividad</h2></div><a routerLink="/cards">Ver cartas →</a></div>
        @if (d.recent_history.length) {
          @for (item of d.recent_history; track item.id) { <div class="activity-row"><span class="activity-icon">✦</span><div><strong>{{ item.descripcion || 'Actividad registrada' }}</strong><small>{{ item.fecha | date:'dd/MM/yyyy · HH:mm' }}</small></div><b>{{ item.electronios_en_momento }} ⚡</b></div> }
        } @else { <div class="empty-state">Todavía no hay actividad registrada.</div> }
      </article><aside class="surface profile-card"><div class="profile-avatar">{{ d.user.nombre.charAt(0).toUpperCase() }}</div><p class="eyebrow">TU PERFIL</p><h2>{{ d.user.nombre }}</h2><span class="role-pill">{{ d.user.rol }}</span><div class="profile-line"><span>Curso</span><b>{{ d.user.curso || '—' }}</b></div><div class="profile-line"><span>Correo</span><b>{{ d.user.email }}</b></div></aside></div>
    </section>
  } @else { <div class="loading">Cargando tu espacio…</div> }
`, styles: [] })
export class DashboardComponent { private readonly api = inject(ApiService); data: Dashboard | null = null; constructor() { this.api.dashboard().subscribe({ next: (data) => this.data = data }); } }
