import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { ModalDirective } from 'ng2-bootstrap';
import { CustomService } from '../shared/services/index';
import { FileModel, NotificationModel } from '../shared/models/index';

@Component({
  moduleId: module.id,
  selector: 'sj-custom',
  templateUrl: 'custom.component.html'
})
export class CustomComponent implements OnInit {

  public path: string = 'files';
  public fileList: FileModel[];
  public currentFile: FileModel;
  public newDescription: string;
  public newFile: any;
  public alerts: NotificationModel[] = [];
  public isUploading: boolean = false;
  public showSpinner: boolean;


  constructor(
    private route: ActivatedRoute,
    private customService: CustomService
  ) {}

  public ngOnInit() {
    this.route.params.subscribe(params => {
      this.path = params['path'];
      this.getCustomList();
    });
  }

  public getCustomList() {
    this.customService.getList(this.path)
      .subscribe(
        response => this.fileList = this.path === 'files' ? response.customFiles: response.customJars
      );
  }

  public setNewFile(event: any) {
    this.newFile = event.target.files[0];
  }

  public uploadFile(event: any) {
    this.isUploading = true;
    let file = this.path === 'jars' ? event.target.files[0]: this.newFile;
    if (file) {
      this.customService.upload({path: this.path, file: file, description:  this.newDescription}).then((result: any) => {
          this.isUploading = false;
          this.showAlert({ message: result, type: 'success', closable: true, timeout: 3000 });
          event.target.value = null;
          this.getCustomList();
        },
        (error: any) => {
          this.isUploading = false;
          event.target.value = null;
          this.showAlert({ message: error, type: 'danger', closable: true, timeout: 0 });
        });
    } else {
      this.isUploading = false;
      this.newFile = null;
    }
  }

  public downloadFile(file: FileModel) {
    this.showSpinner = true;
    let params = this.path === 'files'? {name: file.name, path: this.path} :
      {name: file.name, version: file.version, path: this.path};
    this.customService.download(params)
      .subscribe(
        data => {
          let a = document.createElement('a');
          let innerUrl = window.URL.createObjectURL(data['blob']);
          a.style.display = 'none';
          a.href = innerUrl;
          a.download = data['filename'];
          document.body.appendChild(a);
          a.click();
          document.body.removeChild(a);
          // Clean the blob (with timeout (firefox fix))
          setTimeout(()=>window.URL.revokeObjectURL(innerUrl), 1000);
          this.showSpinner = false;
        },
        error => {
          this.showSpinner = false;
          this.showAlert({ message: error, type: 'danger', closable: true, timeout: 0 });
        });
  }

  public deleteFileConfirm(modal: ModalDirective, file: FileModel) {
    this.currentFile = file;
    modal.show();
  }

  public deleteFile(modal: ModalDirective) {
    this.customService.removeFile({path: this.path, name: this.currentFile.name, version: this.currentFile.version})
      .subscribe(
        response => {
          this.showAlert({ message: response.message, type: 'success', closable: true, timeout: 3000 });
          this.getCustomList();
        },
        error => this.showAlert({ message: error, type: 'danger', closable: true, timeout: 0 }));
    modal.hide();
  }

  public showAlert(notification: NotificationModel): void {
    if (!this.alerts.find(msg => msg.message === notification.message)) {
      this.alerts.push(notification);
    }
  }
}
