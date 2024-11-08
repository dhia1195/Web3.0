import { Routes } from '@angular/router';
import { AppDashboardComponent } from './dashboard/dashboard.component';
import { PersonneComponent } from './personne/personne.component';

export const PagesRoutes: Routes = [
  {
    path: '',
    component: AppDashboardComponent,
    data: {
      title: 'Starter Page',
    },
  },
  {
    path: 'personne',
    component: PersonneComponent,
    data: {
      title: 'Personne Page',
    },
  },
];
