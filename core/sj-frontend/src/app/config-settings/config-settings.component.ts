import { Component, OnInit, ViewChild } from '@angular/core';
import { ModalDirective } from 'ngx-bootstrap';
import { ConfigSettingModel } from './config-setting.model';
import { NotificationModel } from '../shared/model/notification.model';
import { ConfigSettingsService } from './config-settings.service';
import { NgForm } from '@angular/forms';
import { TypeModel } from '../shared/model/type.model';


@Component({
  selector: 'sj-config-setting',
  templateUrl: 'config-settings.component.html',
  providers: [
    ConfigSettingsService
  ]
})
export class ConfigSettingsComponent implements OnInit {
  public settingsList: ConfigSettingModel[];
  public settingsDomains: TypeModel[];
  public alerts: NotificationModel[] = [];
  public formAlerts: NotificationModel[] = [];
  public newSetting: ConfigSettingModel;
  public currentSetting: ConfigSettingModel;
  public showSpinner: boolean;

  @ViewChild('settingForm') currentForm: NgForm;

  constructor(private configSettingsService: ConfigSettingsService) { }

  public ngOnInit() {
    this.newSetting = new ConfigSettingModel();
    this.getSettingsList();
    this.getSettingsDomains();
  }

  public getSettingsList() {
    this.configSettingsService.getList()
      .subscribe(
        response => {
          this.settingsList = response.configSettings;
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
          this.newSetting = new ConfigSettingModel();
          this.getSettingsList();
          this.showSpinner = false;
          this.showAlert({ message: setting.message, type: 'success', closable: true, timeout: 3000 });
          this.currentForm.reset();
        },
        error => {
          this.showSpinner = false;
          this.newSetting = new ConfigSettingModel();
          this.formAlerts.push({ message: error, type: 'danger', closable: true, timeout: 0 });
        });
  }

  public closeModal(modal: ModalDirective) {
    this.newSetting = new ConfigSettingModel();
    modal.hide();
    this.formAlerts = [];
    this.currentForm.reset();
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

  public deleteSettingConfirm(modal: ModalDirective, setting: ConfigSettingModel) {
    this.currentSetting = setting;
    modal.show();
  }

  public showAlert(notification: NotificationModel): void {
    if (!this.alerts.find(msg => msg.message === notification.message)) {
      this.alerts.push(notification);
    }
  }

  public selectSetting(setting: ConfigSettingModel) {
    this.currentSetting = setting;
  }
}
