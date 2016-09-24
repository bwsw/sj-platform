import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { FormGroup, FormBuilder } from '@angular/forms';
import { ModalDirective } from 'ng2-bootstrap';

import { ProviderModel } from '../shared/models/provider.model';
import { ServiceModel } from '../shared/models/service.model';
import { ProvidersService } from '../shared/services/providers.service';
import { ServicesService } from '../shared/services/services.service';

@Component({
  moduleId: module.id,
  selector: 'sj-providers',
  templateUrl: 'providers.component.html',
  styleUrls: ['providers.component.css']
})
export class ProvidersComponent implements OnInit {
  @Input() public provider: ProviderModel;
  @Output() public close = new EventEmitter();
  public alerts: Array<Object> = [];
  public providerList: ProviderModel[];
  public serviceList: ServiceModel[];
  public blockingServices: ServiceModel[] = [];
  public current_provider: ProviderModel;
  public provider_to_delete: ProviderModel;
  public new_provider: ProviderModel;
  public errorMessage: string;
  public current_connectors: [String] = [''];
  public provider_hosts = ['']; //TODO Refactor

  public form: FormGroup;

  constructor(private _providersService: ProvidersService,
              private _servicesService: ServicesService,
              private _fb: FormBuilder) {
  }


  public ngOnInit() {
    this.getProviderList();
    this.getServiceList();
    this.new_provider = new ProviderModel();
    this.form = this._fb.group({
      //firstName: ['', Validators.required],
      //lastName: ['', Validators.required],
      //email: ['', Validators.compose([Validators.required])],
      //phone: ['', Validators.required],
    });
  }

  public getProviderList() {
    this._providersService.getProviderList()
      .subscribe(
        providerList => {
          this.providerList = providerList;
          if (providerList.length > 0) {
            this.current_provider = providerList[0];
          }
        },
        error => this.alerts.push({ msg: error, type: 'danger', closable: true, timeout: 0 }));
  }

  public getServiceList() {
    this._servicesService.getServiceList()
      .subscribe(
        serviceList => {
          this.serviceList = serviceList;
        },
        error => this.alerts.push({ msg: error, type: 'danger', closable: true, timeout: 0 }));
  }

  public testConnection(provider: ProviderModel) {
    this.current_connectors.push(provider.name);
    this._providersService.testConnection(provider)
      .subscribe(
        status => {
          if (status === true) {
            this.alerts.push({
              msg: 'ProviderModel "' + provider.name + '" available',
              type: 'success',
              closable: true
            });
          } else {
            this.alerts.push({
              msg: 'ProviderModel "' + provider.name + '" not available',
              type: 'danger',
              closable: true
            });
          }
          this.current_connectors.splice(this.current_connectors.indexOf(provider.name));
        },
        error => {
          this.alerts.push({ msg: error, type: 'danger', closable: true, timeout: 0 });
          this.current_connectors.splice(this.current_connectors.indexOf(provider.name));
        });
  }

  public provider_select(provider: ProviderModel) {
    this.current_provider = provider;
  }

  public delete_provider_confirm(modal: ModalDirective, provider: ProviderModel) {
    this.provider_to_delete = provider;
    this.blockingServices = [];
    this.serviceList.forEach((item: ServiceModel) => {
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
    });
    modal.show();
  }

  public delete_provider(modal: ModalDirective, provider: ProviderModel) {
    this._providersService.deleteProvider(provider)
      .subscribe(
        status => {
          this.alerts.push({ msg: status, type: 'success', closable: true, timeout: 3000 });
          this.getProviderList();
        },
        error => this.alerts.push({ msg: error, type: 'danger', closable: true, timeout: 0 }));
    this.provider_to_delete = null;
    modal.hide();
  }

  public createProvider(modal: ModalDirective) {
    this._providersService.saveProvider(this.new_provider)
      .subscribe(
        message => {
          modal.hide();
          this.alerts.push({ msg: message, type: 'success', closable: true, timeout: 3000 });
          this.getProviderList();
          this.current_provider = this.new_provider;
          this.new_provider = new ProviderModel;

        },
        error => {
          modal.hide();
          this.alerts.push({ msg: error, type: 'danger', closable: true, timeout: 0 });
        });
  }

  public isSelected(provider: ProviderModel) {
    return provider === this.current_provider;
  }

  public isConnecting(provider: ProviderModel) {
    return (this.current_connectors.indexOf(provider.name) >= 0);
  }

  public closeAlert(i: number): void {
    this.alerts.splice(i, 1);
  }

  public delete_host(i: number): void {
    this.new_provider.hosts.splice(i, 1);
  }

  public addHost() {
    this.new_provider.hosts.push('');
  }

  /* @hack: for nested ngFor and ngModel */
  public customTrackBy(index: number, obj: any): any {
    return index;
  }
}

