import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
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
  styleUrls: ['instances.component.css'],
  templateUrl: 'instances.component.html',
})
export class InstancesComponent implements OnInit {
  public alerts: Array<Object> = [];
  public errorMessage: string;
  public form_ready: boolean = false;
  public instanceList: InstanceModel[];
  public cloneInstanceList: InstanceModel[] = [];
  public moduleList: ModuleModel[];
  public serviceList: ServiceModel[];
  public streamList: StreamModel[];
  public current_instance: InstanceModel;
  public current_instance_info: InstanceModel;
  public current_instance_tasks: string[];
  public new_instance: InstanceModel;
  public cloneInstance: boolean = false;
  public new_instance_module: ModuleModel;
  public instance_to_delete: InstanceModel;
  public instance_to_clone: InstanceModel;
  public instanceForm: FormGroup;
  public showSpinner: boolean;

  constructor(private _instancesService: InstancesService,
              private _modulesService: ModulesService,
              private _streamsService: StreamsService,
              private _servicesService: ServicesService,
              private _fb: FormBuilder) {
  }

  public ngOnInit() {
    this.getInstanceList();
    this.getModuleList();
    this.getStreamList();
    this.getServiceList();
    this._instancesService.getInstanceList()
      .subscribe(
        instanceList => {
          //if (this.cloneInstanceList.length === 0) {
          this.cloneInstanceList = instanceList;
          //}
        },
        error => this.errorMessage = <any>error);
    //if (this.serviceList.length > 0) {
    //  this.form_ready = true;
    //}
    setInterval(function () {
      this.getInstanceList();
    }.bind(this), 2000);
    this.new_instance = new InstanceModel();
    this.instanceForm = this._fb.group({
      //firstName: ['', Validators.required],
      //lastName: ['', Validators.required],
      //email: ['', Validators.compose([Validators.required])],
      //phone: ['', Validators.required],
    });
  }

  public getInstanceList() {
    this._instancesService.getInstanceList()
      .subscribe(
        instanceList => {
          this.instanceList = instanceList;
          //if (this.cloneInstanceList.length === 0) {
          //  this.cloneInstanceList = instanceList;
          //}
          if (this.instanceList.length > 0) {
            //this.current_instance = instanceList[0];
            //this.get_instance_info(this.current_instance);
          }
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
          //this.new_instance = instanceInfo;
          this.current_instance_info.module = new ModuleModel();
          this.current_instance_info.module['module-name'] = instance['module-name'];
          this.current_instance_info.module['module-type'] = instance['module-type'];
          this.current_instance_info.module['module-version'] = instance['module-version'];
          this.current_instance_tasks = Object.keys(this.current_instance_info['execution-plan']['tasks']);
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
          this.new_instance.options = JSON.stringify(instanceInfo.options);
          this.new_instance['jvm-options'] = JSON.stringify(instanceInfo['jvm-options']);
          this.new_instance['node-attributes'] = JSON.stringify(instanceInfo['node-attributes']);
          this.new_instance['environment-variables'] = JSON.stringify(instanceInfo['environment-variables']);
          this.new_instance.name = '';
          this.new_instance.module = new ModuleModel();
          this.new_instance.module['module-name'] = instance['module-name'];
          this.new_instance.module['module-type'] = instance['module-type'];
          this.new_instance.module['module-version'] = instance['module-version'];
          if (this.new_instance.module['module-type'] === 'regular-streaming') {
            this.new_instance['input-type'] = [];
            this.new_instance.inputs.forEach(function (item: string, i: number) {
              let parsedInput = item.split('/');
              this.new_instance.inputs[i] = parsedInput[0];
              this.new_instance['input-type'][i] = parsedInput[1];
            }.bind(this));
          }
          //this.current_instance_tasks = Object.keys(this.current_instance_info['execution-plan']['tasks']);
        },
        error => this.errorMessage = <any>error);
    //this.new_instance = instanceName;
  }

  public createInstance(modal: ModalDirective) {
    let req = this._instancesService.saveInstance(this.new_instance);
    this.showSpinner = true;
    req.subscribe(
      status => {
        modal.hide();
        this.showSpinner = false;
        this.alerts.push({msg: status, type: 'success', closable: true, timeout:3000});
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
    return instance === this.current_instance;
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
  }

  public addOutput() {
    this.new_instance.outputs.push('');
  }

  // TODO: change to x button for every line (get code from providers-hosts form element)
  removeLastInput() {
    this.new_instance.inputs.pop();
    this.new_instance['input-type'].pop();
  }

  removeLastOutput() {
    this.new_instance.outputs.pop();
  }

}
