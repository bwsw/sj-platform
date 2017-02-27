import { Component, OnInit, Input, Output, EventEmitter, ViewChild } from '@angular/core';
import { NgForm } from '@angular/forms';
import { ModalDirective } from 'ng2-bootstrap';

import { ProviderModel, NotificationModel } from '../shared/models/index';
import { ProvidersService } from '../shared/services/index';

@Component({
  moduleId: module.id,
  selector: 'sj-providers',
  templateUrl: 'providers.component.html'
})
export class ProvidersComponent implements OnInit {
  @Input() public provider: ProviderModel;
  @Output() public close = new EventEmitter();
  public alerts: NotificationModel[] = [];
  public providerList: ProviderModel[];
  public providerTypes: string[];
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
              message: 'ProviderModel "' + provider.name + '" available',
              type: 'success',
              closable: true,
              timeout: 3000
            });
          } else {
            this.showAlert({
              message: 'ProviderModel "' + provider.name + '" not available',
              type: 'danger',
              closable: true,
              timeout: 0
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
      .subscribe(response => this.blockingServices = Object.assign({},response)['services']);
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

        },
        error => {
          modal.hide();
          this.showSpinner = false;
          this.showAlert({ message: error, type: 'danger', closable: true, timeout: 0 });
        });
  }

  public isConnecting(provider: ProviderModel) {
    return (this.currentConnectors.indexOf(provider.name) >= 0);
  }

  public showAlert(notification: NotificationModel): void {
    if (!this.alerts.find(msg => msg.message === notification.message)) {
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

