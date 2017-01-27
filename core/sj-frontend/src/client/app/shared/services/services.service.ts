import { Injectable } from '@angular/core';
import { Http, Response, Headers, RequestOptions } from '@angular/http';
import { Observable } from 'rxjs/Rx';

import { ServiceModel } from '../models/service.model';

interface ISomeObject {
  [key: string]: string[];
}

@Injectable()
export class ServicesService {

  private _dataUrl = '/v1/';

  constructor(private _http: Http) {
  }

  public getServiceList(): Observable<ServiceModel[]> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({ headers: headers });
    return this._http.get(this._dataUrl + 'services', options)
      .map(this.extractData)
      .catch(this.handleError);
  }

  public getRelatedStreamsList(serviceName: string): Observable<ISomeObject> {
    return this._http.get(this._dataUrl + 'services/' + serviceName + '/related')
      .map(this.extractData)
      .catch(this.handleError);
  }

  public getService(serviceName: string): Observable<ServiceModel> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({ headers: headers });
    return this._http.get(this._dataUrl + 'services/' + serviceName, options)
      .map(this.extractData)
      .catch(this.handleError);
  }

  public deleteService(service: ServiceModel): Observable<ServiceModel> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({ headers: headers });
    return this._http.delete(this._dataUrl + 'services/' + service.name, options)
      .map(this.extractData)
      .catch(this.handleError);
  }

  public saveService(service: ServiceModel): Observable<ServiceModel> {
    let body = JSON.stringify(service);
    let headers = new Headers({ 'Content-Type': 'application/json' });
    let options = new RequestOptions({ headers: headers });
    return this._http.post(this._dataUrl + 'services', body, options)
      .map(this.extractData)
      .catch(this.handleError);
  }

  private extractData(res: Response) { //TODO Write good response parser
    let body = {};
    if (typeof res.json()['entity']['services'] !== 'undefined') {
      body = res.json()['entity']['services'];
    } else if (typeof res.json()['entity']['message'] !== 'undefined') {
      body = res.json()['entity']['message'];
    } else if (typeof res.json()['entity']['streams'] !== 'undefined' && typeof res.json()['entity']['instances'] !== 'undefined') {
      body = res.json()['entity'];
    } else {
      body = res.json();
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
