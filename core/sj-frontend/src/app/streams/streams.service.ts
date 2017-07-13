import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Rx';
import { CrudService } from '../shared/service/crud.service';
import { StreamModel } from './stream.model';
import { BService, IRelatedObject } from '../shared/service/base.service';



@Injectable()
@BService({
  entity: 'streams',
  entityModel: StreamModel
})
export class StreamsService extends CrudService<StreamModel> {

  public getRelatedList(name: string): Observable<IRelatedObject> {
    return this.http
      .get(`${this.requestUrl}/${name}` + '/related', this.getRequestOptions())
      .map(this.extractData)
      .catch(this.handleError);
  }
}
