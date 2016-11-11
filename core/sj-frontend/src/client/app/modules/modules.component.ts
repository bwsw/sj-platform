import { Component, OnInit } from '@angular/core';

import { ModuleModel } from '../shared/models/module.model';
import { InstanceModel } from '../shared/models/instance.model';
import { ModulesService } from '../shared/services/modules.service';
import { InstancesService } from '../shared/services/instances.service';
import { ModalDirective } from 'ng2-bootstrap';

@Component({
  moduleId: module.id,
  selector: 'sj-modules',
  templateUrl: 'modules.component.html',
  styleUrls: ['modules.component.css']
})
export class ModulesComponent implements OnInit {
  public errorMessage: string;
  public moduleList: ModuleModel[];
  public instanceList: InstanceModel[];
  public blockingInstances: InstanceModel[] = [];
  public alerts: Array<Object> = [];
  public current_module: ModuleModel;
  public current_module_specification: ModuleModel;
  public module_to_delete: ModuleModel;
  public upload_in_progress: boolean = false;
  public showSpinner: boolean;

  constructor(private _modulesService: ModulesService,
              private _instancesService: InstancesService) {
  }

  public ngOnInit() {
    this.getModuleList();
    this.getInstanceList();
  }

  public getModuleList() {
    this._modulesService.getModuleList()
      .subscribe(
        moduleList => {
          this.moduleList = moduleList;
          if (moduleList.length > 0) {
            this.current_module = moduleList[0];
            this.get_module_specification(this.current_module);
          }
        },
        error => this.errorMessage = <any>error);
  }

  public getInstanceList() {
    this._instancesService.getInstanceList()
      .subscribe(
        instanceList => {
          this.instanceList = instanceList;
        },
        error => this.errorMessage = <any>error);
  }

  public get_module_specification(module: ModuleModel) {
    this._modulesService.getModuleSpecification(module)
      .subscribe(
        moduleSpec => this.current_module_specification = moduleSpec,
        error => this.errorMessage = <any>error);
  }

  public delete_module_confirm(modal: ModalDirective, module: ModuleModel) {
    this.module_to_delete = module;
    this.blockingInstances = [];
    this.instanceList.forEach((item: InstanceModel) => {
      if (item['module-name'] === this.module_to_delete['module-name'] &&
        item['module-type'] === this.module_to_delete['module-type'] &&
        item['module-version'] === this.module_to_delete['module-version']) {
        this.blockingInstances.push(item);
      }
    });
    modal.show();
  }

  public delete_module(modal: ModalDirective, module: ModuleModel) {
    this._modulesService.deleteModule(module)
      .subscribe(
        status => {
          this.alerts.push({ msg: status, type: 'success', closable: true, timeout: 3000 });
          this.getModuleList();
        },
        error => this.alerts.push({ msg: error, type: 'danger', closable: true, timeout: 0 }));
    this.module_to_delete = null;
    modal.hide();
  }

  public download_module(module: ModuleModel) {
    this.showSpinner = true;
    this._modulesService.downloadModule(module)
      .subscribe(
        data => {
          let a = document.createElement('a');
          let innerUrl = window.URL.createObjectURL(data['blob']);
          a.style.display = 'none';
          a.href = innerUrl;
          a.download = data['filename'] ? data['filename'] : 'module.jar';
          document.body.appendChild(a);
          a.click();
          document.body.removeChild(a);
          // Clean the blob (with timeout (firefox fix))
          setTimeout(()=>window.URL.revokeObjectURL(innerUrl), 1000);
          this.showSpinner = false;
        },
        error => {
          this.showSpinner = false;
          this.alerts.push({ msg: error, type: 'danger', closable: true, timeout: 0 });
        });
  }

  public fileUpload(event: any) {
    this.upload_in_progress = true;
    var file = event.srcElement.files[0];
    this._modulesService.uploadModule(file).then((result: any) => {
      this.upload_in_progress = false;
      this.alerts.push({ msg: result, type: 'success', closable: true, timeout: 3000 });
      this.getModuleList();
    }, (error: any) => {
      this.upload_in_progress = false;
      this.alerts.push({ msg: error, type: 'danger', closable: true, timeout: 0 });
    });
  }

  public module_select(module: ModuleModel) {
    this.current_module = module;
    this.get_module_specification(module);
  }

  public closeAlert(i: number): void {
    this.alerts.splice(i, 1);
  }

  public isSelected(module: ModuleModel) {
    return module === this.current_module;
  }
}
