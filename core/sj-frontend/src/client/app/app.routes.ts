import { provideRouter, RouterConfig } from '@angular/router';

import { ProvidersRoutes } from './providers/index';
import { ServicesRoutes } from './services/index';
import { StreamsRoutes } from './streams/index';
import { ModulesRoutes } from './modules/index';
import { InstancesRoutes } from './instances/index';

const routes: RouterConfig = [
  ...ProvidersRoutes,
  ...ServicesRoutes,
  ...StreamsRoutes,
  ...ModulesRoutes,
  ...InstancesRoutes,
  {
    path: '',
    redirectTo: '/providers',
    pathMatch: 'full'
  }
];

export const APP_ROUTER_PROVIDERS = [
  provideRouter(routes),
];
