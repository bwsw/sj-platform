import { Component, OnInit, ViewChild, AfterViewChecked } from '@angular/core';
import { NgForm } from '@angular/forms';
import { ModalDirective } from 'ng2-bootstrap';

import { InstanceModel } from '../shared/models/instance.model';
import { ModuleModel } from '../shared/models/module.model';
import { ServiceModel } from '../shared/models/service.model';
import { StreamModel } from '../shared/models/stream.model';
import { InstancesService } from '../shared/services/instances.service';
import { ModulesService } from '../shared/services/modules.service';
import { StreamsService } from '../shared/services/streams.service';
import { ServicesService } from '../shared/services/services.service';

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
  public servicesList: ServiceModel[];
  public streamsList: StreamModel[];
  public streamTypesList: { [key: string]: string } = {};
  public currentInstance: InstanceModel;
  public currentInstanceTasks: string[];
  public newInstance: InstanceModel;
  public cloneInstance: boolean = false;
  public cloningInstance: InstanceModel;
  public instanceForm: NgForm;
  public showSpinner: boolean;
  public startFromTimestampAcceptable: boolean = true;

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
    this.getStreamsList();
    this.getServicesList();
  }

  public getInstancesList() {
    this.instancesService.getInstanceList()
      .subscribe(
        instancesList => {
          this.instancesList = instancesList;
          this.cloneInstancesList = instancesList;
        },
        error => this.errorMessage = <any>error);
  }

  public getModulesList() {
    this.modulesService.getModuleList()
      .subscribe(
        modulesList => {
          this.modulesList = modulesList;
        },
        error => this.errorMessage = <any>error);
  }

  public getStreamsList() {
    this.streamsService.getStreamList()
      .subscribe(
        streamsList => {
          this.streamsList = streamsList;
          for (let stream of streamsList) {
            this.streamTypesList[stream.name] = stream['stream-type'];
          }
        },
        error => this.errorMessage = <any>error);
  }

  public getServicesList() {
    this.servicesService.getServiceList()
      .subscribe(
        servicesList => {
          this.servicesList = servicesList;
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
          this.currentInstance.module['module-name'] = currentInstance['module-name'];
          this.currentInstance.module['module-type'] = currentInstance['module-type'];
          this.currentInstance.module['module-version'] = currentInstance['module-version'];
          this.currentInstanceTasks = this.currentInstance.module['module-type'] !== 'input-streaming' ?
            this.currentInstance['execution-plan']['tasks']:
            this.currentInstance['tasks'];
        },
        error => this.errorMessage = <any>error);
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
          this.newInstance['jvm-options'] = JSON.stringify(instanceInfo['jvm-options']);
          this.newInstance['options'] = JSON.stringify(instanceInfo['options']);
          this.newInstance['node-attributes'] = JSON.stringify(instanceInfo['node-attributes']);
          this.newInstance['environment-variables'] = JSON.stringify(instanceInfo['environment-variables']);
          this.newInstance['coordination-service'] =
            this.servicesList.find(service => service.name === instanceInfo['coordination-service']) ? instanceInfo['coordination-service']: '';
          this.newInstance.name = '';
          this.newInstance.module = new ModuleModel();
          this.newInstance.module['module-name'] = instance['module-name'];
          this.newInstance.module['module-type'] = instance['module-type'];
          this.newInstance.module['module-version'] = instance['module-version'];
          if (this.newInstance.module['module-type'] === 'windowed-streaming') {
            let batchFillType : { [key: string]: any } = instanceInfo['batch-fill-type'];
            this.newInstance['batch-fill-type-name'] = batchFillType['type-name'];
            this.newInstance['batch-fill-type-value'] = batchFillType['value'];
          }
          if (this.newInstance.module['module-type'] === 'windowed-streaming') {
            let mainStream = instanceInfo['main-stream'].split('/');
            this.newInstance['main-stream'] = mainStream[0];
            this.newInstance['main-stream-type'] = mainStream[1];
            this.newInstance['related-streams-type'] = [];
            instanceInfo['related-streams'].forEach((item: string, i: number) => {
              let related = item.split('/');
              this.newInstance['related-streams'][i] = related[0];
              this.newInstance['related-streams-type'][i] = related[1];
            });

          }
          if (this.newInstance.module['module-type'] === 'regular-streaming') {
            this.newInstance['inputs-types'] = [];
            this.newInstance.inputs.forEach(function (item: string, i: number) {
              let input = item.split('/');
              this.newInstance.inputs[i] = input[0];
              this.newInstance['inputs-types'][i] = input[1];
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
        this.alerts.push({msg: status, type: 'success', closable: true, timeout:3000});
        this.getInstancesList();
      },
      error => {
        this.showSpinner = false;
        modal.hide();
        this.alerts.push({msg: error, type: 'danger', closable: true, timeout:0});
      });
  }

  public closeAlert(i: number): void {
    this.alerts.splice(i, 1);
  }

  public deleteInstanceConfirm(modal: ModalDirective, instance: InstanceModel) {
    this.getInstanceInfo(instance);
    modal.show();
  }

  public deleteInstance(modal: ModalDirective) {
    this.instancesService.deleteInstance(this.currentInstance)
      .subscribe(
        status => {
          this.alerts.push({ msg: status, type: 'success', closable: true, timeout: 3000 });
          this.getInstancesList();
        },
        error => this.alerts.push({ msg: error, type: 'danger', closable: true, timeout: 0 }));
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
          this.alerts.push({ msg: status, type: 'success', closable: true, timeout: 3000 });
        },
        error => this.alerts.push({ msg: error, type: 'danger', closable: true, timeout: 0 }));
  }

  public stopInstance(instance: InstanceModel) {
    this.instancesService.stopInstance(instance)
      .subscribe(
        status => {
          this.alerts.push({ msg: status, type: 'success', closable: true, timeout: 3000 });
        },
        error => this.alerts.push({ msg: error, type: 'danger', closable: true, timeout: 0 }));
  }

  public addInput() {
    this.newInstance.inputs.push('');
    this.newInstance['inputs-types'].push('');
    this.checkTimestampAcceptable();
  }

  public addOutput() {
    this.newInstance.outputs.push('');
  }

  public removeInput(i: number): void {
    this.newInstance.inputs.splice(i, 1);
    this.newInstance['inputs-types'].splice(i, 1);
    this.checkTimestampAcceptable();
  }

  public removeOutput(i: number): void {
    this.newInstance.outputs.splice(i, 1);
  }

  public addRelatedStream() {
    this.newInstance['related-streams'].push('');
  }

  public removeRelatedStream(i: number): void {
    this.newInstance['related-streams'].splice(i, 1);
    this.newInstance['related-streams-type'].splice(i, 1);
  }

  public checkTimestampAcceptable(): void {
    switch (this.newInstance.module['module-type']) {
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
        console.error('start-from field is not provided for module-type '+this.newInstance.module['module-type']);
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
