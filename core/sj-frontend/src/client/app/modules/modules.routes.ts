import { AppRoutes } from '../shared/models/routes.model';
import { ModulesComponent } from './modules.component';

export const ModulesRoutes: AppRoutes = [{
  path: 'modules',
  breadcrumbIgnore: true,
  children: [
    { path: '', component: ModulesComponent, breadcrumb: 'Modules' }
  ]
}];

