import { Injectable } from '@angular/core';
import { Http, Response, Headers, RequestOptions } from '@angular/http';
import { Observable } from 'rxjs/Rx';

import { ProviderModel } from '../models/provider.model';
import { BaseResponse } from '../models/base-response.model';


@Injectable()
export class ProvidersService {
  private dataUrl = '/v1/';

  constructor(private http: Http) { }

  public getProviderList(): Observable<ProviderModel[]> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({ headers: headers });
    return this.http.get(this.dataUrl + 'providers', options)
      .map(response => {
        const data = this.extractData(response);
        return data.providers;
      })
      .catch(this.handleError);
  }

  public getProviderTypes(): Observable<string[]> {
    return this.http.get(this.dataUrl + 'providers/_types')
      .map(response => {
        const data = this.extractData(response);
        return data.types;
      })
      .catch(this.handleError);
  }

  public getRelatedServicesList(providerName: string): Observable<string[]> {
    return this.http.get(this.dataUrl + 'providers/' + providerName + '/related')
      .map(response => {
        const data = this.extractData(response);
        return data.services;
      })
      .catch(this.handleError);
  }

  public getProvider(providerName: string): Observable<ProviderModel> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({ headers: headers });
    return this.http.get(this.dataUrl + 'providers/' + providerName, options)
      .map(response => {
        const data = this.extractData(response);
        return data.providers;
      })
      .catch(this.handleError);
  }

  public testConnection(provider: ProviderModel): Observable<Boolean> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({ headers: headers });
    return this.http.get(this.dataUrl + 'providers/' + provider.name + '/connection', options)
      .map(response => {
        const data = this.extractData(response);
        return data.connection;
      })
      .catch(this.handleError);
  }

  public deleteProvider(provider: ProviderModel): Observable<ProviderModel> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({ headers: headers });
    return this.http.delete(this.dataUrl + 'providers/' + provider.name, options)
      .map(response => {
        const data = this.extractData(response);
        return data.message;
      })
      .catch(this.handleError);
  }

  public saveProvider(provider: ProviderModel): Observable<ProviderModel> {
    let body = JSON.stringify(provider);
    let headers = new Headers({ 'Content-Type': 'application/json' });
    let options = new RequestOptions({ headers: headers });
    return this.http.post(this.dataUrl + 'providers', body, options)
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
    let errMsgYo = errMsg.entity.message ? errMsg.entity.message : errMsg.entity.errors ;
    return Observable.throw(errMsgYo);
  }
}
