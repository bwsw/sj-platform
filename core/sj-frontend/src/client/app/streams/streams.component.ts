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
  public current_stream: StreamModel;
  public current_stream_service: ServiceModel;
  public stream_to_delete: StreamModel;
  public new_stream: StreamModel;

  constructor(private _streamsService: StreamsService,
              private _servicesService: ServicesService) {
  }

  public ngOnInit() {
    this.getStreamList();
    this.getServiceList();
    this.new_stream = new StreamModel();
    this.new_stream.generator = {
      'generator-type': '',
      service: '',
      'instance-count': 0
    };
  }

  public getStreamList() {
    this._streamsService.getStreamList()
      .subscribe(
        streamList => {
          this.streamList = streamList;
          if (streamList.length > 0) {
            this.current_stream = streamList[0];
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

  public get_service_info(Modal: ModalDirective, serviceName: string) {
    this.getService(serviceName);
    Modal.show();
  }

  public delete_stream_confirm(modal: ModalDirective, stream: StreamModel) {
    this.stream_to_delete = stream;
    modal.show();
  }

  public delete_stream(modal: ModalDirective, stream: StreamModel) {
    this._streamsService.deleteStream(stream)
      .subscribe(
        status => {
          this.alerts.push({ msg: status, type: 'success', closable: true });
          this.getStreamList();
        },
        error => this.alerts.push({ msg: error, type: 'danger', closable: true, timeout: 0 }));
    this.stream_to_delete = null;
    modal.hide();
  }

  public createStream(modal: ModalDirective) {
    console.log(this.new_stream);
    if (this.new_stream['stream-type'] !== 'stream.t-stream') {
      delete this.new_stream.generator;
    }
    this._streamsService.saveStream(this.new_stream)
      .subscribe(
        status => {
          modal.hide();
          this.alerts.push({ msg: status, type: 'success', closable: true, timeout: 3000 });
          this.getStreamList();
        },
        error => this.alerts.push({ msg: error, type: 'danger', closable: true, timeout: 0 }));
  }

  public stream_select(stream: StreamModel) {
    this.current_stream = stream;
  }

  public isSelected(stream: StreamModel) {
    return stream === this.current_stream;
  }

  public closeAlert(i: number): void {
    this.alerts.splice(i, 1);
  }
}
