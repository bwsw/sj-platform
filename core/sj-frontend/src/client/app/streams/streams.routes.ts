import { Routes } from '@angular/router';

import { StreamsComponent } from './streams.component';

export const StreamsRoutes: Routes = [{
  path: 'streams',
  children: [
    { path: '', component: StreamsComponent }
  ]
}];

