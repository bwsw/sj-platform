import { AppRoutes } from './shared';
import { ProvidersRoutes } from './providers/providers.routes';
import { ServicesRoutes } from './services/services.routes';
import { StreamsRoutes } from './streams/streams.routes';
import { ModulesRoutes } from './modules/modules.routes';
import { InstancesRoutes } from './instances/instances.routes';
import { ConfigSettingsRoutes } from './config-settings/config-settings.routes';
import { CustomRoutes } from './custom/custom.routes';

export const routes: AppRoutes = [{
  path: '',
  breadcrumb: 'Stream Juggler',
  children: [
    { path: '', redirectTo: '/providers', pathMatch: 'full' },
    ...ProvidersRoutes,
    ...ServicesRoutes,
    ...StreamsRoutes,
    ...ModulesRoutes,
    ...InstancesRoutes,
    ...ConfigSettingsRoutes,
    ...CustomRoutes,
  ]
}];
