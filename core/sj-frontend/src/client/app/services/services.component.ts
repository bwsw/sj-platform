import { Component, OnInit } from '@angular/core';
import { ModalDirective } from 'ng2-bootstrap';

import { ServiceModel } from '../shared/models/service.model';
import { StreamModel } from '../shared/models/stream.model';
import { ProviderModel } from '../shared/models/provider.model';
import { ServicesService } from '../shared/services/services.service';
import { StreamsService } from '../shared/services/streams.service';
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
  public providerList: ProviderModel[];
  public streamList: StreamModel[];
  public blockingStreams: StreamModel[] = [];
  public currentService: ServiceModel;
  public currentServiceProvider: ProviderModel;
  public newService: ServiceModel;

  constructor(private servicesService: ServicesService,
              private providersService: ProvidersService,
              private streamsService: StreamsService) {
  }

  public ngOnInit() {
    this.getServiceList();
    this.getProviderList();
    this.getStreamList();
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

  public getProviderList() {
    this.providersService.getProviderList()
      .subscribe(
        providerList => this.providerList = providerList,
        error => this.errorMessage = <any>error);
  }

  public getStreamList() {
    this.streamsService.getStreamList()
      .subscribe(
        streamList => {
          this.streamList = streamList;
        },
        error => this.errorMessage = <any>error);
  }

  public getProvider(providerName: string) {
    this.providersService.getProvider(providerName)
      .subscribe(
        provider => this.currentServiceProvider = provider,
        error => this.errorMessage = <any>error);
  }

  public getProviderInfo(Modal: ModalDirective, providerName: string) {
    this.getProvider(providerName);
    Modal.show();
  }

  public deleteServiceConfirm(modal: ModalDirective, service: ServiceModel) {
    this.currentService = service;
    this.blockingStreams = [];
    this.streamList.forEach((item: StreamModel) => {
      if (typeof item.service !== 'undefined') {
        if (item.service === this.currentService.name) {
          this.blockingStreams.push(item);
        }
      } else if (typeof item.generator !== 'undefined') {
        if (item.generator.service === this.currentService.name) {
          this.blockingStreams.push(item);
        }
      }
    });
    modal.show();
  }

  public closeAlert(i: number): void {
    this.alerts.splice(i, 1);
  }

  public deleteService(modal: ModalDirective) {
    this.servicesService.deleteService(this.currentService)
      .subscribe(
        status => {
          this.alerts.push({ msg: status, type: 'success', closable: true, timeout: 3000 });
          this.getServiceList();
        },
        error => this.alerts.push({ msg: error, type: 'danger', closable: true, timeout: 0 }));
    modal.hide();
  }

  public createService(modal: ModalDirective) {
    this.servicesService.saveService(this.newService)
      .subscribe(
        service => {
          modal.hide();
          this.alerts.push({ msg: service, type: 'success', closable: true, timeout: 3000 });
          this.newService = new ServiceModel();
          this.getServiceList();
          this.currentService = service;
        },
        error => {
          this.alerts.push({ msg: error, type: 'danger', closable: true, timeout: 0 });
          modal.hide();
        });
  }

  public selectService(service: ServiceModel) {
    this.currentService = service;
  }

  public isSelected(service: ServiceModel) {
    return service === this.currentService;
  }
}
