import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface User { id: number; nombre: string; email: string; rol: string; curso?: string; nivel?: number; exp?: number; electronios?: number; }
export interface Card { id: number; titulo: string; descripcion?: string; precio: number; activa: boolean; image_url?: string; quantity: number; }
export interface HistoryItem { id: number; tipo?: string; descripcion?: string; electronios_en_momento: number; fecha: string; }
export interface Badge { id: number; nombre: string; image_url: string; }
export interface ProfileUpdate { nombre: string; email: string; password?: string; }
export interface StudentCreate { nombre: string; email: string; password: string; curso?: string; }
export interface StudentUpdate { nombre?: string; email?: string; password?: string; curso?: string; exp?: number; electronios?: number; }
export interface Dashboard { user: User; web_name: string; cards_available: number; students_count: number; owned_cards: number; recent_history: HistoryItem[]; }

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient); private readonly base = '/api';
  login(email: string, password: string): Observable<{ access_token: string; user: User }> { return this.http.post<{ access_token: string; user: User }>(`${this.base}/auth/login`, { email, password }); }
  me(): Observable<User> { return this.http.get<User>(`${this.base}/auth/me`); }
  dashboard(): Observable<Dashboard> { return this.http.get<Dashboard>(`${this.base}/dashboard`); }
  cards(): Observable<Card[]> { return this.http.get<Card[]>(`${this.base}/cards`); }
  buyCard(id: number): Observable<{ message: string }> { return this.http.post<{ message: string }>(`${this.base}/cards/${id}/buy`, {}); }
  inventory(): Observable<Card[]> { return this.http.get<Card[]>(`${this.base}/inventory`); }
  useCard(id: number): Observable<{ message: string }> { return this.http.post<{ message: string }>(`${this.base}/inventory/${id}/use`, {}); }
  history(): Observable<HistoryItem[]> { return this.http.get<HistoryItem[]>(`${this.base}/history`); }
  updateProfile(payload: ProfileUpdate): Observable<User> { return this.http.patch<User>(`${this.base}/profile`, payload); }
  students(): Observable<User[]> { return this.http.get<User[]>(`${this.base}/students`); }
  createStudent(payload: StudentCreate): Observable<User> { return this.http.post<User>(`${this.base}/students`, payload); }
  updateStudent(id: number, payload: StudentUpdate): Observable<User> { return this.http.patch<User>(`${this.base}/students/${id}`, payload); }
  deleteStudent(id: number): Observable<void> { return this.http.delete<void>(`${this.base}/students/${id}`); }
  awardExperience(payload: { amount: number; curso?: string; student_ids?: number[] }): Observable<{ updated: number }> { return this.http.post<{ updated: number }>(`${this.base}/students/award-experience`, payload); }
  updateConfig(nombre: string): Observable<{ name: string }> { return this.http.patch<{ name: string }>(`${this.base}/config`, { nombre }); }
  badges(studentId: number): Observable<Badge[]> { return this.http.get<Badge[]>(`${this.base}/badges/${studentId}`); }
  createBadge(studentId: number, nombre: string, imagen_base64?: string): Observable<Badge> { return this.http.post<Badge>(`${this.base}/students/${studentId}/badges`, { nombre, imagen_base64 }); }
  deleteBadge(id: number): Observable<void> { return this.http.delete<void>(`${this.base}/badges/${id}`); }
}
