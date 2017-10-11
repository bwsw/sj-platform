import { Component, OnInit, ViewChild } from '@angular/core';
import { ModalDirective } from 'ngx-bootstrap';

import { NotificationModel } from '../shared/model/notification.model';
import { ServiceModel } from './service.model';
import { ProviderModel } from '../providers/provider.model';
import { ServicesService } from './services.service';
import { ProvidersService } from '../providers/providers.service';
import { NgForm } from '@angular/forms';
import { TypeModel } from '../shared/model/type.model';

@Component({
  selector: 'sj-services',
  templateUrl: 'services.component.html',
  providers: [
    ProvidersService,
    ServicesService
  ]
})
export class ServicesComponent implements OnInit {
  public errorMessage: string;
  public alerts: NotificationModel[] = [];
  public formAlerts: NotificationModel[] = [];
  public serviceList: ServiceModel[];
  public serviceTypes: TypeModel[];
  public providerTypes: {};
  public providerList: ProviderModel[];
  public blockingStreams: string[] = [];
  public blockingInstances: string[] = [];
  public currentService: ServiceModel;
  public currentServiceProvider: ProviderModel;
  public newService: ServiceModel;
  public showSpinner: boolean;

  @ViewChild('serviceForm') currentForm: NgForm;

  constructor(private servicesService: ServicesService,
              private providersService: ProvidersService) { }

  public ngOnInit() {
    this.getServiceList();
    this.getProviderList();
    this.getServiceTypes();
    this.getProviderTypes();
    this.newService = new ServiceModel();
  }

  public getServiceList() {
    this.servicesService.getList()
      .subscribe(
        response => {
          this.serviceList = response.services;
          if (this.serviceList.length > 0) {
            this.currentService = this.serviceList[0];
          }
        },
        error => this.errorMessage = <any>error);
  }

  public getServiceTypes() {
    this.servicesService.getTypes()
      .subscribe(
        response => this.serviceTypes = response.types,
        error => this.showAlert({ message: error, type: 'danger', closable: true, timeout: 0 })
      );
  }

  public getProviderTypes() {
    this.providersService.getTypes()
      .subscribe(
        response => {
          this.providerTypes = response.types.reduce((memo, item: TypeModel) => {
            memo[item.id] = item.name;
            return memo;
          }, {});
        },
        error => this.showAlert({ message: error, type: 'danger', closable: true, timeout: 0 })
      );
  }

  public getProviderList() {
    this.providersService.getList()
      .subscribe(
        response => this.providerList = response.providers,
        error => this.errorMessage = <any>error);
  }

  public getProvider(providerName: string) {
    this.providersService.get(providerName)
      .subscribe(
        response => this.currentServiceProvider = response.provider,
        error => this.errorMessage = <any>error);
  }

  public getProviderInfo(modal: ModalDirective, providerName: string) {
    this.getProvider(providerName);
    modal.show();
  }

  public deleteServiceConfirm(modal: ModalDirective, service: ServiceModel) {
    this.currentService = service;
    this.blockingStreams = [];
    this.servicesService.getRelatedList(service.name)
      .subscribe(response => {
        this.blockingStreams = Object.assign({}, response)['streams'];
        this.blockingInstances = Object.assign({}, response)['instances'];
      });
    modal.show();
  }

  public showAlert(notification: NotificationModel): void {
    if (!this.alerts.find(msg => msg.message === notification.message)) {
      this.alerts.push(notification);
    }
  }

  public deleteService(modal: ModalDirective) {
    this.servicesService.remove(this.currentService.name)
      .subscribe(
        response => {
          this.showAlert({ message: response.message, type: 'success', closable: true, timeout: 3000 });
          this.getServiceList();
        },
        error => this.showAlert({ message: error, type: 'danger', closable: true, timeout: 0 }));
    modal.hide();
  }

  public createService(modal: ModalDirective) {
    this.showSpinner = true;
    this.servicesService.save(this.newService)
      .subscribe(
        response => {
          modal.hide();
          this.showAlert({ message: response.message, type: 'success', closable: true, timeout: 3000 });
          this.newService = new ServiceModel();
          this.formAlerts = [];
          this.showSpinner = false;
          this.getServiceList();
          this.currentForm.reset();
        },
        error => {
          this.formAlerts.push({ message: error, type: 'danger', closable: true, timeout: 0 });
          this.showSpinner = false;
        });
  }

  public closeModal(modal: ModalDirective) {
    this.newService = new ServiceModel();
    this.formAlerts = [];
    modal.hide();
    this.currentForm.reset();
  }

  public selectService(service: ServiceModel) {
    this.currentService = service;
  }
}
