import { Injectable } from '@angular/core';
import { Headers, RequestOptions } from '@angular/http';
import { Observable } from 'rxjs/Rx';
import { ConfigSettingModel } from './config-setting.model';
import { BService, IResponse } from '../shared/service/base.service';
import { CrudService } from '../shared/service/crud.service';
import { BaseModel } from '../shared/model/base.model';
import { TypeModel } from '../shared/model/type.model';


@Injectable()
@BService({
  entity: 'config/settings',
  entityModel: ConfigSettingModel
})
export class ConfigSettingsService extends CrudService<ConfigSettingModel> {

  public getConfigSettingsDomains(): Observable<TypeModel[]> {
    return this.http.get(this.requestUrl + '/domains')
      .map(response => {
        const data = this.extractData(response);
        return data.domains;
      })
      .catch(this.handleError);
  }

  public deleteSetting(setting: ConfigSettingModel): Observable<IResponse<BaseModel>> {
    const headers = new Headers();
    headers.append('Content-Type', 'application/json');
    const options = new RequestOptions({ headers: headers });
    return this.http.delete(this.requestUrl + '/' + setting.domain + '/' + setting.name, options)
      .map(response => {
        const data = this.extractData(response);
        return data;
      })
      .catch(this.handleError);
  }
}
