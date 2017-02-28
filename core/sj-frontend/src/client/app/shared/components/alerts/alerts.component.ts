import { Component, Input } from '@angular/core';
import { NotificationModel } from '../../models/index';

@Component({
  moduleId: module.id,
  selector: 'sj-alerts',
  templateUrl: 'alerts.component.html'
})
export class AlertsComponent {
  @Input() alerts: NotificationModel[];

  public closeAlert(i: number): void {
    this.alerts.splice(i, 1);
  }

}
