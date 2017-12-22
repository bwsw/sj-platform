import { Injectable } from '@angular/core';
import { Headers, RequestOptions } from '@angular/http';
import { Observable } from 'rxjs/Rx';
import { BService, IRelatedObject } from '../shared/service/base.service';
import { ModuleModel } from './module.model';
import { CrudFileService } from '../shared/service/crud-file.service';


@Injectable()
@BService({
  entity: 'modules',
  entityModel: ModuleModel
})
export class ModulesService extends CrudFileService<ModuleModel> {

  public getModuleSpecification(module: ModuleModel): Observable<ModuleModel> {
    const headers = new Headers();
    headers.append('Content-Type', 'application/json');
    const options = new RequestOptions({ headers: headers });
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
