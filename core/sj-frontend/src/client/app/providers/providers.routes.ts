import { Routes } from '@angular/router';

import { ProvidersComponent } from './providers.component';

export const ProvidersRoutes: Routes = [{
  path: 'providers',
  children: [
    { path: '', component: ProvidersComponent }
  ]
}];

