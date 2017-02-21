import { Component, OnInit, ViewChild, AfterViewChecked } from '@angular/core';
import { NgForm } from '@angular/forms';
import { ModalDirective } from 'ng2-bootstrap';

import { InstanceModel, TaskModel, ModuleModel, ServiceModel, StreamModel } from '../shared/models/index';
import { InstancesService, ModulesService, StreamsService, ServicesService } from '../shared/services/index';

@Component({
  moduleId: module.id,
  selector: 'sj-instances',
  templateUrl: 'instances.component.html'
})
export class InstancesComponent implements OnInit, AfterViewChecked {

  public alerts: Array<Object> = [];
  public errorMessage: string;
  public isFormReady: boolean = false;
  public instancesList: InstanceModel[];
  public cloneInstancesList: InstanceModel[] = [];
  public modulesList: ModuleModel[];
  public moduleTypes: string[];
  public servicesList: ServiceModel[];
  public streamsList: StreamModel[];
  public streamTypesList: { [key: string]: string } = {};
  public currentInstance: InstanceModel;
  public currentInstanceTasks: {};
  public newInstance: InstanceModel;
  public cloneInstance: boolean = false;
  public cloningInstance: InstanceModel;
  public instanceForm: NgForm;
  public showSpinner: boolean;
  public startFromTimestampAcceptable: boolean = true;
  public tasks: TaskModel[];
  public message: string;

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

  public getInstancesList() {
    this.instancesService.getList('instances')
      .subscribe(
        response => {
          this.instancesList = response.instances;
          this.cloneInstancesList = response.instances;
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
          for (let stream of this.streamsList) {
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
    this.instancesService.getInstanceInfo(currentInstance)
      .subscribe(
        instance => {
          this.currentInstance = instance;
          this.currentInstance.module = new ModuleModel();
          this.currentInstance.module.moduleName = currentInstance.moduleName;
          this.currentInstance.module.moduleType = currentInstance.moduleType;
          this.currentInstance.module.moduleVersion = currentInstance.moduleVersion;
          this.currentInstanceTasks = this.currentInstance.module.moduleType!== 'input-streaming' ?
            this.currentInstance.executionPlan.tasks:
            this.currentInstance.tasks;
        },
        error => this.errorMessage = <any>error);
  }

  public getInstanceTasks(instance: InstanceModel) {
    this.instancesService.getInstanceTasks(instance)
      .subscribe(
        response => {
          if(response) {
            this.message = response.message;
            this.tasks = response.tasks;
          };
        }
      );
  }

  public selectInstance(instance: InstanceModel) {
    this.getInstanceInfo(instance);
  }

  public createByClone(instanceIndex: number) {
    let instance = this.cloneInstancesList[instanceIndex];
    this.instancesService.getInstanceInfo(instance)
      .subscribe(
        instanceInfo => {
          this.newInstance = instanceInfo;
          this.newInstance.jvmOptions = JSON.stringify(instanceInfo.jvmOptions);
          this.newInstance.options = JSON.stringify(instanceInfo.options);
          this.newInstance.nodeAttributes = JSON.stringify(instanceInfo.nodeAttributes);
          this.newInstance.environmentVariables = JSON.stringify(instanceInfo.environmentVariables);
          this.newInstance.coordinationService =
            this.servicesList.find(service => service.name === instanceInfo.coordinationService) ? instanceInfo.coordinationService: '';
          this.newInstance.name = '';
          this.newInstance.module = new ModuleModel();
          this.newInstance.module.moduleName = instance.moduleName;
          this.newInstance.module.moduleType = instance.moduleType;
          this.newInstance.module.moduleVersion = instance.moduleVersion;
          if (this.newInstance.module.moduleType === 'windowed-streaming') {
            this.newInstance.batchFillType.typeName = instanceInfo.batchFillType.typeName;
            this.newInstance.batchFillType.value = instanceInfo.batchFillType.value;
          }
          if (this.newInstance.module.moduleType === 'windowed-streaming') {
            let mainStream = instanceInfo.mainStream.split('/');
            this.newInstance.mainStream = mainStream[0];
            this.newInstance.mainStreamType = mainStream[1];
            this.newInstance.relatedStreamsType = [];
            instanceInfo.relatedStreams.forEach((item: string, i: number) => {
              let related = item.split('/');
              this.newInstance.relatedStreams[i] = related[0];
              this.newInstance.relatedStreamsType[i] = related[1];
            });

          }
          if (this.newInstance.module.moduleType === 'regular-streaming') {
            this.newInstance.inputsTypes = [];
            this.newInstance.inputs.forEach(function (item: string, i: number) {
              let input = item.split('/');
              this.newInstance.inputs[i] = input[0];
              this.newInstance.inputsTypes[i] = input[1];
            }.bind(this));
          }
        },
        error => this.errorMessage = <any>error);
  }

  public createInstance(modal: ModalDirective) {
    let req = this.instancesService.saveInstance(this.newInstance);
    this.showSpinner = true;
    req.subscribe(
      status => {
        modal.hide();
        this.newInstance = new InstanceModel();
        this.showSpinner = false;
        this.showAlert({msg: status, type: 'success', closable: true, timeout:3000});
        this.getInstancesList();
      },
      error => {
        this.showSpinner = false;
        modal.hide();
        this.showAlert({msg: error, type: 'danger', closable: true, timeout:0});
      });
  }

  public showAlert(message: Object): void {
    this.alerts = [];
    this.alerts.push(message);
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
        status => {
          this.showAlert({ msg: status, type: 'success', closable: true, timeout: 3000 });
          this.getInstancesList();
        },
        error => {
          this.showAlert({ msg: error, type: 'danger', closable: true, timeout: 0 });
        });
    modal.hide();
  }

  public isSelected(instance: InstanceModel) {
    return this.currentInstance && instance.name === this.currentInstance.name;
  }

  public clearInstance() {
    this.newInstance = new InstanceModel();
    this.cloningInstance = new InstanceModel();
  }

  public startInstance(instance: InstanceModel) {
    this.instancesService.startInstance(instance)
      .subscribe(
        status => {
          this.showAlert({ msg: status, type: 'success', closable: true, timeout: 3000 });
          this.getInstancesList();
        },
        error => {
          this.showAlert({ msg: error, type: 'danger', closable: true, timeout: 0 });
        });
  }

  public stopInstance(instance: InstanceModel) {
    this.instancesService.stopInstance(instance)
      .subscribe(
        status => {
          this.showAlert({ msg: status, type: 'success', closable: true, timeout: 3000 });
          this.getInstancesList();
        },
        error => {
          this.showAlert({ msg: error, type: 'danger', closable: true, timeout: 0 });
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

  public addRelatedStream() {
    this.newInstance['related-streams'].push('');
  }

  public removeRelatedStream(i: number): void {
    this.newInstance.relatedStreams.splice(i, 1);
    this.newInstance.relatedStreamsType.splice(i, 1);
  }

  public checkTimestampAcceptable(): void {
    switch (this.newInstance.module.moduleType) {
      case 'regular-streaming':
      case 'windowed-streaming':
        if (this.newInstance.inputs &&  this.newInstance.inputs.length > 0 && this.newInstance.inputs[0]) {
          this.startFromTimestampAcceptable = true;
          for (let inputName of this.newInstance.inputs) {
            if (this.streamTypesList[inputName] !== 'stream.t-stream') {
              this.startFromTimestampAcceptable = false;
              break;
            }
          }
        }
        break;
      case 'output-streaming':
        this.startFromTimestampAcceptable = true;
        break;
      default:
        console.error('start-from field is not provided for module-type '+this.newInstance.module.moduleType);
        break;
    }
    if (!this.startFromTimestampAcceptable && this.newInstance['start-from'] === 'timestamp') {
      this.newInstance['start-from'] = '';
    }
  }

  public ifInstanceCanBeRemoved(): boolean {
    return ['starting', 'started', 'stopping'].indexOf(this.currentInstance.status) === -1;
  }

  public ngAfterViewChecked() {
    this.formChanged();
  }

  public formChanged() {
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
