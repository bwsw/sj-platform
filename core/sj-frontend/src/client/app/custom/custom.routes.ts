import { AppRoutes } from '../shared/models/routes.model';
import { CustomComponent } from './custom.component';

export const CustomRoutes: AppRoutes = [{
  path: 'custom',
  breadcrumbIgnore: true,
  children: [
    { path: ':path', component: CustomComponent, breadcrumb: 'Custom files' },
    { path: '', component: CustomComponent }
  ]
}];
