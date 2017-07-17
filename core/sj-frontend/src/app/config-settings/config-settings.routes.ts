import { ConfigSettingsComponent } from './config-settings.component';
import { AppRoutes } from '../shared/model/routes.model';

export const ConfigSettingsRoutes: AppRoutes = [{
  path: 'config/settings',
  breadcrumbIgnore: true,
  children: [
    { path: '', component: ConfigSettingsComponent, breadcrumb: 'Config Settings' }
  ]
}];
