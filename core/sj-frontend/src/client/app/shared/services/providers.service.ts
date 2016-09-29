import { Injectable } from '@angular/core';
import { Http, Response, Headers, RequestOptions } from '@angular/http';
import { Observable } from 'rxjs/Rx';

import { ProviderModel } from '../models/provider.model';

@Injectable()
export class ProvidersService {
  private _dataUrl = '/v1/';

  constructor(private _http: Http) {
  }

  public getProviderList(): Observable<ProviderModel[]> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({ headers: headers });
    return this._http.get(this._dataUrl + 'providers', options)
      .map(this.extractData)
      .catch(this.handleError);
  }

  public getProvider(providerName: string): Observable<ProviderModel> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({ headers: headers });
    return this._http.get(this._dataUrl + 'providers/' + providerName, options)
      .map(this.extractData)
      .catch(this.handleError);
  }

  public testConnection(provider: ProviderModel): Observable<Boolean> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({ headers: headers });
    return this._http.get(this._dataUrl + 'providers/' + provider.name + '/connection', options)
      .map(this.extractData)
      .catch(this.handleError);
  }

  public deleteProvider(provider: ProviderModel): Observable<ProviderModel> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({ headers: headers });
    return this._http.delete(this._dataUrl + 'providers/' + provider.name, options)
      .map(this.extractData)
      .catch(this.handleError);
  }

  public saveProvider(provider: ProviderModel): Observable<ProviderModel> {
    let body = JSON.stringify(provider);
    let headers = new Headers({ 'Content-Type': 'application/json' });
    let options = new RequestOptions({ headers: headers });
    return this._http.post(this._dataUrl + 'providers', body, options)
      .map(this.extractData)
      .catch(this.handleError);
  }

  private extractData(res: Response) { //TODO Write good response parser
    let body = {};
    if (typeof res.json()['entity']['connection'] !== 'undefined') {
      body = res.json()['entity']['connection'];
    } else if (typeof res.json()['entity']['message'] !== 'undefined') {
      body = res.json()['entity']['message'];
    } else {
      if (typeof res.json()['id'] === 'undefined') {
        body = res.json()['entity']['providers'];
      } else {
        body = res.json();
      }
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
