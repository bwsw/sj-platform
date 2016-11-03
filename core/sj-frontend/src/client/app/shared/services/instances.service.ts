import { Injectable } from '@angular/core';
import { Http, Response, Headers, RequestOptions } from '@angular/http';
import { Observable } from 'rxjs/Rx';

import {
  InstanceModel, SubtypedInstance, RegularStreamingInstance, OutputStreamingInstance, InputStreamingInstance
} from '../models/instance.model';

@Injectable()
export class InstancesService {
  private _dataUrl = '/v1/';

  private static fillInstanceGeneralFields(originalInstance: InstanceModel, instance: SubtypedInstance) {
    instance['name'] = originalInstance['name'];
    instance['description'] = originalInstance['description'];
    // Checking if string 'max' or numeric is passed
    instance['parallelism'] = /^\+?(0|[1-9]\d*)$/.test(originalInstance['parallelism'])
      ? parseInt(originalInstance['parallelism'])
      : originalInstance['parallelism'];
    instance['options'] = originalInstance['options'];
    instance['per-task-cores'] = originalInstance['per-task-cores'];
    instance['per-task-ram'] = originalInstance['per-task-ram'];
    instance['jvm-options'] = originalInstance['jvm-options'];
    instance['node-attributes'] = originalInstance['node-attributes'];
    instance['coordination-service'] = originalInstance['coordination-service'];
    instance['environment-variables'] = originalInstance['environment-variables'];
    instance['performance-reporting-interval'] = originalInstance['performance-reporting-interval'];

    return instance;
  }

  constructor(private _http: Http) {
  }

  public getInstanceList(): Observable<InstanceModel[]> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({ headers: headers });
    return this._http.get(this._dataUrl + '/modules/instances', options)
      .map(this.extractData)
      .catch(this.handleError);
  }

  public getInstanceInfo(instance: InstanceModel): Observable<InstanceModel> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({ headers: headers });
    return this._http.get(this._dataUrl + '/modules/' + instance['module-type'] + '/' + instance['module-name'] + '/' +
      instance['module-version'] + '/instance' + '/' + instance['name'], options)
      .map(this.extractData)
      .catch(this.handleError);
  }

  public saveInstance(instance: InstanceModel): Observable<InstanceModel> {
    let subtypedInstance = this.getPreparedInstance(instance);
    let instance_body = Object.assign({}, subtypedInstance);
    let body = JSON.stringify(instance_body);
    let headers = new Headers({'Content-Type': 'application/json'});
    let options = new RequestOptions({ headers: headers });

    return this._http.post(this._dataUrl + 'modules/' + instance.module['module-type'] + '/' + instance.module['module-name'] + '/' +
      instance.module['module-version'] + '/instance', body, options)
      .map(this.extractData)
      .catch(this.handleError);
  }

  public deleteInstance(instance: InstanceModel): Observable<InstanceModel> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({ headers: headers });
    return this._http.delete(this._dataUrl + '/modules/' + instance['module-type'] + '/' + instance['module-name'] + '/' +
      instance['module-version'] + '/instance' + '/' + instance['name'], options)
      .map(this.extractData)
      .catch(this.handleError);
  }

  public startInstance(instance: InstanceModel): Observable<InstanceModel> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({ headers: headers });
    return this._http.get(this._dataUrl + '/modules/' + instance['module-type'] + '/' + instance['module-name'] + '/' +
      instance['module-version'] + '/instance' + '/' + instance['name'] + '/start', options)
      .map(this.extractData)
      .catch(this.handleError);
  }

  public stopInstance(instance: InstanceModel): Observable<InstanceModel> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({ headers: headers });
    return this._http.get(this._dataUrl + '/modules/' + instance['module-type'] + '/' + instance['module-name'] + '/' +
      instance['module-version'] + '/instance' + '/' + instance['name'] + '/stop', options)
      .map(this.extractData)
      .catch(this.handleError);
  }

  private getPreparedInstance(originalInstance: InstanceModel) {
    let inst: SubtypedInstance;

    switch (originalInstance.module['module-type']) {

      case 'regular-streaming':
        inst = new RegularStreamingInstance();
        inst = InstancesService.fillInstanceGeneralFields(originalInstance, inst);
        inst['checkpoint-mode'] = originalInstance['checkpoint-mode'];
        inst['checkpoint-interval'] = originalInstance['checkpoint-interval'];
        inst['event-wait-time'] = originalInstance['event-wait-time'];
        originalInstance['inputs'].forEach(function(item:string, i:number) {
          inst['inputs'][i] = originalInstance['inputs'][i] + '/' + originalInstance['input-type'][i];
        });
        inst['outputs'] = originalInstance['outputs'];
        inst['state-management'] = originalInstance['state-management'];
        inst['start-from'] = originalInstance['start-from'];
        inst['state-full-checkpoint'] = originalInstance['state-full-checkpoint'];

        break;

      // case 'windowed-streaming':
      //   break;

      case 'output-streaming':
        inst = new OutputStreamingInstance();
        inst = InstancesService.fillInstanceGeneralFields(originalInstance, inst);
        inst['checkpoint-mode'] = originalInstance['checkpoint-mode'];
        inst['checkpoint-interval'] = originalInstance['checkpoint-interval'];
        inst['input'] = originalInstance['input'];
        inst['output'] = originalInstance['output'];
        inst['start-from'] = originalInstance['start-from'];
        break;

      case 'input-streaming':
        inst = new InputStreamingInstance();
        inst = InstancesService.fillInstanceGeneralFields(originalInstance, inst);
        inst['async-backup-count'] = originalInstance['async-backup-count'];
        inst['backup-count'] = originalInstance['backup-count'];
        inst['checkpoint-interval'] = originalInstance['checkpoint-interval'];
        inst['checkpoint-mode'] = originalInstance['checkpoint-mode'];
        inst['default-eviction-policy'] = originalInstance['default-eviction-policy'];
        inst['duplicate-check'] = originalInstance['duplicate-check'];
        inst['eviction-policy'] = originalInstance['eviction-policy'];
        inst['lookup-history'] = originalInstance['lookup-history'];
        inst['outputs'] = originalInstance['outputs'];
        inst['queue-max-size'] = originalInstance['queue-max-size'];
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

  private extractData(res: Response) { //TODO Write good response parser
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

  private handleError(error: any) {
    let errMsg = (error._body) ? error._body :
      error.status ? `${error.status} - ${error.statusText}` : 'Server error';
    errMsg = JSON.parse(errMsg);
    let errMsgYo = errMsg.entity.message;
    return Observable.throw(errMsgYo);
  }
}
