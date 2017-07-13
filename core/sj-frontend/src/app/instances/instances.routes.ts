import { InstancesComponent } from './instances.component';
import { AppRoutes } from '../shared/model/routes.model';

export const InstancesRoutes: AppRoutes = [{
  path: 'instances',
  breadcrumbIgnore: true,
  children: [
    { path: '', component: InstancesComponent, breadcrumb: 'Instances' }
  ]
}];

