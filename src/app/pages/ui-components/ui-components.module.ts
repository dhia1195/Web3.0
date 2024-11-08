import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MaterialModule } from '../../material.module';

// icons
import { TablerIconsModule } from 'angular-tabler-icons';
import * as TablerIcons from 'angular-tabler-icons/icons';

import { UiComponentsRoutes } from './ui-components.routing';

// ui components
import { AppBadgeComponent } from './badge/badge.component';
import { AppChipsComponent } from './chips/chips.component';
import { AppListsComponent } from './lists/lists.component';
import { AppMenuComponent } from './menu/menu.component';
import { AppTooltipsComponent } from './tooltips/tooltips.component';
import { MatNativeDateModule } from '@angular/material/core';
import { PlateformeComponent } from './plateforme/plateforme.component';
import { MethodeComponent } from './methode/methode.component';
import { TechnologieEducativeComponent } from './technologie-educative/technologie-educative.component';
import { CoursComponent } from './cours/cours.component';
import { EvaluationComponent } from './evaluation/evaluation.component';
import { ActiviteEducativeComponent } from './activite-educative/activite-educative.component';

@NgModule({
  imports: [
    CommonModule,
    RouterModule.forChild(UiComponentsRoutes),
    MaterialModule,
    FormsModule,
    ReactiveFormsModule,
    TablerIconsModule.pick(TablerIcons),
    MatNativeDateModule,
  ],
  declarations: [
    AppBadgeComponent,
    AppChipsComponent,
    AppListsComponent,
    AppMenuComponent,
    AppTooltipsComponent,
    PlateformeComponent,
    
    CoursComponent,
    EvaluationComponent,
    ActiviteEducativeComponent
    MethodeComponent,
    TechnologieEducativeComponent
  ],
})
export class UicomponentsModule {}