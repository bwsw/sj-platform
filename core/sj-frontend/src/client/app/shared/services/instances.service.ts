import { Injectable } from '@angular/core';
import { Http, Response, Headers, RequestOptions } from '@angular/http';
import { Observable } from 'rxjs/Rx';

import {
  InstanceModel,
  SubtypedInstance,
  RegularStreamingInstance,
  OutputStreamingInstance,
  InputStreamingInstance,
  WindowedStreamingInstance
} from '../models/instance.model';

@Injectable()
export class InstancesService {
  private _dataUrl = '/v1/';

  private static fillInstanceGeneralFields(orig: InstanceModel, instance: SubtypedInstance) {
    instance['name'] = orig['name'];
    instance['description'] = orig['description'];
    // Checking if string 'max' or numeric is passed
    instance['parallelism'] = /^\+?(0|[1-9]\d*)$/.test(orig['parallelism'])
      ? parseInt(orig['parallelism'])
      : orig['parallelism'];
    instance['options'] = orig['options'];
    instance['per-task-cores'] = orig['per-task-cores'];
    instance['per-task-ram'] = orig['per-task-ram'];
    instance['jvm-options'] = orig['jvm-options'];
    instance['node-attributes'] = orig['node-attributes'];
    instance['coordination-service'] = orig['coordination-service'];
    instance['environment-variables'] = orig['environment-variables'];
    instance['performance-reporting-interval'] = orig['performance-reporting-interval'];

    return instance;
  }

  constructor(private _http: Http) {
  }

  public getInstanceList(): Observable<InstanceModel[]> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({ headers: headers });
    return this._http.get(this._dataUrl + 'modules/instances', options)
      .map(this._extractData)
      .catch(this._handleError);
  }

  public getInstanceInfo(instance: InstanceModel): Observable<InstanceModel> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({ headers: headers });
    return this._http.get(this._dataUrl + 'modules/' + instance['module-type'] + '/' + instance['module-name'] + '/' +
      instance['module-version'] + '/instance' + '/' + instance['name'], options)
      .map(this._extractData)
      .catch(this._handleError);
  }

  public saveInstance(instance: InstanceModel): Observable<InstanceModel> {
    let subtypedInstance = this.getPreparedInstance(instance);
    let instance_body = Object.assign({}, subtypedInstance);
    let body = JSON.stringify(instance_body, this._cleanupBodyValues);
    let headers = new Headers({'Content-Type': 'application/json'});
    let options = new RequestOptions({ headers: headers });

    return this._http.post(this._dataUrl + 'modules/' + instance.module['module-type'] + '/' + instance.module['module-name'] + '/' +
      instance.module['module-version'] + '/instance', body, options)
      .map(this._extractData)
      .catch(this._handleError);
  }

  public deleteInstance(instance: InstanceModel): Observable<InstanceModel> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({ headers: headers });
    return this._http.delete(this._dataUrl + 'modules/' + instance['module-type'] + '/' + instance['module-name'] + '/' +
      instance['module-version'] + '/instance' + '/' + instance['name'], options)
      .map(this._extractData)
      .catch(this._handleError);
  }

  public startInstance(instance: InstanceModel): Observable<InstanceModel> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({ headers: headers });
    return this._http.get(this._dataUrl + 'modules/' + instance['module-type'] + '/' + instance['module-name'] + '/' +
      instance['module-version'] + '/instance' + '/' + instance['name'] + '/start', options)
      .map(this._extractData)
      .catch(this._handleError);
  }

  public stopInstance(instance: InstanceModel): Observable<InstanceModel> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({ headers: headers });
    return this._http.get(this._dataUrl + 'modules/' + instance['module-type'] + '/' + instance['module-name'] + '/' +
      instance['module-version'] + '/instance' + '/' + instance['name'] + '/stop', options)
      .map(this._extractData)
      .catch(this._handleError);
  }

  private getPreparedInstance(orig: InstanceModel) {
    let inst: SubtypedInstance;

    switch (orig.module['module-type']) {

      case 'regular-streaming':
        inst = new RegularStreamingInstance();
        inst = InstancesService.fillInstanceGeneralFields(orig, inst);
        inst['checkpoint-mode'] = orig['checkpoint-mode'];
        inst['checkpoint-interval'] = orig['checkpoint-interval'];
        inst['event-wait-time'] = orig['event-wait-time'];
        orig['inputs'].forEach(function(item:string, i:number) {
          inst['inputs'][i] = orig['inputs'][i] + '/' + orig['inputs-types'][i];
        });
        inst['outputs'] = orig['outputs'];
        inst['state-management'] = orig['state-management'];
        inst['start-from'] = orig['start-from'] === 'timestamp' ? orig['start-from-timestamp'] : orig['start-from'];
        inst['state-full-checkpoint'] = orig['state-full-checkpoint'];

        break;

      case 'windowed-streaming':
        inst = new WindowedStreamingInstance();
        inst = InstancesService.fillInstanceGeneralFields(orig, inst);
        inst['outputs'] = orig['outputs'];
        inst['state-management'] = orig['state-management'];
        inst['window'] = orig['window'];
        inst['related-streams'] = orig['related-streams'];
        inst['state-full-checkpoint'] = orig['state-full-checkpoint'];
        inst['event-wait-time'] = orig['event-wait-time'];
        inst['main-stream'] = orig['main-stream'];
        inst['batch-fill-type'] = orig['batch-fill-type'];
        inst['sliding-interval'] = orig['sliding-interval'];
        inst['start-from'] = orig['start-from'] === 'timestamp' ? orig['start-from-timestamp'] : orig['start-from'];
        break;

      case 'output-streaming':
        inst = new OutputStreamingInstance();
        inst = InstancesService.fillInstanceGeneralFields(orig, inst);
        inst['checkpoint-mode'] = orig['checkpoint-mode'];
        inst['checkpoint-interval'] = orig['checkpoint-interval'];
        inst['input'] = orig['input'];
        inst['output'] = orig['output'];
        inst['start-from'] = orig['start-from'] === 'timestamp' ? orig['start-from-datetime'] : orig['start-from'];
        break;

      case 'input-streaming':
        inst = new InputStreamingInstance();
        inst = InstancesService.fillInstanceGeneralFields(orig, inst);
        inst['async-backup-count'] = orig['async-backup-count'];
        inst['backup-count'] = orig['backup-count'];
        inst['checkpoint-interval'] = orig['checkpoint-interval'];
        inst['checkpoint-mode'] = orig['checkpoint-mode'];
        inst['default-eviction-policy'] = orig['default-eviction-policy'];
        inst['duplicate-check'] = orig['duplicate-check'];
        inst['eviction-policy'] = orig['eviction-policy'];
        inst['lookup-history'] = orig['lookup-history'];
        inst['outputs'] = orig['outputs'];
        inst['queue-max-size'] = orig['queue-max-size'];
        break;
    }

    let objectFields = ['environment-variables', 'jvm-options', 'node-attributes', 'options'];
    for (let fieldName of objectFields) {
      if (inst[fieldName] !== undefined) {
        inst[fieldName] = inst[fieldName].length === 0 ? '{}' : inst[fieldName];
        inst[fieldName] = JSON.parse(inst[fieldName].toString());
      }
    }

    return inst;
  }

  private _extractData(res: Response) { //TODO Write good response parser
    let body = {};
    if (typeof res.json()['entity']['instances'] !== 'undefined') {
      body = res.json()['entity']['instances'];
    } else if (typeof res.json()['entity']['message'] !== 'undefined') {
      body = res.json()['entity']['message'];
    } else {
      body = res.json()['entity']['instance'];
    }
    return body;
  }

  private _handleError(error: any) {
    let errMsg = (error._body) ? error._body :
      error.status ? `${error.status} - ${error.statusText}` : 'Server error';
    errMsg = JSON.parse(errMsg);
    let errMsgYo = errMsg.entity.message;
    return Observable.throw(errMsgYo);
  }


  private _cleanupBodyValues(key: string, value: any): any {
    if ( [null, ''].indexOf(value) > -1 ) {
      return undefined;
    } else {
      return value;
    }
  }


}
