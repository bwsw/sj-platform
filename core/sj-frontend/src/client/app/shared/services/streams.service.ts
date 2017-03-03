import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Rx';

import { StreamModel } from '../models/index';
import { BaseService, BService, IRelatedObject } from './index';

@Injectable()
@BService({
  entity: 'streams',
  entityModel: StreamModel
})
export class StreamsService extends BaseService<StreamModel> {

  public getRelatedList(name: string): Observable<IRelatedObject> {
    return this.http
      .get(`${this.requestUrl}/${name}` + '/related', this.getRequestOptions())
      .map(this.extractData)
      .catch(this.handleError);
  }
}
