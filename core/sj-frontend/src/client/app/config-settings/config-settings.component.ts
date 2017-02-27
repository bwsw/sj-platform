import { Component, OnInit } from '@angular/core';
import { ModalDirective } from 'ng2-bootstrap';

import { SettingModel, NotificationModel } from '../shared/models/index';
import { ConfigSettingsService } from '../shared/services/index';

@Component({
  moduleId: module.id,
  selector: 'sj-config-setting',
  templateUrl: 'config-settings.component.html'
})
export class ConfigSettingsComponent implements OnInit {
  public settingsList: SettingModel[];
  public settingsDomains: string[];
  public alerts: NotificationModel[] = [];
  public newSetting: SettingModel;
  public currentSetting: SettingModel;
  public showSpinner: boolean;

  constructor(private configSettingsService: ConfigSettingsService) { }

  public ngOnInit() {
    this.newSetting = new SettingModel();
    this.getSettingsList();
    this.getSettingsDomains();
  }

  public getSettingsList() {
    this.configSettingsService.getList()
      .subscribe(
        response => {
          this.settingsList = response.configSettings;
          if (this.settingsList.length > 0) {
            this.currentSetting = this.settingsList[0];
          }
        },
        error => this.showAlert({ message: error, type: 'danger', closable: true, timeout: 0 }));
  }

  public getSettingsDomains() {
    this.configSettingsService.getConfigSettingsDomains()
      .subscribe(domains => this.settingsDomains = domains);
  }

  public createSetting(modal: ModalDirective) {
    this.showSpinner = true;
    this.configSettingsService.save(this.newSetting)
      .subscribe(
        setting => {
          modal.hide();
          this.newSetting = new SettingModel();
          this.getSettingsList();
          this.showSpinner = false;
          this.showAlert({ message: setting.message, type: 'success', closable: true, timeout: 3000 });
        },
        error => {
          modal.hide();
          this.showSpinner = false;
          this.newSetting = new SettingModel();
          this.showAlert({ message: error, type: 'danger', closable: true, timeout: 0 });
        });
  }

  public deleteSetting(modal: ModalDirective) {
    this.configSettingsService.deleteSetting(this.currentSetting)
      .subscribe(
        response => {
          this.showAlert({ message: response.message, type: 'success', closable: true, timeout: 3000 });
          this.getSettingsList();
        },
        error => this.showAlert({ message: error, type: 'danger', closable: true, timeout: 0 }));
    modal.hide();
  }

  public deleteSettingConfirm(modal: ModalDirective, setting: SettingModel) {
    this.currentSetting = setting;
    modal.show();
  }

  public showAlert(notification: NotificationModel): void {
    if (!this.alerts.find(msg => msg.message === notification.message)) {
      this.alerts.push(notification);
    }
  }

  public selectSetting(setting: SettingModel) {
    this.currentSetting = setting;
  }

  public isSelected(setting: SettingModel) {
    return setting === this.currentSetting;
  }
}
