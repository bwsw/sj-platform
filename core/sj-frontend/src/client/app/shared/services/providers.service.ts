import { Injectable } from '@angular/core';
import { Http, Response, Headers, RequestOptions } from '@angular/http';
import { Observable } from 'rxjs/Rx';

import { ProviderModel } from '../models/provider.model';
import { BaseService, BService } from './base.service';

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
}
