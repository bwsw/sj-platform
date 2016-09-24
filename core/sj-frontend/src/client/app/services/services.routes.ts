import { Routes } from '@angular/router';

import { ServicesComponent } from './services.component';

export const ServicesRoutes: Routes = [{
  path: 'services',
  children: [
    { path: '', component: ServicesComponent }
  ]
}];

