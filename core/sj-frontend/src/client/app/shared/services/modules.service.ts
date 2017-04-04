import { Injectable } from '@angular/core';
import { Headers, RequestOptions } from '@angular/http';
import { Observable } from 'rxjs/Rx';

import { ModuleModel } from '../models/index';
import { CrudFileService, BService, IRelatedObject } from './index';

@Injectable()
@BService({
  entity: 'modules',
  entityModel: ModuleModel
})
export class ModulesService extends CrudFileService<ModuleModel> {

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

  public getRelatedList(name: string, type: string, version: string): Observable<IRelatedObject> {
    return this.http
        .get(`${this.requestUrl}/${type}/${name}/${version}` + '/related', this.getRequestOptions())
        .map(this.extractData)
        .catch(this.handleError);
  }
}
