import { Component, ViewContainerRef } from '@angular/core';

@Component({
  moduleId: module.id,
  selector: 'sj-app',
  templateUrl: 'app.component.html'
})
export class AppComponent {
  public constructor(private viewContainerRef: ViewContainerRef) {
  }
}
