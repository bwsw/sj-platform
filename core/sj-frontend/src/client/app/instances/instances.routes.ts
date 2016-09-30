import { Routes } from '@angular/router';
import { InstancesComponent } from './instances.component';

export const InstancesRoutes: Routes = [{
  path: 'instances',
  children: [
    { path: '', component: InstancesComponent }
  ]
}];

