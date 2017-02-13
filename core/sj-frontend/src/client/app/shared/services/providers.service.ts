import { Injectable } from '@angular/core';
import { Http, Response, Headers, RequestOptions } from '@angular/http';
import { Observable } from 'rxjs/Rx';

import { ProviderModel } from '../models/provider.model';
import { BaseModel } from '../models/base.model';
import { BaseService, BService } from './base.service';

@Injectable()
@BService({
  entity: 'providers',
  requestPath: '',
  entityModel: ProviderModel
})
export class ProvidersService extends BaseService<ProviderModel> {
  private dataUrl = '/v1/';

  public getRelatedServicesList(providerName: string): Observable<string[]> {
    return this.http.get(this.dataUrl + 'providers/' + providerName + '/related')
      .map(response => {
        const data = this.extractData(response);
        return data.services;
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
}
