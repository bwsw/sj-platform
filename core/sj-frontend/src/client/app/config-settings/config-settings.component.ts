import { Component, OnInit } from '@angular/core';
import { ModalDirective } from 'ng2-bootstrap';

import { SettingModel } from '../shared/models/setting.model';
import { ConfigSettingsService } from '../shared/services/config-settings.service';

@Component({
  moduleId: module.id,
  selector: 'sj-config-setting',
  templateUrl: 'config-settings.component.html'
})
export class ConfigSettingsComponent implements OnInit {
  public settingsList: SettingModel[];
  public settingsDomains: string[];
  public alerts: Array<Object> = [];
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
        error => this.showAlert({ msg: error, type: 'danger', closable: true, timeout: 0 }));
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
          this.showAlert({ msg: setting, type: 'success', closable: true, timeout: 3000 });
        },
        error => {
          modal.hide();
          this.showSpinner = false;
          this.newSetting = new SettingModel();
          this.showAlert({ msg: error, type: 'danger', closable: true, timeout: 0 });
        });
  }

  public deleteSetting(modal: ModalDirective) {
    this.configSettingsService.deleteSetting(this.currentSetting)
      .subscribe(
        status => {
          this.showAlert({ msg: status, type: 'success', closable: true, timeout: 3000 });
          this.getSettingsList();
        },
        error => this.showAlert({ msg: error, type: 'danger', closable: true, timeout: 0 }));
    modal.hide();
  }

  public deleteSettingConfirm(modal: ModalDirective, setting: SettingModel) {
    this.currentSetting = setting;
    modal.show();
  }

  public closeAlert(i: number): void {
    this.alerts.splice(i, 1);
  }

  public showAlert(message: Object): void {
    this.alerts = [];
    this.alerts.push(message);
  }

  public selectSetting(setting: SettingModel) {
    this.currentSetting = setting;
  }

  public isSelected(setting: SettingModel) {
    return setting === this.currentSetting;
  }
}
