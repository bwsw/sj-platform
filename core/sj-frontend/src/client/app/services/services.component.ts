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
  templateUrl: 'services.component.html',
  styleUrls: ['services.component.css']
})
export class ServicesComponent implements OnInit {
  public errorMessage: string;
  public alerts: Array<Object> = [];
  public serviceList: ServiceModel[];
  public providerList: ProviderModel[];
  public streamList: StreamModel[];
  public blockingStreams: StreamModel[] = [];
  public current_service: ServiceModel;
  public current_service_provider: ProviderModel;
  public service_to_delete: ServiceModel;
  public new_service: ServiceModel;

  constructor(private _servicesService: ServicesService,
              private _providersService: ProvidersService,
              private _streamsService: StreamsService) {
  }

  public ngOnInit() {
    this.getServiceList();
    this.getProviderList();
    this.getStreamList();
    this.new_service = new ServiceModel();
  }

  public getServiceList() {
    this._servicesService.getServiceList()
      .subscribe(
        serviceList => {
          this.serviceList = serviceList;
          if (serviceList.length > 0) {
            this.current_service = serviceList[0];
          }
        },
        error => this.errorMessage = <any>error);
  }

  public getProviderList() {
    this._providersService.getProviderList()
      .subscribe(
        providerList => this.providerList = providerList,
        error => this.errorMessage = <any>error);
  }

  public getStreamList() {
    this._streamsService.getStreamList()
      .subscribe(
        streamList => {
          this.streamList = streamList;
        },
        error => this.errorMessage = <any>error);
  }

  public getProvider(providerName: string) {
    this._providersService.getProvider(providerName)
      .subscribe(
        provider => this.current_service_provider = provider,
        error => this.errorMessage = <any>error);
  }

  public get_provider_info(Modal: ModalDirective, providerName: string) {
    this.getProvider(providerName);
    Modal.show();
  }

  public delete_service_confirm(modal: ModalDirective, service: ServiceModel) {
    this.service_to_delete = service;
    this.blockingStreams = [];
    this.streamList.forEach((item: StreamModel) => {
      if (typeof item.service !== 'undefined') {
        if (item.service === this.service_to_delete.name) {
          this.blockingStreams.push(item);
        }
      } else if (typeof item.generator !== 'undefined') { //TODO
        if (item.generator.service === this.service_to_delete.name) {
          this.blockingStreams.push(item);
        }
      }
    });
    modal.show();
  }

  public closeAlert(i: number): void {
    this.alerts.splice(i, 1);
  }

  public delete_service(modal: ModalDirective, service: ServiceModel) {
    this._servicesService.deleteService(service)
      .subscribe(
        status => {
          this.alerts.push({ msg: status, type: 'success', closable: true, timeout: 3000 });
          this.getServiceList();
        },
        error => this.alerts.push({ msg: error, type: 'danger', closable: true, timeout: 0 }));
    this.service_to_delete = null;
    modal.hide();
  }

  public createService(modal: ModalDirective) {
    this._servicesService.saveService(this.new_service)
      .subscribe(
        service => {
          modal.hide();
          this.new_service = new ServiceModel();
          this.getServiceList();
          this.current_service = service;
        },
        error => {
          this.alerts.push({ msg: error, type: 'danger', closable: true, timeout: 0 });
          modal.hide();
        });
  }

  public service_select(service: ServiceModel) {
    this.current_service = service;
  }

  public isSelected(service: ServiceModel) {
    return service === this.current_service;
  }
}
