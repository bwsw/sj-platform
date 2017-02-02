import { Component, OnInit } from '@angular/core';
import { ModalDirective } from 'ng2-bootstrap';

import { StreamModel } from '../shared/models/stream.model';
import { ServiceModel } from '../shared/models/service.model';
import { ServicesService } from '../shared/services/services.service';
import { StreamsService } from '../shared/services/streams.service';

@Component({
  moduleId: module.id,
  selector: 'sj-streams',
  templateUrl: 'streams.component.html'
})
export class StreamsComponent implements OnInit {
  public errorMessage: string;
  public alerts: Array<Object> = [];
  public streamList: StreamModel[];
  public streamTypes: string[];
  public serviceList: ServiceModel[];
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
    this.newStream.generator = {
      'generator-type': 'local',
      service: '',
      'instance-count': 0
    };
  }

  public keyUp(event:KeyboardEvent) {
    if (event.keyCode === 32) {
      this.newStream.tags.push(this.currentTag.substr(0, this.currentTag.length-1));
      this.currentTag = '';
    } else if (event.keyCode === 8 && this.currentTag.length == 0){
      this.currentTag = this.newStream.tags.pop();
    }
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
    this.streamsService.getStreamList()
      .subscribe(
        streamList => {
          this.streamList = streamList;
          if (streamList.length > 0) {
            this.currentStream = streamList[0];
          }
        },
        error => this.errorMessage = <any>error);
  }

  public getStreamTypes() {
    this.streamsService.getStreamTypes()
      .subscribe(
        types => this.streamTypes = types,
        error => this.showAlert({ msg: error, type: 'danger', closable: true, timeout: 0 })
      );
  }

  public getServiceList() {
    this.servicesService.getServiceList()
      .subscribe(
        serviceList => this.serviceList = serviceList,
        error => this.errorMessage = <any>error);
  }

  public getService(serviceName: string) {
    this.servicesService.getService(serviceName)
      .subscribe(
        service => this.currentStreamService = service,
        error => this.errorMessage = <any>error);
  }

  public getServiceInfo(Modal: ModalDirective, serviceName: string) {
    this.getService(serviceName);
    Modal.show();
  }

  public deleteStreamConfirm(modal: ModalDirective, stream: StreamModel) {
    this.currentStream = stream;
    this.streamsService.getRelatedInstancesList(stream.name)
      .subscribe(response => this.blockingInstances = response);
    modal.show();
  }

  public deleteStream(modal: ModalDirective) {
    this.streamsService.deleteStream(this.currentStream)
      .subscribe(
        status => {
          this.showAlert({ msg: status, type: 'success', closable: true, timeout: 3000 });
          this.getStreamList();
        },
        error => this.showAlert({ msg: error, type: 'danger', closable: true, timeout: 0 }));
    modal.hide();
  }

  public createStream(modal: ModalDirective) {
    this.showSpinner = true;
    if (this.newStream['stream-type'] !== 'stream.t-stream') {
      delete this.newStream.generator;
    }
    this.streamsService.saveStream(this.newStream)
      .subscribe(
        status => {
          modal.hide();
          this.showSpinner = false;
          this.showAlert({ msg: status, type: 'success', closable: true, timeout: 3000 });
          this.getStreamList();
          this.newStream = new StreamModel();
          this.newStream.tags = [];
          this.newStream.generator = {
            'generator-type': 'local',
            service: '',
            'instance-count': 0
          };
        },
        error => {
          modal.hide();
          this.showSpinner = false;
          this.showAlert({ msg: error, type: 'danger', closable: true, timeout: 0 });
        });
  }

  public selectStream(stream: StreamModel) {
    this.currentStream = stream;
  }

  public isSelected(stream: StreamModel) {
    return stream === this.currentStream;
  }

  public closeAlert(i: number): void {
    this.alerts.splice(i, 1);
  }

  public showAlert(message: Object): void {
    this.alerts = [];
    this.alerts.push(message);
  }
}
