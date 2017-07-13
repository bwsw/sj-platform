import { CustomComponent } from './custom.component';
import { AppRoutes } from '../shared/model/routes.model';

export const CustomRoutes: AppRoutes = [{
  path: 'custom',
  breadcrumbIgnore: true,
  children: [
    { path: ':path', component: CustomComponent, breadcrumb: 'Custom files' },
    { path: '', component: CustomComponent }
  ]
}];
