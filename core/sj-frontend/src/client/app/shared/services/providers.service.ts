import { Injectable } from '@angular/core';
import { Headers, RequestOptions } from '@angular/http';
import { Observable } from 'rxjs/Rx';

import { ProviderModel } from '../models/index';
import { BaseService, BService, IRelatedObject } from './index';

@Injectable()
@BService({
  entity: 'providers',
  entityModel: ProviderModel
})
export class ProvidersService extends BaseService<ProviderModel> {

  public testConnection(provider: ProviderModel): Observable<Boolean> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({ headers: headers });
    return this.http.get(this.requestUrl + '/' + provider.name + '/connection', options)
      .map(response => {
        const data = this.extractData(response);
        return data.connection;
      })
      .catch(this.handleError);
  }

  public getRelatedList(name: string): Observable<IRelatedObject> {
    return this.http
        .get(`${this.requestUrl}/${name}` + '/related', this.getRequestOptions())
        .map(this.extractData)
        .catch(this.handleError);
  }
}
