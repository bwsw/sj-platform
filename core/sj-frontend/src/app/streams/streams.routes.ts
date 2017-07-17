import { StreamsComponent } from './streams.component';
import { AppRoutes } from '../shared/model/routes.model';

export const StreamsRoutes: AppRoutes = [{
  path: 'streams',
  breadcrumbIgnore: true,
  children: [
    { path: '', component: StreamsComponent, breadcrumb: 'Streams' }
  ]
}];

