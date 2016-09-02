import {
    CORE_DIRECTIVES,
    FormBuilder,
    ControlGroup,
} from '@angular/common';
import {FORM_DIRECTIVES, REACTIVE_FORM_DIRECTIVES} from '@angular/forms';
import { Component, OnInit } from '@angular/core';
import { ACCORDION_DIRECTIVES, MODAL_DIRECTIVES, BS_VIEW_PROVIDERS, AlertComponent  } from 'ng2-bootstrap/ng2-bootstrap';
import { ListFilter } from '../shared/listFilter/list-filter';
import { OrderBy } from '../shared/orderBy/orderBy-pipe';
import 'rxjs/Rx';
import { ServiceFilter } from '../shared/service-filter';
import { SearchBoxComponent } from '../shared/searchBox/search-box';
import { Instance } from './instance';
import { InstanceService } from './instance.service';
import { Module } from '../modules/module';
import { ModuleService } from '../modules/module.service';
import { Service } from '../services/service';
import { ServiceService } from '../services/service.service';
import { Stream } from '../streams/stream';
import { StreamService } from '../streams/stream.service';
import {ModalDirective} from 'ng2-bootstrap/ng2-bootstrap';

@Component({
  moduleId: module.id,
  selector: 'sd-instances',
  templateUrl: 'instances.component.html',
  styleUrls: ['instances.component.css'],
  directives: [ACCORDION_DIRECTIVES, FORM_DIRECTIVES, CORE_DIRECTIVES, REACTIVE_FORM_DIRECTIVES, MODAL_DIRECTIVES,
    SearchBoxComponent, AlertComponent],
  pipes: [ListFilter, ServiceFilter, OrderBy],
  viewProviders: [BS_VIEW_PROVIDERS],
  providers: [InstanceService, ModuleService, ServiceService, StreamService]
})

export class InstancesComponent implements OnInit {
  public alerts:Array<Object> = [];
  errorMessage:string;
  form_ready: boolean = false;
  instanceList:Instance[];
  cloneInstanceList:Instance[] = [];
  moduleList:Module[];
  serviceList: Service[];
  streamList: Stream[];
  current_instance: Instance;
  current_instance_info: Instance;
  current_instance_tasks: string[];
  new_instance: Instance;
  cloneInstance:boolean = false;
  new_instance_module: Module;
  instance_to_delete: Instance;
  instance_to_clone: Instance;
  instanceForm: ControlGroup;

  constructor(
      private instanceService:InstanceService,
      private moduleService:ModuleService,
      private streamService:StreamService,
      private serviceService:ServiceService,
      private fb: FormBuilder
    ) {}

  ngOnInit() {
    this.getInstanceList();
    this.getModuleList();
    this.getStreamList();
    this.getServiceList();
    this.instanceService.getInstanceList()
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
    setInterval(function() {
      this.getInstanceList();
    }.bind(this), 2000);
    this.new_instance = new Instance();
    this.instanceForm = this.fb.group({
      //firstName: ['', Validators.required],
      //lastName: ['', Validators.required],
      //email: ['', Validators.compose([Validators.required])],
      //phone: ['', Validators.required],
    });
  }

  getInstanceList() {
    this.instanceService.getInstanceList()
        .subscribe(
            instanceList => { this.instanceList = instanceList;
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

  getModuleList() {
    this.moduleService.getModuleList()
        .subscribe(
            moduleList => {
              this.moduleList = moduleList;
            },
            error => this.errorMessage = <any>error);
  }
  getStreamList() {
    this.streamService.getStreamList()
        .subscribe(
            streamList => {
              this.streamList = streamList;
            },
            error => this.errorMessage = <any>error);
  }
  getServiceList() {
    this.serviceService.getServiceList()
        .subscribe(
            serviceList => { this.serviceList = serviceList;
              this.form_ready = true;
            },

            error => this.errorMessage = <any>error);
  }
  get_instance_info(instance:Instance) {
    this.instanceService.getInstanceInfo(instance)
        .subscribe(
            instanceInfo => { this.current_instance_info = instanceInfo;
              //this.new_instance = instanceInfo;
              this.current_instance_info.module = new Module();
              this.current_instance_info.module['module-name'] = instance['module-name'];
              this.current_instance_info.module['module-type'] = instance['module-type'];
              this.current_instance_info.module['module-version'] = instance['module-version'];
              this.current_instance_tasks = Object.keys(this.current_instance_info['execution-plan']['tasks']);
            },
            error => this.errorMessage = <any>error);
  }
  instance_select(instance: Instance) {
    this.current_instance = instance;
    this.get_instance_info(instance);
  }
  createByClone(instanceIndex: number) {
    let instance = this.cloneInstanceList[instanceIndex];
    this.instanceService.getInstanceInfo(instance)
      .subscribe(
        instanceInfo => {
          //console.log(this.new_instance.inputs);
          this.new_instance = instanceInfo;
          this.new_instance.options = JSON.stringify(instanceInfo.options);
          this.new_instance['jvm-options'] = JSON.stringify(instanceInfo['jvm-options']);
          this.new_instance['node-attributes'] = JSON.stringify(instanceInfo['node-attributes']);
          this.new_instance['environment-variables'] = JSON.stringify(instanceInfo['environment-variables']);
          this.new_instance.name = '';
          //this.new_instance = instanceInfo;
          this.new_instance.module = new Module();
          this.new_instance.module['module-name'] = instance['module-name'];
          this.new_instance.module['module-type'] = instance['module-type'];
          this.new_instance.module['module-version'] = instance['module-version'];
          if (this.new_instance.module['module-type'] === 'regular-streaming') {
            this.new_instance['input-type'] = [];
            this.new_instance.inputs.forEach(function(item:string, i:number) {
              let parsedInput = item.split('/');
              this.new_instance.inputs[i] = parsedInput[0];
              this.new_instance['input-type'][i] = parsedInput[1];
            }.bind(this));
          }
          console.log(this.new_instance);
          //this.current_instance_tasks = Object.keys(this.current_instance_info['execution-plan']['tasks']);
        },
        error => this.errorMessage = <any>error);
    //this.new_instance = instanceName;
  }
  createInstance(modal:ModalDirective) {
    console.log(this.new_instance);
    //this.instanceService.saveInstance(this.new_instance)
    //    .subscribe(
    //        status => {
    //          modal.hide();
    //          //this.new_instance = new Instance();
    //          this.alerts.push({msg: status, type: 'success', closable: true, timeout:3000});
    //          //this.getInstanceList();
    //        },
    //        error => { this.alerts.push({msg: error, type: 'danger', closable: true, timeout:0});
    //          modal.hide();
    //        });
  }
  closeAlert(i:number):void {
    this.alerts.splice(i, 1);
  }
  delete_instance_confirm(modal:ModalDirective, instance: Instance) {
    //console.log(typeof modal);
    //debugger;
    this.instance_to_delete = instance;
    modal.show();
  }
  delete_instance(modal:ModalDirective, instance: Instance) {
    this.instanceService.deleteInstance(instance)
        .subscribe(
            status => {  this.alerts.push({msg: status, type: 'success', closable: true, timeout:3000});
              this.getInstanceList();
            },
            error => this.alerts.push({msg: error, type: 'danger', closable: true, timeout:0}));
    this.instance_to_delete = null;
    modal.hide();
  }
  isSelected(instance:Instance) {
    return instance === this.current_instance;
  }
  start_instance(instance : Instance) {
    this.instanceService.startInstance(instance)
    .subscribe(
        status => {
          //alerts add status message
        },
        error => this.errorMessage = <any>error);
  }
  stop_instance(instance : Instance) {
    this.instanceService.stopInstance(instance)
        .subscribe(
            status => {
              //alerts add status message
            },
            error => this.errorMessage = <any>error);
  }
  addInput() {
    this.new_instance.inputs.push('');
  }
  addOutput() {
    this.new_instance.outputs.push('');
  }
}
