import { Component, EventEmitter, Input, Output } from '@angular/core';
import { NotificationModel } from '../../model/notification.model';

@Component({
  selector: 'sj-alerts',
  templateUrl: 'alerts.component.html'
})
export class AlertsComponent {
  @Input() alerts: NotificationModel[];
  @Output() alertsChange = new EventEmitter<NotificationModel[]>();

  public closeAlert(i: number): void {
    this.alerts.splice(i,1);
    this.alertsChange.emit(this.alerts);
  }

  public getTimeOut(alert: NotificationModel) {
    return alert.timeout !== 0 ? alert.timeout : '';
  }

}
