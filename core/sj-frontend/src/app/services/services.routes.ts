import { AppRoutes } from '../shared/model/routes.model';
import { ServicesComponent } from './services.component';

export const ServicesRoutes: AppRoutes = [{
  path: 'services',
  breadcrumbIgnore: true,
  children: [
    { path: '', component: ServicesComponent, breadcrumb: 'Services' }
  ]
}];

