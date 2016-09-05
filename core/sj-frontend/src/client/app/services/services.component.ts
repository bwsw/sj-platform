import {
    CORE_DIRECTIVES,
    FormBuilder,
    ControlGroup
} from '@angular/common';
import {FORM_DIRECTIVES, REACTIVE_FORM_DIRECTIVES} from '@angular/forms';
import { Component, OnInit} from '@angular/core';
import { MODAL_DIRECTIVES, BS_VIEW_PROVIDERS, AlertComponent } from 'ng2-bootstrap/ng2-bootstrap';
import { ListFilter } from '../shared/listFilter/list-filter';
import { OrderBy } from '../shared/orderBy/orderBy-pipe';
import { ProviderFilter } from './provider-filter';
import 'rxjs/Rx';
import { SearchBoxComponent } from '../shared/searchBox/search-box';
import { Service } from './service';
import { ServiceService } from './service.service';
import { Provider } from '../providers/provider';
import { ProviderService } from '../providers/provider.service';
import { Stream } from '../streams/stream';
import { StreamService } from '../streams/stream.service';
import {ModalDirective} from 'ng2-bootstrap/ng2-bootstrap';

@Component({
  moduleId: module.id,
  selector: 'sd-services',
  templateUrl: 'services.component.html',
  styleUrls: ['services.component.css'],
  directives: [FORM_DIRECTIVES, CORE_DIRECTIVES, MODAL_DIRECTIVES, REACTIVE_FORM_DIRECTIVES, SearchBoxComponent, AlertComponent],
  pipes: [ListFilter, ProviderFilter, OrderBy],
  viewProviders: [BS_VIEW_PROVIDERS],
  providers: [ProviderService, ServiceService, StreamService]
})
export class ServicesComponent implements OnInit {
  errorMessage:string;
  public alerts:Array<Object> = [];
  serviceList:Service[];
  providerList: Provider[];
  streamList: Stream[];
  blockingStreams: Stream[] = [];
  current_service: Service;
  current_service_provider: Provider;
  service_to_delete: Service;
  new_service: Service;
  serviceForm: ControlGroup;

  constructor(
      private serviceService:ServiceService,
      private providerService:ProviderService,
      private streamService:StreamService,
      private fb: FormBuilder
  ) {}

  ngOnInit() {
    this.getServiceList();
    this.getProviderList();
    this.getStreamList();
    this.new_service = new Service();
    this.serviceForm = this.fb.group({
      //firstName: ['', Validators.required],
      //lastName: ['', Validators.required],
      //email: ['', Validators.compose([Validators.required])],
      //phone: ['', Validators.required],
    });
  }

  getServiceList() {
    this.serviceService.getServiceList()
        .subscribe(
            serviceList => {
              this.serviceList = serviceList;
              if (serviceList.length > 0) {
                this.current_service = serviceList[0];
              }
            },
            error => this.errorMessage = <any>error);
  }
  getProviderList() {
    this.providerService.getProviderList()
        .subscribe(
            providerList => this.providerList = providerList,
            error => this.errorMessage = <any>error);
  }
  getStreamList() {
    this.streamService.getStreamList()
      .subscribe(
        streamList => { this.streamList = streamList;
        },
        error => this.errorMessage = <any>error);
  }
  getProvider(providerName:string) {
    this.providerService.getProvider(providerName)
        .subscribe(
            provider => this.current_service_provider = provider,
            error => this.errorMessage = <any>error);
  }
  get_provider_info(Modal:ModalDirective, providerName:string) {
    this.getProvider(providerName);
    Modal.show();
  }
  delete_service_confirm(modal:ModalDirective, service:Service) {
    this.service_to_delete = service;
    this.blockingStreams = [];
    this.streamList.forEach(function(item:Stream, i:number) {
      if (typeof item.service !== 'undefined') {
        if (item.service === this.service_to_delete.name) {
          this.blockingStreams.push(item);
        }
      } else if (typeof item.generator !== 'undefined') { //TODO
        if (item.generator.service === this.service_to_delete.name) {
          this.blockingStreams.push(item);
        }
      }
    }.bind(this));
    modal.show();
  }
  closeAlert(i:number):void {
    this.alerts.splice(i, 1);
  }
  delete_service(modal:ModalDirective, service: Service) {
    this.serviceService.deleteService(service)
        .subscribe(
            status => {
              this.alerts.push({msg: status, type: 'success', closable: true, timeout:3000});
              this.getServiceList();
            },
            error => this.alerts.push({msg: error, type: 'danger', closable: true, timeout:0}));
    this.service_to_delete = null;
    modal.hide();
  }
  createService(modal:ModalDirective) {
    this.serviceService.saveService(this.new_service)
        .subscribe(
            service => {
              modal.hide();
              this.new_service = new Service;
              this.getServiceList();
              this.current_service = service;
            },
            error => {
              this.alerts.push({msg: error, type: 'danger', closable: true, timeout:0});
              modal.hide();
            });
  }
  service_select(service: Service) {
    this.current_service = service;
  }

  isSelected(service : Service) {
    return service === this.current_service;
  }
}
