import { Injectable } from '@angular/core';
import { Observable } from 'rxjs/Rx';
import { CrudService } from '../shared';
import { ServiceModel } from './service.model';
import { IRelatedObject, BService } from '../shared/service/base.service';

@Injectable()
@BService({
  entity: 'services',
  entityModel: ServiceModel
})
export class ServicesService extends CrudService<ServiceModel> {

  public getRelatedList(name: string): Observable<IRelatedObject> {
    return this.http
      .get(`${this.requestUrl}/${name}` + '/related', this.getRequestOptions())
      .map(this.extractData)
      .catch(this.handleError);
  }

}
