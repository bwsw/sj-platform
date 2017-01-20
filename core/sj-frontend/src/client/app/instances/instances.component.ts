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
  public form_ready: boolean = false;
  public instanceList: InstanceModel[];
  public cloneInstanceList: InstanceModel[] = [];
  public moduleList: ModuleModel[];
  public serviceList: ServiceModel[];
  public streamList: StreamModel[];
  public streamTypesList: { [key: string]: string } = {};
  public current_instance: InstanceModel;
  public current_instance_info: InstanceModel;
  public current_instance_tasks: string[];
  public new_instance: InstanceModel;
  public cloneInstance: boolean = false;
  public instance_to_delete: InstanceModel;
  public instance_to_clone: InstanceModel;
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

  constructor(private _instancesService: InstancesService,
              private _modulesService: ModulesService,
              private _streamsService: StreamsService,
              private _servicesService: ServicesService) {
  }

  public ngOnInit() {
    this.new_instance = new InstanceModel();
    this.getInstanceList();
    this.getModuleList();
    this.getStreamList();
    this.getServiceList();
  }

  public getInstanceList() {
    this._instancesService.getInstanceList()
      .subscribe(
        instanceList => {
          this.instanceList = instanceList;
          this.cloneInstanceList = instanceList;
        },
        error => this.errorMessage = <any>error);
  }

  public getModuleList() {
    this._modulesService.getModuleList()
      .subscribe(
        moduleList => {
          this.moduleList = moduleList;
        },
        error => this.errorMessage = <any>error);
  }

  public getStreamList() {
    this._streamsService.getStreamList()
      .subscribe(
        streamList => {
          this.streamList = streamList;
          for (let stream of streamList) {
            this.streamTypesList[stream.name] = stream['stream-type'];
          }
        },
        error => this.errorMessage = <any>error);
  }

  public getServiceList() {
    this._servicesService.getServiceList()
      .subscribe(
        serviceList => {
          this.serviceList = serviceList;
          this.form_ready = true;
        },

        error => this.errorMessage = <any>error);
  }

  public get_instance_info(instance: InstanceModel) {
    this._instancesService.getInstanceInfo(instance)
      .subscribe(
        instanceInfo => {
          this.current_instance_info = instanceInfo;
          this.current_instance_info.module = new ModuleModel();
          this.current_instance_info.module['module-name'] = instance['module-name'];
          this.current_instance_info.module['module-type'] = instance['module-type'];
          this.current_instance_info.module['module-version'] = instance['module-version'];
          this.current_instance_tasks = this.current_instance_info.module['module-type'] !== 'input-streaming' ?
            this.current_instance_info['execution-plan']['tasks']:
            this.current_instance_info['tasks'];
        },
        error => this.errorMessage = <any>error);
  }

  public instance_select(instance: InstanceModel) {
    this.current_instance = instance;
    this.get_instance_info(instance);
  }

  public createByClone(instanceIndex: number) {
    let instance = this.cloneInstanceList[instanceIndex];
    this._instancesService.getInstanceInfo(instance)
      .subscribe(
        instanceInfo => {
          this.new_instance = instanceInfo;
          this.new_instance['jvm-options'] = JSON.stringify(instanceInfo['jvm-options']);
          this.new_instance['options'] = JSON.stringify(instanceInfo['options']);
          this.new_instance['node-attributes'] = JSON.stringify(instanceInfo['node-attributes']);
          this.new_instance['environment-variables'] = JSON.stringify(instanceInfo['environment-variables']);
          this.new_instance['coordination-service'] =
            this.serviceList.find(service => service.name === instanceInfo['coordination-service']) ? instanceInfo['coordination-service']: '';
          this.new_instance.name = '';
          this.new_instance.module = new ModuleModel();
          this.new_instance.module['module-name'] = instance['module-name'];
          this.new_instance.module['module-type'] = instance['module-type'];
          this.new_instance.module['module-version'] = instance['module-version'];
          if (this.new_instance.module['module-type'] === 'regular-streaming') {
            this.new_instance['inputs-types'] = [];
            this.new_instance.inputs.forEach(function (item: string, i: number) {
              let parsedInput = item.split('/');
              this.new_instance.inputs[i] = parsedInput[0];
              this.new_instance['inputs-types'][i] = parsedInput[1];
            }.bind(this));
          }
        },
        error => this.errorMessage = <any>error);
  }

  public createInstance(modal: ModalDirective) {
    let req = this._instancesService.saveInstance(this.new_instance);
    this.showSpinner = true;
    req.subscribe(
      status => {
        modal.hide();
        this.new_instance = new InstanceModel();
        this.showSpinner = false;
        this.alerts.push({msg: status, type: 'success', closable: true, timeout:3000});
      },
      error => {
        this.showSpinner = false;
        modal.hide();
        this.alerts.push({msg: error, type: 'danger', closable: true, timeout:0});
      });
    this.getInstanceList();
  }

  public closeAlert(i: number): void {
    this.alerts.splice(i, 1);
  }

  public delete_instance_confirm(modal: ModalDirective, instance: InstanceModel) {
    //console.log(typeof modal);
    //debugger;
    this.instance_to_delete = instance;
    modal.show();
  }

  public delete_instance(modal: ModalDirective, instance: InstanceModel) {
    this._instancesService.deleteInstance(instance)
      .subscribe(
        status => {
          this.alerts.push({ msg: status, type: 'success', closable: true, timeout: 3000 });
          this.getInstanceList();
        },
        error => this.alerts.push({ msg: error, type: 'danger', closable: true, timeout: 0 }));
    this.instance_to_delete = null;
    modal.hide();
  }

  isSelected(instance: InstanceModel) {
    return this.current_instance && instance.name === this.current_instance.name;
  }

  public start_instance(instance: InstanceModel) {
    this._instancesService.startInstance(instance)
      .subscribe(
        status => {
          //alerts add status message
        },
        error => this.errorMessage = <any>error);
  }

  public stop_instance(instance: InstanceModel) {
    this._instancesService.stopInstance(instance)
      .subscribe(
        status => {
          //alerts add status message
        },
        error => this.errorMessage = <any>error);
  }

  public addInput() {
    this.new_instance.inputs.push('');
    this.new_instance['inputs-types'].push('');
    this.checkTimestampAcceptable();
  }

  public addOutput() {
    this.new_instance.outputs.push('');
  }

  public removeInput(i: number): void {
    this.new_instance.inputs.splice(i, 1);
    this.new_instance['inputs-types'].splice(i, 1);
    this.checkTimestampAcceptable();
  }

  public removeOutput(i: number): void {
    this.new_instance.outputs.splice(i, 1);
  }

  public addRelatedStream() {
    this.new_instance['related-streams'].push('');
  }

  public removeRelatedStream(i: number): void {
    this.new_instance['related-streams'].splice(i, 1);
  }

  public checkTimestampAcceptable(): void {
    switch (this.new_instance.module['module-type']) {
      case 'regular-streaming':
      case 'windowed-streaming':
        if (this.new_instance.inputs &&  this.new_instance.inputs.length > 0 && this.new_instance.inputs[0]) {
          this.startFromTimestampAcceptable = true;
          for (let inputName of this.new_instance.inputs) {
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
        console.error('start-from field is not provided for module-type '+this.new_instance.module['module-type']);
        break;
    }
    if (!this.startFromTimestampAcceptable && this.new_instance['start-from'] === 'timestamp') {
      this.new_instance['start-from'] = '';
    }
  }

  public ifInstanceCanBeRemoved(): boolean {
    return ['starting', 'started', 'stopping'].indexOf(this.instance_to_delete.status) === -1;
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
