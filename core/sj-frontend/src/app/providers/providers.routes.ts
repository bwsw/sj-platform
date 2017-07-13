import { AppRoutes } from '../shared/model/routes.model';
import { ProvidersComponent } from './providers.component';

export const ProvidersRoutes: AppRoutes = [{
  path: 'providers',
  breadcrumbIgnore: true,
  children: [
    { path: '', component: ProvidersComponent, breadcrumb: 'Providers' }
  ]
}];

