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

  constructor(private router: Router) {
  }

  public ngOnInit() {
    // Apply next state only if navigation was ended
    this.router.events
      .filter(event => event instanceof NavigationEnd)
      .subscribe(this.applyNextState.bind(this));
  }

  private applyNextState() {
    let routes: IRouteItem[] = [];
    let path: string[] = [];

    let child = this.router.routerState.snapshot.root.firstChild;

    while (child) {
      let routeConfig = <AppRoute>child.routeConfig;

      if (!routeConfig.breadcrumbIgnore) {
        let name = this.extractRouteName(child);
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

  private extractRouteName(route: ActivatedRouteSnapshot) {
    let routeConfig = <AppRoute>route.routeConfig;
    let breadcrumbName = routeConfig.breadcrumb;

    return breadcrumbName || this.extractRouteNameFallback(route);
  }

  private extractRouteNameFallback(route: ActivatedRouteSnapshot) {
    if (route.component) {
      let componentName = typeof route.component === 'string' ? <string>route.component : (<Function>route.component).name;
      return this.prepareRouteName(componentName);
    } else {
      return this.prepareRouteName(route.url.join('/'));
    }
  }

  private prepareRouteName(name: string) {
    if (name.length > 0) {
      name = name.replace('-', ' ').replace('Component', '').replace(/\/\d+/, '');
      name = name.split(' ').map(this.capitalizeFirstLetter).join('');
      name = name.split(/(?=[A-Z])/).join(' ');
    }
    return name;
  }

  private capitalizeFirstLetter(str: string) {
    return str[0].toUpperCase() + str.slice(1);
  }
}
