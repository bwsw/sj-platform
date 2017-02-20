import { Component, Input } from '@angular/core';

@Component({
  moduleId: module.id,
  selector: 'sj-alerts',
  templateUrl: 'alerts.component.html'
})
export class AlertsComponent {
  @Input() alerts: string[];

  public closeAlert(i: number): void {
    this.alerts.splice(i, 1);
  }

}
