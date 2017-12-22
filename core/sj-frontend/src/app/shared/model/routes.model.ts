import { Route } from '@angular/router';

export interface AppRoute extends Route {
  children?: AppRoute[];
  hasPermissions?: string[];
  breadcrumb?: string;
  breadcrumbIgnore?: boolean;
}

export declare type AppRoutes = AppRoute[];
