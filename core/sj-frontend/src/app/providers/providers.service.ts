import { Injectable } from '@angular/core';
import { Headers, RequestOptions } from '@angular/http';
import { Observable } from 'rxjs/Rx';

import { ProviderModel } from './provider.model';
import { CrudService, BService, IRelatedObject } from '../shared';

@Injectable()
@BService({
  entity: 'providers',
  entityModel: ProviderModel
})
export class ProvidersService extends CrudService<ProviderModel> {

  public testConnection(provider: ProviderModel): Observable<Boolean> {
    const headers = new Headers();
    headers.append('Content-Type', 'application/json');
    const options = new RequestOptions({ headers: headers });
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
