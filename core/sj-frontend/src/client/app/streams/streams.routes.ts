import { AppRoutes } from '../shared/models/routes.model';
import { StreamsComponent } from './streams.component';

export const StreamsRoutes: AppRoutes = [{
  path: 'streams',
  breadcrumbIgnore: true,
  children: [
    { path: '', component: StreamsComponent, breadcrumb: 'Streams' }
  ]
}];

