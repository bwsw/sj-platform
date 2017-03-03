import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Rx';
import { ServiceModel } from '../models/index';
import { BaseService, BService, IRelatedObject } from './index';

@Injectable()
@BService({
  entity: 'services',
  entityModel: ServiceModel
})
export class ServicesService extends BaseService<ServiceModel> {

  public getRelatedList(name: string): Observable<IRelatedObject> {
    return this.http
      .get(`${this.requestUrl}/${name}` + '/related', this.getRequestOptions())
      .map(this.extractData)
      .catch(this.handleError);
  }

}
