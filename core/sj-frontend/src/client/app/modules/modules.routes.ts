import { ModulesComponent } from './modules.component';
import { Routes } from '@angular/router';

export const ModulesRoutes: Routes = [{
  path: 'modules',
  children: [
    { path: '', component: ModulesComponent }
  ]
}];

