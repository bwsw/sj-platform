import { Routes } from '@angular/router';

import { ProvidersRoutes } from './providers/providers.routes';
import { ServicesRoutes } from './services/services.routes';
import { StreamsRoutes } from './streams/streams.routes';
import { ModulesRoutes } from './modules/modules.routes';
import { InstancesRoutes } from './instances/instances.routes';

export const routes: Routes = [
  ...ProvidersRoutes,
  ...ServicesRoutes,
  ...StreamsRoutes,
  ...ModulesRoutes,
  ...InstancesRoutes,
  { path: '', redirectTo: '/providers', pathMatch: 'full' }
];
