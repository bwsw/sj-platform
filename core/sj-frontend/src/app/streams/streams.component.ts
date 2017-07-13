import { Component, OnInit } from '@angular/core';
import { ModalDirective } from 'ngx-bootstrap';
import { NotificationModel } from '../shared/model/notification.model';
import { StreamModel } from './stream.model';
import { ServiceModel } from '../services/service.model';
import { StreamsService } from './streams.service';
import { ServicesService } from '../services/services.service';
import { ProvidersService } from '../providers/providers.service';



@Component({
  selector: 'sj-streams',
  templateUrl: 'streams.component.html',
  providers: [
    ServicesService,
    ProvidersService,
    StreamsService
  ]
})
export class StreamsComponent implements OnInit {
  public errorMessage: string;
  public alerts: NotificationModel[] = [];
  public formAlerts: NotificationModel[] = [];
  public streamList: StreamModel[];
  public streamTypes: string[];
  public serviceList: ServiceModel[] = [];
  public currentStream: StreamModel;
  public blockingInstances: string[] = [];
  public showSpinner: boolean = false;
  public currentTag: string;
  public currentStreamService: ServiceModel;
  public newStream: StreamModel;

  constructor(private streamsService: StreamsService,
              private servicesService: ServicesService) { }

  public ngOnInit() {
    this.getStreamList();
    this.getServiceList();
    this.getStreamTypes();
    this.newStream = new StreamModel();
    this.newStream.tags = [];
  }

  public keyDown(event: KeyboardEvent) {
    if (event.keyCode === 13) {
      this.newStream.tags.push(this.currentTag);
      this.currentTag = '';
      event.preventDefault();
    } else if (event.keyCode === 8 && this.currentTag.length === 0) {
      this.newStream.tags.pop();
    }
  }

  public deleteTag(index: number) {
    this.newStream.tags.splice(index, 1);
  }

  public blur() {
    if (this.currentTag !== '') {
      this.newStream.tags.push(this.currentTag);
      this.currentTag = '';
    }
  }

  public focus() {
    document.getElementById('tagInput').focus();
  }

  public getStreamList() {
    this.streamsService.getList()
      .subscribe(
        response => {
          this.streamList = response.streams;
          if (this.streamList.length > 0 ) {
            this.currentStream = this.streamList[0];
          }
        },
        error => this.errorMessage = <any>error);
  }

  public getStreamTypes() {
    this.streamsService.getTypes()
      .subscribe(
        response => this.streamTypes = response.types,
        error => this.showAlert({ message: error, type: 'danger', closable: true, timeout: 0 })
      );
  }

  public getServiceList() {
    this.servicesService.getList()
      .subscribe(
        response => this.serviceList = response.services,
        error => this.errorMessage = <any>error);
  }

  public getService(serviceName: string) {
    this.servicesService.get(serviceName)
      .subscribe(
        response => this.currentStreamService = response.service,
        error => this.errorMessage = <any>error);
  }

  public getServiceInfo(modal: ModalDirective, serviceName: string) {
    this.getService(serviceName);
    modal.show();
  }

  public deleteStreamConfirm(modal: ModalDirective, stream: StreamModel) {
    this.currentStream = stream;
    this.streamsService.getRelatedList(stream.name)
      .subscribe(response => this.blockingInstances = Object.assign({}, response)['instances']);
    modal.show();
  }

  public deleteStream(modal: ModalDirective) {
    this.streamsService.remove(this.currentStream.name)
      .subscribe(
        response => {
          this.showAlert({ message: response.message, type: 'success', closable: true, timeout: 3000 });
          this.getStreamList();
        },
        error => this.showAlert({ message: error, type: 'danger', closable: true, timeout: 0 }));
    modal.hide();
  }

  public createStream(modal: ModalDirective) {
    this.showSpinner = true;
    /*if (this.newStream['stream-type'] !== 'stream.t-stream') {
      delete this.newStream.generator;
    }*/
    this.streamsService.save(this.newStream)
      .subscribe(
        response => {
          modal.hide();
          this.showSpinner = false;
          this.showAlert({ message: response.message, type: 'success', closable: true, timeout: 3000 });
          this.getStreamList();
          this.newStream = new StreamModel();
          this.newStream.tags = [];
        },
        error => {
          this.showSpinner = false;
          this.formAlerts.push({ message: error, type: 'danger', closable: true, timeout: 0 });
        });
  }

  public selectStream(stream: StreamModel) {
    this.currentStream = stream;
  }

  public showAlert(notification: NotificationModel): void {
    if (!this.alerts.find(msg => msg.message === notification.message)) {
      this.alerts.push(notification);
    }
  }
}
