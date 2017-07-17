import { Component, OnInit, ViewChild, AfterViewChecked, OnDestroy } from '@angular/core';
import { NgForm } from '@angular/forms';
import { ModalDirective } from 'ngx-bootstrap';
import { NotificationModel } from '../shared/model/notification.model';
import { InstanceModel } from './instance.model';
import { ModuleModel } from '../modules/module.model';
import { ServiceModel } from '../services/service.model';
import { StreamModel } from '../streams/stream.model';
import { TaskModel } from './task.model';
import { InstancesService } from './instances.service';
import { ModulesService } from '../modules/modules.service';
import { StreamsService } from '../streams/streams.service';
import { ServicesService } from '../services/services.service';
import { AnonymousSubscription } from 'rxjs/Subscription';
import { Observable } from 'rxjs';

@Component({
  selector: 'sj-instances',
  templateUrl: 'instances.component.html',
  providers: [
    InstancesService,
    StreamsService,
    ServicesService,
    ModulesService
  ]
})
export class InstancesComponent implements OnInit, AfterViewChecked, OnDestroy {

  public alerts: NotificationModel[] = [];
  public formAlerts: NotificationModel[] = [];
  public errorMessage: string;
  public isFormReady: boolean = false;
  public instancesList: InstanceModel[];
  public cloneInstancesList: InstanceModel[] = [];
  public modulesList: ModuleModel[];
  public moduleTypes: string[];
  public servicesList: ServiceModel[] = [];
  public streamsList: StreamModel[] = [];
  public streamTypesList: { [key: string]: string } = {};
  public currentInstance: InstanceModel;
  public currentInstanceTasks: {};
  public newInstance: InstanceModel;
  public isInstanceClone: boolean = false;
  public cloningInstance: InstanceModel;
  public instanceForm: NgForm;
  public showSpinner: boolean;
  public startFromDateTimeAcceptable: boolean = true;
  public tasks: TaskModel[];
  public message: string;

  private timerSubscription: AnonymousSubscription;

  @ViewChild('instanceForm') currentForm: NgForm;

  public formErrors: { [key: string]: string } = {
    'instanceJvmOptions': '',
    'instanceOptions': '',
    'instanceNodeAttributes': '',
    'instanceEnvironmentVariables': '',
  };

  public validationMessages: { [key: string]: { [key: string]: string } } = {
    'instanceOptions': {
      'validJson': 'JVM options value is not a valid json'
    },
    'instanceJvmOptions': {
      'validJson': 'JVM options value is not a valid json'
    },
    'instanceNodeAttributes': {
      'validJson': 'Node attributes value is not a valid json'
    },
    'instanceEnvironmentVariables': {
      'validJson': 'Environment variables value is not a valid json'
    }
  };

  constructor(
    private instancesService: InstancesService,
    private modulesService: ModulesService,
    private streamsService: StreamsService,
    private servicesService: ServicesService) { }

  public ngOnInit() {
    this.newInstance = new InstanceModel();
    this.getInstancesList();
    this.getModulesList();
    this.getModuleTypes();
    this.getStreamsList();
    this.getServicesList();
  }

  public ngOnDestroy(): void {
    if (this.timerSubscription) {
      this.timerSubscription.unsubscribe();
    }
  }

  public getInstancesList() {
    this.instancesService.getList('instances')
      .subscribe(
        response => {
          this.instancesList = response.instances;
          this.cloneInstancesList = response.instances;
          if (this.instancesList.length > 0) {
            this.getInstanceInfo(this.instancesList[0]);
          }
        },
        error => this.errorMessage = <any>error);
  }

  public getModulesList() {
    this.modulesService.getList()
      .subscribe(
        response => {
          this.modulesList = response.modules;
        },
        error => this.errorMessage = <any>error);
  }

  public getModuleTypes() {
    this.modulesService.getTypes()
      .subscribe(response => this.moduleTypes = response.types);
  }

  public getStreamsList() {
    this.streamsService.getList()
      .subscribe(
        response => {
          this.streamsList = response.streams;
          for (const stream of this.streamsList) {
            this.streamTypesList[stream.name] = stream.type;
          }
        },
        error => this.errorMessage = <any>error);
  }

  public getServicesList() {
    this.servicesService.getList()
      .subscribe(
        response => {
          this.servicesList = response.services;
          this.isFormReady = true;
        },

        error => this.errorMessage = <any>error);
  }

  public getInstanceInfo(currentInstance: InstanceModel) {
    if ( !this.currentInstance || this.currentInstance.name != currentInstance.name &&
      this.currentInstance.module.moduleName != currentInstance.moduleName &&
      this.currentInstance.module.moduleType != currentInstance.moduleType &&
      this.currentInstance.module.moduleVersion != currentInstance.moduleVersion
    ) {
      this.instancesService.getInstanceInfo(currentInstance)
        .subscribe(
          instance => {
            this.currentInstance = instance;
            this.currentInstance.module = new ModuleModel();
            this.currentInstance.module.moduleName = currentInstance.moduleName;
            this.currentInstance.module.moduleType = currentInstance.moduleType;
            this.currentInstance.module.moduleVersion = currentInstance.moduleVersion;
            this.currentInstanceTasks = this.currentInstance.module.moduleType !== 'input-streaming' ?
              this.currentInstance.executionPlan.tasks :
              this.currentInstance.tasks;
          },
          error => this.errorMessage = <any>error);
    }
  }

  public getInstanceTasks(instance: InstanceModel) {
    this.instancesService.getInstanceTasks(instance)
      .subscribe(
        response => {
          if (response) {
            this.tasks = response.tasks;
          }
        }
      );
  }

  public selectInstance(instance: InstanceModel) {
    this.getInstanceInfo(instance);
  }

  public createByClone(instance: InstanceModel) {
    this.instancesService.getInstanceInfo(instance)
      .subscribe(
        instanceInfo => {
          this.newInstance = instanceInfo;
          this.newInstance.jvmOptions = JSON.stringify(instanceInfo.jvmOptions);
          this.newInstance.options = JSON.stringify(instanceInfo.options);
          this.newInstance.nodeAttributes = JSON.stringify(instanceInfo.nodeAttributes);
          this.newInstance.environmentVariables = JSON.stringify(instanceInfo.environmentVariables);
          this.newInstance.coordinationService =
            this.servicesList.find(service => service.name === instanceInfo.coordinationService) ? instanceInfo.coordinationService : '';
          this.newInstance.name = '';
          this.newInstance.module = new ModuleModel();
          this.newInstance.module.moduleName = instance.moduleName;
          this.newInstance.module.moduleType = instance.moduleType;
          this.newInstance.module.moduleVersion = instance.moduleVersion;
          if (this.newInstance.module.moduleType === 'batch-streaming') {
            this.newInstance.inputsTypes = [];
            this.newInstance.inputs.forEach(function (item: string, i: number) {
              const input = item.split('/');
              this.newInstance.inputs[i] = input[0];
              this.newInstance.inputsTypes[i] = input[1];
            }.bind(this));
          }
          if (this.newInstance.module.moduleType === 'regular-streaming') {
            this.newInstance.inputsTypes = [];
            this.newInstance.inputs.forEach(function (item: string, i: number) {
              const input = item.split('/');
              this.newInstance.inputs[i] = input[0];
              this.newInstance.inputsTypes[i] = input[1];
            }.bind(this));
          }
        },
        error => this.errorMessage = <any>error);
  }

  public cloneInstance(instance: InstanceModel, modal: ModalDirective) {
    this.isInstanceClone = true;
    this.createByClone(instance);
    modal.show();
  }

  public createInstance(modal: ModalDirective) {
    const req = this.instancesService.saveInstance(this.newInstance);
    this.showSpinner = true;
    req.subscribe(
      response => {
        modal.hide();
        this.newInstance = new InstanceModel();
        this.showSpinner = false;
        this.showAlert({message: response.message, type: 'success', closable: true, timeout: 3000});
        this.getInstancesList();
        this.currentForm.reset();
      },
      error => {
        this.showSpinner = false;
        this.formAlerts.push({message: error, type: 'danger', closable: true, timeout: 0});
      });
  }

  public closeModal(modal: ModalDirective) {
    this.newInstance = new InstanceModel();
    modal.hide();
    this.formAlerts = [];
    this.currentForm.reset();
  }

  public showAlert(notification: NotificationModel): void {
    if (!this.alerts.find(msg => msg.message === notification.message)) {
      this.alerts.push(notification);
    }
  }

  public showInstanceTasks(modal: ModalDirective, instance: InstanceModel) {
    this.getInstanceTasks(instance);
    modal.show();
  }

  public deleteInstanceConfirm(modal: ModalDirective, instance: InstanceModel) {
    this.getInstanceInfo(instance);
    modal.show();
  }

  public deleteInstance(modal: ModalDirective) {
    this.instancesService.deleteInstance(this.currentInstance)
      .subscribe(
        response => {
          this.showAlert({ message: response.message, type: 'success', closable: true, timeout: 3000 });
          this.getInstancesList();
        },
        error => {
          this.showAlert({ message: error, type: 'danger', closable: true, timeout: 0 });
        });
    modal.hide();
  }

  public clearInstance() {
    this.newInstance = new InstanceModel();
    this.cloningInstance = new InstanceModel();
  }

  public startInstance(instance: InstanceModel) {
    this.instancesService.startInstance(instance)
      .subscribe(
        response => {
          this.showAlert({ message: response.message, type: 'success', closable: true, timeout: 3000 });
          this.timerSubscription = Observable.interval(3000).subscribe(() => {
            return this.getInstancesList();
          });
        },
        error => {
          this.showAlert({ message: error, type: 'danger', closable: true, timeout: 0 });
        });
  }

  public stopInstance(instance: InstanceModel) {
    this.instancesService.stopInstance(instance)
      .subscribe(
        response => {
          this.showAlert({ message: response.message, type: 'success', closable: true, timeout: 3000 });
          this.timerSubscription = Observable.interval(3000).subscribe(() => {
            return this.getInstancesList();
          });
        },
        error => {
          this.showAlert({ message: error, type: 'danger', closable: true, timeout: 0 });
        });
  }

  public addInput() {
    this.newInstance.inputs.push('');
    this.newInstance.inputsTypes.push('');
    this.checkTimestampAcceptable();
  }

  public addOutput() {
    this.newInstance.outputs.push('');
  }

  public removeInput(i: number): void {
    this.newInstance.inputs.splice(i, 1);
    this.newInstance.inputsTypes.splice(i, 1);
    this.checkTimestampAcceptable();
  }

  public removeOutput(i: number): void {
    this.newInstance.outputs.splice(i, 1);
  }

  public checkTimestampAcceptable(): void {
    switch (this.newInstance.module.moduleType) {
      case 'regular-streaming':
      case 'batch-streaming':
        if (this.newInstance.inputs &&  this.newInstance.inputs.length > 0 && this.newInstance.inputs[0]) {
          this.startFromDateTimeAcceptable = true;
          for (const inputName of this.newInstance.inputs) {
            if (this.streamTypesList[inputName] !== 'stream.kafka') {
              this.startFromDateTimeAcceptable = false;
              break;
            }
          }
        }
        break;
      case 'output-streaming':
        this.startFromDateTimeAcceptable = false;
        break;
      default:
        console.error('start-from field is not provided for module-type ' + this.newInstance.module.moduleType);
        break;
    }
    if (!this.startFromDateTimeAcceptable && this.newInstance.startFrom === 'timestamp') {
      this.newInstance.startFrom = '';
    }
  }

  public ifInstanceCanBeRemoved(): boolean {
    return ['starting', 'started', 'stopping'].indexOf(this.currentInstance.status) === -1;
  }

  public ngAfterViewChecked() {
    if (this.currentForm === this.instanceForm) { return; }
    this.instanceForm = this.currentForm;
    if (this.instanceForm) {
      this.instanceForm.valueChanges
        .subscribe(data => this.onValueChanged(data));
    }
  }

  public onValueChanged(data?: any) {
    if (!this.instanceForm) { return; }
    const form = this.instanceForm.form;

    for (const field of Object.keys(this.formErrors)) {
      // clear previous error message (if any)
      this.formErrors[field] = '';
      const control = form.get(field);
      if (control && control.dirty && !control.valid) {
        const messages = this.validationMessages[field];
        for (const key of Object.keys(control.errors)) {
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
