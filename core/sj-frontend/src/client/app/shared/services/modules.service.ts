import { Injectable } from '@angular/core';
import { Headers, RequestOptions } from '@angular/http';
import { Observable } from 'rxjs/Rx';

import { ModuleModel } from '../models';
import { BaseService, BService } from './';

@Injectable()
@BService({
  entity: 'modules',
  entityModel: ModuleModel
})
export class ModulesService extends BaseService<ModuleModel> {

  public getModuleSpecification(module: ModuleModel): Observable<ModuleModel> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({ headers: headers });
    return this.http.get(this.requestUrl + '/' + module.moduleType + '/' + module.moduleName + '/' +
      module.moduleVersion + '/specification', options)
      .map(response => {
        const data = this.extractData(response);
        return data.specification;
      })
      .catch(this.handleError);
  }
}
