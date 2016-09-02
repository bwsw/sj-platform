import {
    CORE_DIRECTIVES,
    FormBuilder,
    ControlGroup,
} from '@angular/common';
import {FORM_DIRECTIVES, REACTIVE_FORM_DIRECTIVES} from '@angular/forms';
import { Component, EventEmitter, Injectable, Input, OnInit, Output} from '@angular/core';
import { MODAL_DIRECTIVES, BS_VIEW_PROVIDERS, AlertComponent } from 'ng2-bootstrap/ng2-bootstrap';
//import { DialogRef } from 'angular2-modal';
//import { Modal, BS_MODAL_PROVIDERS as MODAL_P } from 'angular2-modal/plugins/bootstrap';
//import {Validators, NgClass, Control, ControlGroup, AbstractControl, FormBuilder} from 'angular2/common';
//import {Injectable} from 'angular2/core';
//import {Observable}     from 'rxjs/Observable';
import 'rxjs/Rx';

//import {CORE_DIRECTIVES, FORM_DIRECTIVES} from 'angular2/common';
//import {NameListService} from '../../shared/index';
//import { MODAL_DIRECTIVES } from 'ng2-bs3-modal/ng2-bs3-modal';
//import {Headers} from 'angular2/http';

import { SearchBoxComponent } from '../shared/searchBox/search-box';
import { ListFilter } from '../shared/listFilter/list-filter';
import { OrderBy } from '../shared/orderBy/orderBy-pipe';
import { ProviderService } from './provider.service';
import { Provider } from './provider';
import { Service } from '../services/service';
import { ServiceService } from '../services/service.service';
import {ModalDirective} from 'ng2-bootstrap/ng2-bootstrap';

@Component({
  moduleId: module.id,
  selector: 'sj-providers',
  templateUrl: 'providers.component.html',
  styleUrls: ['providers.component.css'],
  directives: [FORM_DIRECTIVES, CORE_DIRECTIVES, SearchBoxComponent, MODAL_DIRECTIVES, REACTIVE_FORM_DIRECTIVES, AlertComponent],
  pipes: [ListFilter, OrderBy],
  viewProviders: [BS_VIEW_PROVIDERS],
  providers: [ProviderService, ServiceService]
})

@Injectable()
export class ProvidersComponent implements OnInit {
  @Input() provider: Provider;
  @Output() close = new EventEmitter();
  public alerts:Array<Object> = [];
  providerList: Provider[];
  serviceList: Service[];
  blockingServices: Service[] = [];
  current_provider: Provider;
  provider_to_delete: Provider;
  new_provider: Provider;
  errorMessage: string;
  form: ControlGroup;
  current_connectors:[String] = [''];
  provider_hosts = ['']; //TODO Refactor
  constructor(
      private providerService: ProviderService,
      private serviceService: ServiceService,
      private fb: FormBuilder
  ) {}


  ngOnInit() {
    this.getProviderList();
    this.getServiceList();
    this.new_provider = new Provider();
    this.form = this.fb.group({
      //firstName: ['', Validators.required],
      //lastName: ['', Validators.required],
      //email: ['', Validators.compose([Validators.required])],
      //phone: ['', Validators.required],
    });
  }

  getProviderList() {
    this.providerService.getProviderList()
      .subscribe(
        providerList => {
          this.providerList = providerList;
          if (providerList.length > 0) {
            this.current_provider = providerList[0];
          }
        },
        error => this.alerts.push({msg: error, type: 'danger', closable: true, timeout:0}));
  }
  getServiceList() {
    this.serviceService.getServiceList()
      .subscribe(
        serviceList => {
          this.serviceList = serviceList;
        },
        error => this.alerts.push({msg: error, type: 'danger', closable: true, timeout:0}));
  }
  testConnection(provider: Provider) {
    this.current_connectors.push(provider.name);
    this.providerService.testConnection(provider)
        .subscribe(
            status => {
              if (status === true) {
                this.alerts.push({msg: 'Provider "' + provider.name + '" available', type: 'success', closable: true});
              } else {
                this.alerts.push({msg: 'Provider "' + provider.name + '" not available', type: 'danger', closable: true});
              }
              this.current_connectors.splice(this.current_connectors.indexOf(provider.name));
            },
            error => {this.alerts.push({msg: error, type: 'danger', closable: true, timeout:0});
              this.current_connectors.splice(this.current_connectors.indexOf(provider.name));
            } );
  }
  provider_select(provider: Provider) {
    this.current_provider = provider;
  }
  delete_provider_confirm(modal:ModalDirective, provider: Provider) {
    this.provider_to_delete = provider;
    this.blockingServices = [];
    this.serviceList.forEach(function(item:Service, i:number) {
      if (typeof item.provider !== 'undefined') {
        if (item.provider === this.provider_to_delete.name) {
          this.blockingServices.push(item);
        }
      } else if (typeof item['metadata-provider'] !== 'undefined') {
        if (item['metadata-provider'] === this.provider_to_delete.name) {
          this.blockingServices.push(item);
        }
      } else if (typeof item['data-provider'] !== 'undefined') {
        if (item['data-provider'] === this.provider_to_delete.name) {
          this.blockingServices.push(item);
        }
      } else if (typeof item['lock-provider'] !== 'undefined') {
        if (item['lock-provider'] === this.provider_to_delete.name) {
          this.blockingServices.push(item);
        }
      }
    }.bind(this));
    modal.show();
  }
  delete_provider(modal:ModalDirective, provider: Provider) {
    this.providerService.deleteProvider(provider)
        .subscribe(
        status => { this.alerts.push({msg: status, type: 'success', closable: true, timeout:3000});
          this.getProviderList();
        },
        error => this.alerts.push({msg: error, type: 'danger', closable: true, timeout:0}));
    this.provider_to_delete = null;
    modal.hide();
  }
  createProvider(modal:ModalDirective) {
    this.providerService.saveProvider(this.new_provider)
        .subscribe(
            message => {
              modal.hide();
              this.alerts.push({msg: message, type: 'success', closable: true, timeout:3000});
              this.getProviderList();
              this.current_provider = this.new_provider;
              this.new_provider = new Provider;

            },
            error => {
                modal.hide();
                this.alerts.push({msg: error, type: 'danger', closable: true, timeout:0});
            });
  }

  isSelected(provider:Provider) {
    return provider === this.current_provider;
  }
  isConnecting(provider:Provider) {
    return (this.current_connectors.indexOf(provider.name) >= 0);
  }
  closeAlert(i:number):void {
    this.alerts.splice(i, 1);
  }
  delete_host(i:number):void {
    this.new_provider.hosts.splice(i, 1);
  }
  addHost() {
    this.new_provider.hosts.push('');
  }
}

