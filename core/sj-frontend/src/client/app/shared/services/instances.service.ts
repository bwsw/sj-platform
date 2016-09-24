import { Injectable } from '@angular/core';
import { Http, Response, Headers, RequestOptions } from '@angular/http';
import { Observable } from 'rxjs/Rx';

import { InstanceModel } from '../models/instance.model';

@Injectable()
export class InstancesService {
  private _dataUrl = '/v1/';

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
    let instance_body = Object.assign({}, instance);
    if (instance_body.module['module-type'] === 'regular-streaming') {
      instance_body['inputs'].forEach(function (item: string, i: number) {
        instance_body['inputs'][i] = instance_body['inputs'][i] + '/' + instance_body['input-type'][i];
      });
      delete instance_body['input-type'];
    }
    delete instance_body.module;
    //delete instance_body.outputs;
    //delete instance_body.inputs;
    //instance_body.options = JSON.stringify(instance.options);
    instance_body.options = JSON.parse(instance_body.options.toString());
    instance_body['jvm-options'] = JSON.parse(instance_body['jvm-options'].toString());
    //instance_body['node-attributes'] = JSON.parse(instance_body['node-attributes']);
    //instance_body['environment-variables'] = JSON.parse(instance_body['environment-variables']);
    console.log(instance_body);
    let body = JSON.stringify(instance_body);
    let headers = new Headers({ 'Content-Type': 'application/json' });
    let options = new RequestOptions({ headers: headers });
    return this._http.post(this._dataUrl + '/modules/' + instance.module['module-type'] + '/' + instance.module['module-name'] + '/' +
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
