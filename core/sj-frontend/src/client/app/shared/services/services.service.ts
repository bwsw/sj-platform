import { Injectable } from '@angular/core';
import { Http, Response, Headers, RequestOptions } from '@angular/http';
import { Observable } from 'rxjs/Rx';

import { ServiceModel } from '../models/service.model';
import { BaseResponse } from '../models/base-response.model';

interface ISomeObject {
  [key: string]: string[];
}

@Injectable()
export class ServicesService {
  private dataUrl = '/v1/';

  constructor(private http: Http) { }

  public getServiceList(): Observable<ServiceModel[]> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({ headers: headers });
    return this.http.get(this.dataUrl + 'services', options)
      .map(response => {
        const data = this.extractData(response);
        return data.services;
      })
      .catch(this.handleError);
  }

  public getServiceTypes(): Observable<string[]> {
    return this.http.get(this.dataUrl + 'services/types')
      .map(response => {
        const data = this.extractData(response);
        return data.types;
      })
      .catch(this.handleError);
  }

  public getRelatedStreamsList(serviceName: string): Observable<ISomeObject> {
    return this.http.get(this.dataUrl + 'services/' + serviceName + '/related')
      .map(response => {
        const data = this.extractData(response);
        return data;
      })
      .catch(this.handleError);
  }

  public getService(serviceName: string): Observable<ServiceModel> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({ headers: headers });
    return this.http.get(this.dataUrl + 'services/' + serviceName, options)
      .map(response => {
        const data = this.extractData(response);
        return data.services;
      })
      .catch(this.handleError);
  }

  public deleteService(service: ServiceModel): Observable<ServiceModel> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({ headers: headers });
    return this.http.delete(this.dataUrl + 'services/' + service.name, options)
      .map(response => {
        const data = this.extractData(response);
        return data.message;
      })
      .catch(this.handleError);
  }

  public saveService(service: ServiceModel): Observable<ServiceModel> {
    let body = JSON.stringify(service);
    let headers = new Headers({ 'Content-Type': 'application/json' });
    let options = new RequestOptions({ headers: headers });
    return this.http.post(this.dataUrl + 'services', body, options)
      .map(response => {
        const data = this.extractData(response);
        return data.message;
      })
      .catch(this.handleError);
  }

  private extractData(res: Response) {
    let body = new BaseResponse();
    body.fillFromJSON(res.json()['entity']);
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
