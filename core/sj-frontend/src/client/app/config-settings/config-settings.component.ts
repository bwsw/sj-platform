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
  public new_setting: SettingModel;
  public current_setting: SettingModel;

  constructor(private _configSettingsService: ConfigSettingsService) {
  }

  public ngOnInit() {
    this.new_setting = new SettingModel();
  }

  public createSetting(modal: ModalDirective) {
    this._configSettingsService.saveSetting(this.new_setting)
      .subscribe(
        setting => {
          modal.hide();
          this.new_setting = new SettingModel();
          this.current_setting = setting;
        },
        error => {
          this.alerts.push({ msg: error, type: 'danger', closable: true, timeout: 0 });
          modal.hide();
        });
  }

  public setting_select(setting: SettingModel) {
    this.current_setting = setting;
  }

  public isSelected(setting: SettingModel) {
    return setting === this.current_setting;
  }
}
