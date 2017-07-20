import { Component, Input } from '@angular/core';
import { NotificationModel } from '../../model/notification.model';

@Component({
  selector: 'sj-alerts',
  templateUrl: 'alerts.component.html'
})
export class AlertsComponent {
  @Input() alerts: NotificationModel[];

  public closeAlert(i: number): void {
    this.alerts.splice(i, 1);
  }

  public getTimeOut(alert: NotificationModel) {
    return alert.timeout !== 0 ? alert.timeout : '';
  }

}
