import { AppRoutes } from '../shared/models/routes.model';
import { ConfigSettingsComponent } from './config-settings.component';

export const ConfigSettingsRoutes: AppRoutes = [{
  path: 'config/settings',
  breadcrumbIgnore: true,
  children: [
    { path: '', component: ConfigSettingsComponent, breadcrumb: 'Config Settings' }
  ]
}];
