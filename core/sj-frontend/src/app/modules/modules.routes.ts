import { ModulesComponent } from './modules.component';
import { AppRoutes } from '../shared/model/routes.model';

export const ModulesRoutes: AppRoutes = [{
  path: 'modules',
  breadcrumbIgnore: true,
  children: [
    { path: '', component: ModulesComponent, breadcrumb: 'Modules' }
  ]
}];

