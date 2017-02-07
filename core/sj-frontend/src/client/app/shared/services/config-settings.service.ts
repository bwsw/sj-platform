import { Injectable } from '@angular/core';
import { Http, Response, Headers, RequestOptions } from '@angular/http';
import { Observable } from 'rxjs/Rx';

import { SettingModel } from '../models/setting.model';
import { BaseResponse } from '../models/base-response.model';

@Injectable()
export class ConfigSettingsService {
  private _dataUrl = '/v1/';

  constructor(private http: Http) {}

  public getConfigSettingsList(): Observable<SettingModel[]> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({ headers: headers });
    return this.http.get(this._dataUrl + 'config/settings', options)
      .map(response => {
        const data = this.extractData(response);
        return data.configSettings;
      })
      .catch(this.handleError);
  }

  public getConfigSettingsDomains(): Observable<string[]> {
    return this.http.get(this._dataUrl + 'config/settings/domains')
      .map(response => {
        const data = this.extractData(response);
        return data.domains;
      })
      .catch(this.handleError);
  }

  public saveSetting(setting: SettingModel): Observable<SettingModel> {
    let body = JSON.stringify(setting);
    let headers = new Headers({ 'Content-Type': 'application/json' });
    let options = new RequestOptions({ headers: headers });
    return this.http.post(this._dataUrl + 'config/settings', body, options)
      .map(response => {
        const data = this.extractData(response);
        return data.message;
      })
      .catch(this.handleError);
  }

  public deleteSetting(setting: SettingModel): Observable<SettingModel> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({ headers: headers });
    return this.http.delete(this._dataUrl + 'config/settings/' + setting.domain + '/' + setting.name, options)
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
