import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';
import { LoginComponent } from './pages/login.component';
import { DashboardComponent } from './pages/dashboard.component';
import { CardsComponent } from './pages/cards.component';
import { InventoryComponent } from './pages/inventory.component';
import { HistoryComponent } from './pages/history.component';
import { StudentsComponent } from './pages/students.component';
import { ProfileComponent } from './pages/profile.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: '', canActivate: [authGuard], component: DashboardComponent },
  { path: 'cards', canActivate: [authGuard], component: CardsComponent },
  { path: 'inventory', canActivate: [authGuard], component: InventoryComponent },
  { path: 'history', canActivate: [authGuard], component: HistoryComponent },
  { path: 'students', canActivate: [authGuard], component: StudentsComponent },
  { path: 'profile', canActivate: [authGuard], component: ProfileComponent },
  { path: '**', redirectTo: '' },
];
