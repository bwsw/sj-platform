import { Component, OnInit } from '@angular/core';
import { ModalDirective } from 'ng2-bootstrap';

import { ServiceModel } from '../shared/models/service.model';
import { ProviderModel } from '../shared/models/provider.model';
import { ServicesService } from '../shared/services/services.service';
import { ProvidersService } from '../shared/services/providers.service';

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
    this.servicesService.getServiceList()
      .subscribe(
        serviceList => {
          this.serviceList = serviceList;
          if (serviceList.length > 0) {
            this.currentService = serviceList[0];
          }
        },
        error => this.errorMessage = <any>error);
  }

  public getServiceTypes() {
    this.servicesService.getServiceTypes()
      .subscribe(
        types => this.serviceTypes = types,
        error => this.showAlert({ msg: error, type: 'danger', closable: true, timeout: 0 })
      );
  }

  public getProviderList() {
    this.providersService.getProviderList()
      .subscribe(
        providerList => this.providerList = providerList,
        error => this.errorMessage = <any>error);
  }

  public getProvider(providerName: string) {
    this.providersService.getProvider(providerName)
      .subscribe(
        provider => this.currentServiceProvider = provider,
        error => this.errorMessage = <any>error);
  }

  public getProviderInfo(modal: ModalDirective, providerName: string) {
    this.getProvider(providerName);
    modal.show();
  }

  public deleteServiceConfirm(modal: ModalDirective, service: ServiceModel) {
    this.currentService = service;
    this.blockingStreams = [];
    this.servicesService.getRelatedStreamsList(service.name)
      .subscribe(response => {
        this.blockingStreams = Object.assign({},response)['streams'];
        this.blockingInstances = Object.assign({},response)['instances'];
      });
    modal.show();
  }

  public closeAlert(i: number): void {
    this.alerts.splice(i, 1);
  }

  public showAlert(message: Object): void {
    this.alerts = [];
    this.alerts.push(message);
  }

  public deleteService(modal: ModalDirective) {
    this.servicesService.deleteService(this.currentService)
      .subscribe(
        status => {
          this.showAlert({ msg: status, type: 'success', closable: true, timeout: 3000 });
          this.getServiceList();
        },
        error => this.showAlert({ msg: error, type: 'danger', closable: true, timeout: 0 }));
    modal.hide();
  }

  public createService(modal: ModalDirective) {
    this.showSpinner = true;
    this.servicesService.saveService(this.newService)
      .subscribe(
        service => {
          modal.hide();
          this.showAlert({ msg: service, type: 'success', closable: true, timeout: 3000 });
          this.newService = new ServiceModel();
          this.showSpinner = false;
          this.getServiceList();
          this.currentService = service;
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
