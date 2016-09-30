import {
    CORE_DIRECTIVES,
    FormBuilder,
    //ControlGroup,
    //Validators
} from '@angular/common';
import {FORM_DIRECTIVES, REACTIVE_FORM_DIRECTIVES} from '@angular/forms';
import { Component, OnInit } from '@angular/core';
import { MODAL_DIRECTIVES, BS_VIEW_PROVIDERS, AlertComponent } from 'ng2-bootstrap/ng2-bootstrap';
//import {NameListService} from '../../shared/index';
import { ListFilter } from '../shared/listFilter/list-filter';
import { OrderBy } from '../shared/orderBy/orderBy-pipe';
import { ServiceFilter } from '../shared/service-filter';
import 'rxjs/Rx';
//import { TagInput } from 'ng2-tag-input';
import { SearchBoxComponent } from '../shared/searchBox/search-box';
import { Stream } from './stream';
import { StreamService } from './stream.service';
import { Service } from '../services/service';
import { ServiceService } from '../services/service.service';
import {ModalDirective} from 'ng2-bootstrap/ng2-bootstrap';

@Component({
  moduleId: module.id,
  selector: 'sd-streams',
  templateUrl: 'streams.component.html',
  styleUrls: ['streams.component.css'],
  directives: [ CORE_DIRECTIVES, MODAL_DIRECTIVES, FORM_DIRECTIVES,
    REACTIVE_FORM_DIRECTIVES, SearchBoxComponent, AlertComponent], //, TagInput
  viewProviders: [BS_VIEW_PROVIDERS],
  pipes: [ServiceFilter, ListFilter, OrderBy],
  providers: [ServiceService, StreamService]
})


export class StreamsComponent implements OnInit {
  errorMessage:string;
  public alerts:Array<Object> = [];
  streamList:Stream[];
  serviceList: Service[];
  current_stream: Stream;
  current_stream_service: Service;
  stream_to_delete: Stream;
  new_stream: Stream;
  //streamForm: ControlGroup;

  constructor(
      private streamService:StreamService,
      private serviceService: ServiceService,
      private fb:FormBuilder
  ) {}

  ngOnInit() {
    this.getStreamList();
    this.getServiceList();
    this.new_stream = new Stream();
    this.new_stream.generator =  {
      'generator-type': '',
      service: '',
      'instance-count': 0
    };
    //this.streamForm = this.fb.group({
    //  //firstName: ['', Validators.required],
    //  //lastName: ['', Validators.required],
    //  //email: ['', Validators.compose([Validators.required])],
    //  //phone: ['', Validators.required],
    //});
  }

  getStreamList() {
    this.streamService.getStreamList()
        .subscribe(
            streamList => { this.streamList = streamList;
              if (streamList.length > 0) {
                this.current_stream = streamList[0];
              }
            },
            error => this.errorMessage = <any>error);
  }

  getServiceList() {
    this.serviceService.getServiceList()
        .subscribe(
            serviceList => this.serviceList = serviceList,
            error => this.errorMessage = <any>error);
  }

  getService(serviceName: string) {
    this.serviceService.getService(serviceName)
        .subscribe(
            service => this.current_stream_service = service,
            error => this.errorMessage = <any>error);
  }
  get_service_info(Modal:ModalDirective, serviceName: string) {
    this.getService(serviceName);
    Modal.show();
  }
  delete_stream_confirm(modal:ModalDirective, stream:Stream) {
    this.stream_to_delete = stream;
    modal.show();
  }
  delete_stream(modal:ModalDirective, stream: Stream) {
    this.streamService.deleteStream(stream)
        .subscribe(
            status => {
              this.alerts.push({msg: status, type: 'success', closable: true});
              this.getStreamList();
            },
            error => this.alerts.push({msg: error, type: 'danger', closable: true, timeout:0}));
    this.stream_to_delete = null;
    modal.hide();
  }
  createStream(modal:ModalDirective) {
    console.log(this.new_stream);
    if (this.new_stream['stream-type'] !== 'stream.t-stream') {
      delete this.new_stream.generator;
    }
    this.streamService.saveStream(this.new_stream)
        .subscribe(
            status => {
              modal.hide();
              this.alerts.push({msg: status, type: 'success', closable: true, timeout:3000});
              this.getStreamList();
            },
            error => this.alerts.push({msg: error, type: 'danger', closable: true, timeout:0}));
  }
  stream_select(stream: Stream) {
    this.current_stream = stream;
  }

  isSelected(stream : Stream) {
    return stream === this.current_stream;
  }
  closeAlert(i:number):void {
    this.alerts.splice(i, 1);
  }
}
