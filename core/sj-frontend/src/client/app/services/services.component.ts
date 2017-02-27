import { Component, OnInit } from '@angular/core';
import { ModalDirective } from 'ng2-bootstrap';

import { ServiceModel, ProviderModel } from '../shared/models/index';
import { ServicesService, ProvidersService } from '../shared/services/index';

@Component({
  moduleId: module.id,
  selector: 'sj-services',
  templateUrl: 'services.component.html'
})
export class ServicesComponent implements OnInit {
  public errorMessage: string;
  public alerts: Array<Object> = [];
  public serviceList: ServiceModel[];
  public serviceTypes: string[];
  public providerList: ProviderModel[];
  public blockingStreams: string[] = [];
  public blockingInstances: string[] = [];
  public currentService: ServiceModel;
  public currentServiceProvider: ProviderModel;
  public newService: ServiceModel;
  public showSpinner: boolean;

  constructor(private servicesService: ServicesService,
              private providersService: ProvidersService) { }

  public ngOnInit() {
    this.getServiceList();
    this.getProviderList();
    this.getServiceTypes();
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
        error => this.showAlert({ msg: error, type: 'danger', closable: true, timeout: 0 })
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
        this.blockingStreams = Object.assign({},response)['streams'];
        this.blockingInstances = Object.assign({},response)['instances'];
      });
    modal.show();
  }

  public showAlert(message: Object): void {
    this.alerts = [];
    this.alerts.push(message);
  }

  public deleteService(modal: ModalDirective) {
    this.servicesService.remove(this.currentService.name)
      .subscribe(
        response => {
          this.showAlert({ msg: response.message, type: 'success', closable: true, timeout: 3000 });
          this.getServiceList();
        },
        error => this.showAlert({ msg: error, type: 'danger', closable: true, timeout: 0 }));
    modal.hide();
  }

  public createService(modal: ModalDirective) {
    this.showSpinner = true;
    this.servicesService.save(this.newService)
      .subscribe(
        response => {
          modal.hide();
          this.showAlert({ msg: response.message, type: 'success', closable: true, timeout: 3000 });
          this.newService = new ServiceModel();
          this.showSpinner = false;
          this.getServiceList();
        },
        error => {
          this.showAlert({ msg: error, type: 'danger', closable: true, timeout: 0 });
          modal.hide();
          this.showSpinner = false;
        });
  }

  public selectService(service: ServiceModel) {
    this.currentService = service;
  }

  public isSelected(service: ServiceModel) {
    return service === this.currentService;
  }
}
