import { AppRoutes } from '../shared/models/routes.model';
import { InstancesComponent } from './instances.component';

export const InstancesRoutes: AppRoutes = [{
  path: 'instances',
  breadcrumbIgnore: true,
  children: [
    { path: '', component: InstancesComponent, breadcrumb: 'Instances' }
  ]
}];

