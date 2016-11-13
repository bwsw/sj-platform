
import { Component, OnInit } from '@angular/core';
import { Router, NavigationEnd, ActivatedRouteSnapshot } from '@angular/router';
import { AppRoute } from '../../models/routes.model';

interface IRouteItem {
  name: string;
  path: string;
}

@Component({
  selector: 'sj-breadcrumbs',
  moduleId: module.id,
  templateUrl: 'breadcrumbs.component.html'
})
export class BreadcrumbsComponent implements OnInit {
  private _routes: IRouteItem[];
  private _currentRoute: IRouteItem;

  public get routes() {
    return this._routes;
  }

  public get currentRoute() {
    return this._currentRoute;
  }

  constructor(private _router: Router) {
  }

  public ngOnInit() {
    // Apply next state only if navigation was ended
    this._router.events
      .filter(event => event instanceof NavigationEnd)
      .subscribe(this._applyNextState.bind(this));
  }

  private _applyNextState() {
    let routes: IRouteItem[] = [];
    let path: string[] = [];

    let child = this._router.routerState.snapshot.root.firstChild;

    while (child) {
      let routeConfig = <AppRoute>child.routeConfig;

      if (!routeConfig.breadcrumbIgnore) {
        let name = this._extractRouteName(child);
        let pathPart = child.url.join('/');

        if (name || pathPart) {
          path.push(pathPart);
          routes.push({
            name: name,
            path: path.join('/')
          });
        }
      }

      child = child.firstChild;
    }

    this._currentRoute = routes.pop();
    this._routes = routes;
  }

  private _extractRouteName(route: ActivatedRouteSnapshot) {
    let routeConfig = <AppRoute>route.routeConfig;
    let breadcrumbName = routeConfig.breadcrumb;

    return breadcrumbName || this._extractRouteNameFallback(route);
  }

  private _extractRouteNameFallback(route: ActivatedRouteSnapshot) {
    if (route.component) {
      let componentName = typeof route.component === 'string' ? <string>route.component : (<Function>route.component).name;
      return this._prepareRouteName(componentName);
    } else {
      return this._prepareRouteName(route.url.join('/'));
    }
  }

  private _prepareRouteName(name: string) {
    if (name.length > 0) {
      name = name.replace('-', ' ').replace('Component', '').replace(/\/\d+/, '');
      name = name.split(' ').map(this._capitalizeFirstLetter).join('');
      name = name.split(/(?=[A-Z])/).join(' ');
    }
    return name;
  }

  private _capitalizeFirstLetter(str: string) {
    return str[0].toUpperCase() + str.slice(1);
  }
}
