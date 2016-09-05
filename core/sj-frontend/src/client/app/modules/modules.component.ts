import { Component, OnInit } from '@angular/core';
import { MODAL_DIRECTIVES, BS_VIEW_PROVIDERS, AlertComponent } from 'ng2-bootstrap/ng2-bootstrap';
import { ListFilter } from '../shared/listFilter/list-filter';
import { OrderBy } from '../shared/orderBy/orderBy-pipe';
import 'rxjs/Rx';
import { SearchBoxComponent } from '../shared/searchBox/search-box';
import { Module } from './module';
import { ModuleService } from './module.service';
import { Instance } from '../instances/instance';
import { InstanceService} from '../instances/instance.service';
import {ModalDirective} from 'ng2-bootstrap/ng2-bootstrap';

@Component({
  moduleId: module.id,
  selector: 'sd-modules',
  templateUrl: 'modules.component.html',
  styleUrls: ['modules.component.css'],
  directives: [MODAL_DIRECTIVES, SearchBoxComponent, AlertComponent],
  pipes: [ListFilter, OrderBy],
  viewProviders: [BS_VIEW_PROVIDERS],
  providers: [ModuleService, InstanceService]
})

export class ModulesComponent implements OnInit {
  errorMessage:string;
  moduleList:Module[];
  instanceList:Instance[];
  blockingInstances: Instance[] = [];
  public alerts:Array<Object> = [];
  current_module: Module;
  current_module_specification: Module;
  module_to_delete: Module;
  upload_in_progress:boolean = false;

  constructor(
    private moduleService:ModuleService,
    private instanceService:InstanceService) {
  }

  ngOnInit() {
    this.getModuleList();
    this.getInstanceList();
  }

  getModuleList() {
    this.moduleService.getModuleList()
        .subscribe(
            moduleList => { this.moduleList = moduleList;
              if (moduleList.length > 0) {
                this.current_module = moduleList[0];
                this.get_module_specification(this.current_module);
              }
            },
            error => this.errorMessage = <any>error);
  }
  getInstanceList() {
    this.instanceService.getInstanceList()
      .subscribe(
        instanceList => { this.instanceList = instanceList;
        },
        error => this.errorMessage = <any>error);
  }
  get_module_specification(module:Module) {
    this.moduleService.getModuleSpecification(module)
        .subscribe(
            moduleSpec =>  this.current_module_specification = moduleSpec,
            error => this.errorMessage = <any>error);
  }

  delete_module_confirm(modal:ModalDirective, module:Module) {
    this.module_to_delete = module;
    this.blockingInstances = [];
    this.instanceList.forEach(function(item:Instance, i:number) {
        if (item['module-name'] === this.module_to_delete['module-name'] &&
               item['module-type'] === this.module_to_delete['module-type'] &&
               item['module-version'] === this.module_to_delete['module-version']) {
          this.blockingInstances.push(item);
        }
    }.bind(this));
    modal.show();
  }
  delete_module(modal:ModalDirective, module: Module) {
    this.moduleService.deleteModule(module)
        .subscribe(
            status => { this.alerts.push({msg: status, type: 'success', closable: true, timeout:3000});
              this.getModuleList();
            },
            error => this.alerts.push({msg: error, type: 'danger', closable: true, timeout:0}));
    this.module_to_delete = null;
    modal.hide();
  }
  download_module(module: Module) {
    this.moduleService.downloadModule(module)
      .subscribe(
        data => {
          debugger;
        window.open(window.URL.createObjectURL(data)); },
        error => this.alerts.push({msg: error, type: 'danger', closable: true, timeout:0}));
  }
  fileUpload(event:any) {
    this.upload_in_progress = true;
    var file = event.srcElement.files[0];
    this.moduleService.uploadModule(file).then((result:any) => {
      this.upload_in_progress = false;
      this.alerts.push({msg: result, type: 'success', closable: true, timeout:3000});
      this.getModuleList();
    }, (error:any) => {
      this.upload_in_progress = false;
      this.alerts.push({msg: error, type: 'danger', closable: true, timeout:0});
    });
  }
  module_select(module: Module) {
    this.get_module_specification(module);
  }
  closeAlert(i:number):void {
    this.alerts.splice(i, 1);
  }
  isSelected(module : Module) {
    return module === this.current_module;
  }
}
