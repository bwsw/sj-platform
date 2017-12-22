import { Component, OnInit } from '@angular/core';

import { ModalDirective } from 'ngx-bootstrap';
import { ModuleModel } from './module.model';
import { NotificationModel } from '../shared/model/notification.model';
import { ModulesService } from './modules.service';
import { TypeModel } from '../shared/model/type.model';

@Component({
  selector: 'sj-modules',
  templateUrl: 'modules.component.html',
  providers: [
    ModulesService
  ]
})
export class ModulesComponent implements OnInit {
  public errorMessage: string;
  public moduleList: ModuleModel[];
  public moduleTypes: TypeModel[];
  public blockingInstances: string[] = [];
  public alerts: NotificationModel[] = [];
  public currentModule: ModuleModel;
  public currentModuleSpecification: ModuleModel;
  public isUploading: boolean = false;
  public showSpinner: boolean;
  public modService: ModulesService;

  constructor(private modulesService: ModulesService) {
    this.modService = modulesService;
  }

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
      .subscribe(response => this.blockingInstances = Object.assign({}, response)['instances']);
    modal.show();
  }

  public deleteModule(modal: ModalDirective) {
    this.modulesService.removeFile({name: this.currentModule.moduleName,
      type: this.currentModule.moduleType, version: this.currentModule.moduleVersion})
      .subscribe(
        response => {
          this.showAlert({ message: response.message, type: 'success', closable: true, timeout: 3000 });
          this.getModuleList();
        },
        error => this.showAlert({ message: error, type: 'danger', closable: true, timeout: 0 }));
    modal.hide();
  }

  public downloadModule(module: ModuleModel) {
    this.showSpinner = true;
    this.modulesService.download({name: module.moduleName, type: module.moduleType, version: module.moduleVersion, size: module.size}).then(
      (result: any) => {
        const a = document.createElement('a');
        // console.log(result);
        const innerUrl = window.URL.createObjectURL(result['blob']);
        a.style.display = 'none';
        a.href = innerUrl;
        a.download = result['filename'] ? result['filename'] : 'module.jar';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        // Clean the blob (with timeout (firefox fix))
        setTimeout(() => window.URL.revokeObjectURL(innerUrl), 1000);
        this.showSpinner = false;
      },
      error => {
        this.showSpinner = false;
        this.showAlert({ message: error.error, type: 'danger', closable: true, timeout: 0 });
      }
    );
  }

  public uploadFile(event: any) {
    this.isUploading = true;
    this.showSpinner = true;
    const file = event.target.files[0];
    if (file) {
      this.modulesService.upload({file: file}).then((result: any) => {
        this.isUploading = false;
          this.showSpinner = false;
        this.showAlert({ message: result, type: 'success', closable: true, timeout: 3000 });
        event.target.value = null;
        this.getModuleList();
      },
        (error: any) => {
        this.isUploading = false;
          this.showSpinner = false;
        event.target.value = null;
        this.showAlert({ message: error, type: 'danger', closable: true, timeout: 0 });
      });
    } else {
      this.isUploading = false;
      this.showSpinner = false;
    }
  }

  public selectModule(module: ModuleModel) {
    this.currentModule = module;
    this.getModuleSpecification(module);
  }

  public showAlert(notification: NotificationModel): void {
    if (!this.alerts.find(msg => msg.message === notification.message)) {
      this.alerts.push(notification);
    }
  }
}
