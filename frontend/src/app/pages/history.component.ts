import { Component, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ApiService, HistoryItem } from '../core/api.service';

@Component({ selector: 'fo-history', standalone: true, imports: [DatePipe], template: `<section class="page-shell"><div class="hero"><div><p class="eyebrow">SEGUIMIENTO</p><h1>Historial</h1><p class="muted">Todo tu progreso, en un solo lugar.</p></div></div><article class="surface history-list">@for (item of items; track item.id) { <div class="activity-row"><span class="activity-icon">✦</span><div><strong>{{ item.descripcion || 'Actividad registrada' }}</strong><small>{{ item.fecha | date:'dd/MM/yyyy · HH:mm' }}</small></div><b>{{ item.electronios_en_momento }} ⚡</b></div> } @empty { <div class="empty-state">Todavía no hay movimientos.</div> }</article></section>`, styles: [] })
export class HistoryComponent { private readonly api = inject(ApiService); items: HistoryItem[] = []; constructor() { this.api.history().subscribe({ next: (items) => this.items = items }); } }
