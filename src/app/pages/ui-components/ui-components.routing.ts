import { Routes } from '@angular/router';

// ui
import { AppBadgeComponent } from './badge/badge.component';
import { AppChipsComponent } from './chips/chips.component';
import { AppListsComponent } from './lists/lists.component';
import { AppMenuComponent } from './menu/menu.component';
import { AppTooltipsComponent } from './tooltips/tooltips.component';
import { PlateformeComponent } from './plateforme/plateforme.component';
import { TechnologieEducativeComponent } from './technologie-educative/technologie-educative.component';
import { CoursComponent } from './cours/cours.component';
import { EvaluationComponent } from './evaluation/evaluation.component';
import { ActiviteEducativeComponent } from './activite-educative/activite-educative.component';

export const UiComponentsRoutes: Routes = [
  {
    path: '',
    children: [
      {
        path: 'badge',
        component: AppBadgeComponent,
      },
      {
        path: 'chips',
        component: AppChipsComponent,
      },
      {
        path: 'lists',
        component: AppListsComponent,
      },
      {
        path: 'menu',
        component: AppMenuComponent,
      },
      {
        path: 'tooltips',
        component: AppTooltipsComponent,
      },
      {
        path: 'plateforme',
        component: PlateformeComponent,
      },
      {
        path: 'cours',
        component: CoursComponent,
      },
      {
        path: 'evaluation',
        component: EvaluationComponent,
      },

      {
        path: 'activite-educative',
        component: ActiviteEducativeComponent,
      },      {
        path: 'technologie',
        component: TechnologieEducativeComponent,
      },


    ],
  },
];
