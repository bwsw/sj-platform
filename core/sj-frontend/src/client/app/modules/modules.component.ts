import { Component, OnInit } from '@angular/core';

import { ModuleModel } from '../shared/models/module.model';
import { ModulesService } from '../shared/services/modules.service';
import { ModalDirective } from 'ng2-bootstrap';

@Component({
  moduleId: module.id,
  selector: 'sj-modules',
  templateUrl: 'modules.component.html'
})
export class ModulesComponent implements OnInit {
  public errorMessage: string;
  public moduleList: ModuleModel[];
  public moduleTypes: string[];
  public blockingInstances: string[] = [];
  public alerts: Array<Object> = [];
  public currentModule: ModuleModel;
  public currentModuleSpecification: ModuleModel;
  public isUploading: boolean = false;
  public showSpinner: boolean;

  constructor(private modulesService: ModulesService) { }

  public ngOnInit() {
    this.getModuleList();
    this.getModuleTypes();
  }

  public getModuleTypes() {
    this.modulesService.getTypes()
      .subscribe(response => this.moduleTypes = response.types);
  }

  public getModuleList() {
    this.modulesService.getList()
      .subscribe(
        response => {
          this.moduleList = response.modules;
          if (this.moduleList.length > 0) {
            this.currentModule = this.moduleList[0];
            this.getModuleSpecification(this.currentModule);
          }
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
    this.modulesService.getRelatedList(module.moduleName, module.moduleType, module.moduleVersion)
      .subscribe(response => this.blockingInstances = Object.assign({},response)['instances']);
    modal.show();
  }

  public deleteModule(modal: ModalDirective) {
    this.modulesService.removeFile({name: this.currentModule.moduleName,
      type: this.currentModule.moduleType, version: this.currentModule.moduleVersion})
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
    this.modulesService.download({name: module.moduleName, type: module.moduleType, version: module.moduleVersion})
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
      this.modulesService.upload({file:file}).then((result: any) => {
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
