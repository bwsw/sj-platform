import { Injectable } from '@angular/core';
import { Http, Response, Headers, RequestOptions } from '@angular/http';
import { Observable } from 'rxjs/Rx';

import { SettingModel } from '../models/setting.model';

@Injectable()
export class ConfigSettingsService {
  private _dataUrl = '/v1/';

  constructor(private _http: Http) {}

  public getConfigSettingsList(): Observable<SettingModel[]> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({ headers: headers });
    return this._http.get(this._dataUrl + 'config/settings', options)
      .map(this.extractData)
      .catch(this.handleError);
  }

  public saveSetting(setting: SettingModel): Observable<SettingModel> {
    let body = JSON.stringify(setting);
    let headers = new Headers({ 'Content-Type': 'application/json' });
    let options = new RequestOptions({ headers: headers });
    return this._http.post(this._dataUrl + 'config-settings/', body, options)
      .map(this.extractData)
      .catch(this.handleError);
  }

  private extractData(res: Response) { //TODO Write good response parser
    let body = {};
    if (typeof res.json()['entity']['config-settings'] !== 'undefined') {
      body = res.json()['entity']['config-settings'];
    } else if (typeof res.json()['entity']['message'] !== 'undefined') {
      body = res.json()['entity']['message'];
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
