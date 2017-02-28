import { Injectable } from '@angular/core';
import { Headers, RequestOptions } from '@angular/http';
import { Observable } from 'rxjs/Rx';

import { SettingModel, BaseModel } from '../models/index';
import { BaseService, BService, IResponse } from './index';

@Injectable()
@BService({
  entity: 'config/settings',
  entityModel: SettingModel
})
export class ConfigSettingsService extends BaseService<SettingModel> {

  public getConfigSettingsDomains(): Observable<string[]> {
    return this.http.get(this.requestUrl + '/domains')
      .map(response => {
        const data = this.extractData(response);
        return data.domains;
      })
      .catch(this.handleError);
  }

  public deleteSetting(setting: SettingModel): Observable<IResponse<BaseModel>> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({ headers: headers });
    return this.http.delete(this.requestUrl + '/' + setting.domain + '/' + setting.name, options)
      .map(response => {
        const data = this.extractData(response);
        return data;
      })
      .catch(this.handleError);
  }
}
