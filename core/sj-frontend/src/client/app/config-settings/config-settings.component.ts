import { Component, OnInit } from '@angular/core';
import { ModalDirective } from 'ng2-bootstrap';

import { SettingModel } from '../shared/models/setting.model';
import { ConfigSettingsService } from "../shared/services/config-settings.service";

@Component({
  moduleId: module.id,
  selector: 'sj-config-setting',
  templateUrl: 'config-settings.component.html'
})
export class ConfigSettingsComponent implements OnInit {
  public settingsList: SettingModel[];
  public alerts: Array<Object> = [];
  public newSetting: SettingModel;
  public currentSetting: SettingModel;

  constructor(private configSettingsService: ConfigSettingsService) { }

  public ngOnInit() {
    this.newSetting = new SettingModel();
    this.getSettingsList();
  }

  public getSettingsList() {
    this.configSettingsService.getConfigSettingsList()
      .subscribe(
        settingsList => {
          this.settingsList = settingsList;
          if (settingsList.length > 0) {
            this.currentSetting = settingsList[0];
          }
        },
        error => this.alerts.push({ msg: error, type: 'danger', closable: true, timeout: 0 }));
  }

  public createSetting(modal: ModalDirective) {
    this.configSettingsService.saveSetting(this.newSetting)
      .subscribe(
        setting => {
          modal.hide()
          this.newSetting = new SettingModel();
          this.getSettingsList();
          this.alerts.push({ msg: setting, type: 'success', closable: true, timeout: 3000 });
        },
        error => {
          modal.hide();
          this.newSetting = new SettingModel();
          this.alerts.push({ msg: error, type: 'danger', closable: true, timeout: 0 });
        });
  }

  public deleteSetting(modal: ModalDirective) {
    this.configSettingsService.deleteSetting(this.currentSetting)
      .subscribe(
        status => {
          this.alerts.push({ msg: status, type: 'success', closable: true, timeout: 3000 });
          this.getSettingsList();
        },
        error => this.alerts.push({ msg: error, type: 'danger', closable: true, timeout: 0 }));
    modal.hide();
  }

  public deleteSettingConfirm(modal: ModalDirective, setting: SettingModel) {
    this.currentSetting = setting;
    modal.show();
  }

  public closeAlert(i: number): void {
    this.alerts.splice(i, 1);
  }

  public selectSetting(setting: SettingModel) {
    this.currentSetting = setting;
  }

  public isSelected(setting: SettingModel) {
    return setting === this.currentSetting;
  }
}
