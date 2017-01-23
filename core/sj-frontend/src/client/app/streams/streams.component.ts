import { Component, OnInit } from '@angular/core';
import { ModalDirective } from 'ng2-bootstrap';

import { StreamModel } from '../shared/models/stream.model';
import { ServiceModel } from '../shared/models/service.model';
import { ServicesService } from '../shared/services/services.service';
import { StreamsService } from '../shared/services/streams.service';

@Component({
  moduleId: module.id,
  selector: 'sj-streams',
  templateUrl: 'streams.component.html',
  styleUrls: ['streams.component.css']
})
export class StreamsComponent implements OnInit {
  public errorMessage: string;
  public alerts: Array<Object> = [];
  public streamList: StreamModel[];
  public serviceList: ServiceModel[];
  public currentStream: StreamModel;
  public currentTag: string;
  public current_stream_service: ServiceModel;
  public stream_to_delete: StreamModel;
  public newStream: StreamModel;

  constructor(private streamsService: StreamsService,
              private _servicesService: ServicesService) {
  }

  public ngOnInit() {
    this.getStreamList();
    this.getServiceList();
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

  focus() {
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

  public getServiceList() {
    this._servicesService.getServiceList()
      .subscribe(
        serviceList => this.serviceList = serviceList,
        error => this.errorMessage = <any>error);
  }

  public getService(serviceName: string) {
    this._servicesService.getService(serviceName)
      .subscribe(
        service => this.current_stream_service = service,
        error => this.errorMessage = <any>error);
  }

  public getServiceInfo(Modal: ModalDirective, serviceName: string) {
    this.getService(serviceName);
    Modal.show();
  }

  public deleteStreamConfirm(modal: ModalDirective, stream: StreamModel) {
    this.stream_to_delete = stream;
    modal.show();
  }

  public deleteStream(modal: ModalDirective, stream: StreamModel) {
    this.streamsService.deleteStream(stream)
      .subscribe(
        status => {
          this.alerts.push({ msg: status, type: 'success', closable: true, timeout: 3000 });
          this.getStreamList();
        },
        error => this.alerts.push({ msg: error, type: 'danger', closable: true, timeout: 0 }));
    this.stream_to_delete = null;
    modal.hide();
  }

  public createStream(modal: ModalDirective) {
    console.log(this.newStream);
    if (this.newStream['stream-type'] !== 'stream.t-stream') {
      delete this.newStream.generator;
    }
    this.streamsService.saveStream(this.newStream)
      .subscribe(
        status => {
          modal.hide();
          this.alerts.push({ msg: status, type: 'success', closable: true, timeout: 3000 });
          this.getStreamList();
          this.newStream = new StreamModel();
          this.newStream.tags = [];
        },
        error => {
          modal.hide();
          this.alerts.push({ msg: error, type: 'danger', closable: true, timeout: 0 });
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
}
