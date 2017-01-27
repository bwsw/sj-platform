import { Component, OnInit } from '@angular/core';

import { ModuleModel } from '../shared/models/module.model';
import { InstanceModel } from '../shared/models/instance.model';
import { ModulesService } from '../shared/services/modules.service';
import { InstancesService } from '../shared/services/instances.service';
import { ModalDirective } from 'ng2-bootstrap';

@Component({
  moduleId: module.id,
  selector: 'sj-modules',
  templateUrl: 'modules.component.html'
})
export class ModulesComponent implements OnInit {
  public errorMessage: string;
  public moduleList: ModuleModel[];
  public instanceList: InstanceModel[];
  public blockingInstances: InstanceModel[] = [];
  public alerts: Array<Object> = [];
  public currentModule: ModuleModel;
  public currentModuleSpecification: ModuleModel;
  public isUploading: boolean = false;
  public showSpinner: boolean;

  constructor(private modulesService: ModulesService,
              private instancesService: InstancesService) {
  }

  public ngOnInit() {
    this.getModuleList();
    this.getInstanceList();
  }

  public getModuleList() {
    this.modulesService.getModuleList()
      .subscribe(
        moduleList => {
          this.moduleList = moduleList;
          if (moduleList.length > 0) {
            this.currentModule = moduleList[0];
            this.getModuleSpecification(this.currentModule);
          }
        },
        error => this.errorMessage = <any>error);
  }

  public getInstanceList() {
    this.instancesService.getInstanceList()
      .subscribe(
        instanceList => {
          this.instanceList = instanceList;
        },
        error => this.errorMessage = <any>error);
  }

  public getModuleSpecification(module: ModuleModel) {
    this.modulesService.getModuleSpecification(module)
      .subscribe(
        moduleSpec => this.currentModuleSpecification = moduleSpec,
        error => this.errorMessage = <any>error);
  }

  public deleteModuleConfirm(modal: ModalDirective, module: ModuleModel) {
    this.currentModule = module;
    this.blockingInstances = [];
    this.instanceList.forEach((item: InstanceModel) => {
      if (item['module-name'] === this.currentModule['module-name'] &&
        item['module-type'] === this.currentModule['module-type'] &&
        item['module-version'] === this.currentModule['module-version']) {
        this.blockingInstances.push(item);
      }
    });
    modal.show();
  }

  public deleteModule(modal: ModalDirective) {
    this.modulesService.deleteModule(this.currentModule)
      .subscribe(
        status => {
          this.showAlert({ msg: status, type: 'success', closable: true, timeout: 3000 });
          this.getModuleList();
        },
        error => this.showAlert({ msg: error, type: 'danger', closable: true, timeout: 0 }));
    modal.hide();
  }

  public downloadModule(module: ModuleModel) {
    this.showSpinner = true;
    this.modulesService.downloadModule(module)
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
          this.showAlert({ msg: error, type: 'danger', closable: true, timeout: 0 });
        });
  }

  public uploadFile(event: any) {
    this.isUploading = true;
    let file = event.target.files[0];
    if (file) {
      this.modulesService.uploadModule(file).then((result: any) => {
        this.isUploading = false;
        this.showAlert({ msg: result, type: 'success', closable: true, timeout: 3000 });
        event.target.value = null;
        this.getModuleList();
      },
        (error: any) => {
        this.isUploading = false;
        event.target.value = null;
        this.showAlert({ msg: error, type: 'danger', closable: true, timeout: 0 });
      });
    } else {
      this.isUploading = false;
    }
  }

  public selectModule(module: ModuleModel) {
    this.currentModule = module;
    this.getModuleSpecification(module);
  }

  public closeAlert(i: number): void {
    this.alerts.splice(i, 1);
  }

  public showAlert(message: Object): void {
    this.alerts = [];
    this.alerts.push(message);
  }

  public isSelected(module: ModuleModel) {
    return module === this.currentModule;
  }
}
