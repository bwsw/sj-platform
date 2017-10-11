import { Component, OnInit, Input, Output, EventEmitter, ViewChild } from '@angular/core';
import { NgForm } from '@angular/forms';
import { ModalDirective } from 'ngx-bootstrap';

import { ProviderModel } from './provider.model';
import { NotificationModel } from '../shared/model/notification.model';
import { ProvidersService } from './providers.service';
import { TypeModel } from '../shared/model/type.model';

@Component({
  selector: 'sj-providers',
  templateUrl: 'providers.component.html',
  providers: [
    ProvidersService
  ]
})
export class ProvidersComponent implements OnInit {
  @Input() public provider: ProviderModel;
  @Output() public close = new EventEmitter();
  public alerts: NotificationModel[] = [];
  public formAlerts: NotificationModel[] = [];
  public providerList: ProviderModel[];
  public providerTypes: TypeModel[];
  public blockingServices: string[] = [];
  public currentProvider: ProviderModel;
  public newProvider: ProviderModel;
  public currentConnectors: [String] = [''];
  public showSpinner: boolean = false;

  @ViewChild('providerForm') currentForm: NgForm;

  constructor(private providersService: ProvidersService) { }

  public ngOnInit() {
    this.getProviderList();
    this.getProviderTypes();
    this.newProvider = new ProviderModel();
  }

  public getProviderList() {
    this.providersService.getList()
      .subscribe(
        response => {
          this.providerList = response.providers;
          if (this.providerList.length > 0) {
            this.currentProvider = this.providerList[0];
          }
        },
        error => this.showAlert({ message: error, type: 'danger', closable: true, timeout: 0 }));
  }

  public getProviderTypes() {
    this.providersService.getTypes()
      .subscribe(
        response => this.providerTypes = response.types,
        error => this.showAlert({ message: error, type: 'danger', closable: true, timeout: 0 })
      );
  }

  public testConnection(provider: ProviderModel) {
    this.currentConnectors.push(provider.name);
    this.providersService.testConnection(provider)
      .subscribe(
        status => {
          if (status === true) {
            this.showAlert({
              message: 'Provider \'' + provider.name + '\' is available',
              type: 'success',
              closable: true,
              timeout: 3000
            });
          } else {
            this.showAlert({
              message: 'Provider \'' + provider.name + '\' is not available',
              type: 'danger',
              closable: true,
              timeout: 3000
            });
          }
          this.currentConnectors.splice(this.currentConnectors.indexOf(provider.name));
        },
        error => {
          this.showAlert({ message: error, type: 'danger', closable: true, timeout: 0 });
          this.currentConnectors.splice(this.currentConnectors.indexOf(provider.name));
        });
  }

  public selectProvider(provider: ProviderModel) {
    this.currentProvider = provider;
  }

  public deleteProviderConfirm(modal: ModalDirective, provider: ProviderModel) {
    this.currentProvider = provider;
    this.blockingServices = [];
    this.providersService.getRelatedList(this.currentProvider.name)
      .subscribe(response => this.blockingServices = Object.assign({}, response)['services']);
    modal.show();
  }

  public deleteProvider(modal: ModalDirective) {
    this.providersService.remove(this.currentProvider.name)
      .subscribe(
        response => {
          this.showAlert({ message: response.message, type: 'success', closable: true, timeout: 3000 });
          this.getProviderList();
        },
        error => this.showAlert({ message: error, type: 'danger', closable: true, timeout: 0 }));
    modal.hide();
  }

  public createProvider(modal: ModalDirective) {
    this.showSpinner = true;
    this.providersService.save(this.newProvider)
      .subscribe(
        response => {
          modal.hide();
          this.showSpinner = false;
          this.showAlert({ message: response.message, type: 'success', closable: true, timeout: 3000 });
          this.getProviderList();
          this.newProvider = new ProviderModel;
          this.formAlerts = [];
          this.currentForm.reset();
        },
        error => {
          this.showSpinner = false;
          this.formAlerts.push({ message: error, type: 'danger', closable: true, timeout: 0 });
        });
  }

  public closeModal(modal: ModalDirective) {
    this.newProvider = new ProviderModel();
    modal.hide();
    this.formAlerts = [];
    this.currentForm.reset();
  }

  public showAlert(notification: NotificationModel): void {
    if (!this.alerts.find(msg => msg.message === notification.message) || notification.timeout !== 0) {
      this.alerts.push(notification);
    }
  }

  public deleteHost(i: number): void {
    this.newProvider.hosts.splice(i, 1);
  }

  public addHost() {
    this.newProvider.hosts.push('');
  }

  /* @hack: for nested ngFor and ngModel */
  public customTrackBy(index: number, obj: any): any {
    return index;
  }
}

