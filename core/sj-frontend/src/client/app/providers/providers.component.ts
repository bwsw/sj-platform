import { Component, OnInit, Input, Output, EventEmitter, ViewChild, AfterViewChecked } from '@angular/core';
import { NgForm } from '@angular/forms';
import { ModalDirective } from 'ng2-bootstrap';

import { ProviderModel } from '../shared/models/provider.model';
import { ServiceModel } from '../shared/models/service.model';
import { ProvidersService } from '../shared/services/providers.service';
import { ServicesService } from '../shared/services/services.service';

@Component({
  moduleId: module.id,
  selector: 'sj-providers',
  templateUrl: 'providers.component.html'
})
export class ProvidersComponent implements OnInit, AfterViewChecked {
  @Input() public provider: ProviderModel;
  @Output() public close = new EventEmitter();
  public alerts: Array<Object> = [];
  public providerList: ProviderModel[];
  public serviceList: ServiceModel[];
  public blockingServices: string[] = [];
  public currentProvider: ProviderModel;
  public newProvider: ProviderModel;
  public currentConnectors: [String] = [''];
  public providerForm: NgForm;
  public showSpinner: boolean = false;

  @ViewChild('providerForm') currentForm: NgForm;

  public formErrors: { [key: string]: string } = {
    'providerHosts': '',
  };

  public validationMessages: { [key: string]: { [key: string]: string } } = {
    'providerHosts': {
      'validHostPort': 'The format of one of hosts is invalid',
    },
  };

  constructor(private providersService: ProvidersService,
              private servicesService: ServicesService) {
  }


  public ngOnInit() {
    this.getProviderList();
    this.getServiceList();
    this.newProvider = new ProviderModel();
  }

  public getProviderList() {
    this.providersService.getProviderList()
      .subscribe(
        providerList => {
          this.providerList = providerList;
          if (providerList.length > 0) {
            this.currentProvider = providerList[0];
          }
        },
        error => this.showAlert({ msg: error, type: 'danger', closable: true, timeout: 0 }));
  }

  public getServiceList() {
    this.servicesService.getServiceList()
      .subscribe(
        serviceList => {
          this.serviceList = serviceList;
        },
        error => this.showAlert({ msg: error, type: 'danger', closable: true, timeout: 0 }));
  }

  public testConnection(provider: ProviderModel) {
    this.currentConnectors.push(provider.name);
    this.providersService.testConnection(provider)
      .subscribe(
        status => {
          if (status === true) {
            this.showAlert({
              msg: 'ProviderModel "' + provider.name + '" available',
              type: 'success',
              closable: true,
              timeout: 3000
            });
          } else {
            this.showAlert({
              msg: 'ProviderModel "' + provider.name + '" not available',
              type: 'danger',
              closable: true,
              timeout: 0
            });
          }
          this.currentConnectors.splice(this.currentConnectors.indexOf(provider.name));
        },
        error => {
          this.showAlert({ msg: error, type: 'danger', closable: true, timeout: 0 });
          this.currentConnectors.splice(this.currentConnectors.indexOf(provider.name));
        });
  }

  public selectProvider(provider: ProviderModel) {
    this.currentProvider = provider;
  }

  public deleteProviderConfirm(modal: ModalDirective, provider: ProviderModel) {
    this.currentProvider = provider;
    this.blockingServices = [];
    this.providersService.getRelatedServicesList(this.currentProvider.name)
      .subscribe(response => this.blockingServices = response);
    modal.show();
  }

  public deleteProvider(modal: ModalDirective) {
    this.providersService.deleteProvider(this.currentProvider)
      .subscribe(
        status => {
          this.showAlert({ msg: status, type: 'success', closable: true, timeout: 3000 });
          this.getProviderList();
        },
        error => this.showAlert({ msg: error, type: 'danger', closable: true, timeout: 0 }));
    modal.hide();
  }

  public createProvider(modal: ModalDirective) {
    this.showSpinner = true;
    this.providersService.saveProvider(this.newProvider)
      .subscribe(
        message => {
          modal.hide();
          this.showSpinner = false;
          this.showAlert({ msg: message, type: 'success', closable: true, timeout: 3000 });
          this.getProviderList();
          this.newProvider = new ProviderModel;

        },
        error => {
          modal.hide();
          this.showSpinner = false;
          this.showAlert({ msg: error, type: 'danger', closable: true, timeout: 0 });
        });
  }

  public isSelected(provider: ProviderModel) {
    return provider === this.currentProvider;
  }

  public isConnecting(provider: ProviderModel) {
    return (this.currentConnectors.indexOf(provider.name) >= 0);
  }

  public closeAlert(i: number): void {
    this.alerts.splice(i, 1);
  }

  public showAlert(message: Object): void {
    this.alerts = [];
    this.alerts.push(message);
  }

  public deleteHost(i: number): void {
    this.newProvider.hosts.splice(i, 1);
  }

  public addHost() {
    this.newProvider.hosts.push('');
  }

  public ngAfterViewChecked() {
    this.formChanged();
  }

  public formChanged() {
    if (this.currentForm === this.providerForm) { return; }
    this.providerForm = this.currentForm;
    if (this.providerForm) {
      this.providerForm.valueChanges
        .subscribe(data => this.onValueChanged(data));
    }
  }

  public onValueChanged(data?: any) {
    if (!this.providerForm) { return; }
    const form = this.providerForm.form;

    for (const field in this.formErrors) {
      // clear previous error message (if any)
      this.formErrors[field] = '';
      const control = form.get(field);
      if (control && control.dirty && !control.valid) {
        const messages = this.validationMessages[field];
        for (const key in control.errors) {
          this.formErrors[field] += messages[key] + ' ';
        }
      }
    }
  }

  /* @hack: for nested ngFor and ngModel */
  public customTrackBy(index: number, obj: any): any {
    return index;
  }
}

