import { Injectable } from '@angular/core';
import { Response, Headers, RequestOptions, ResponseContentType } from '@angular/http';
import { Observable } from 'rxjs/Rx';

import { ModuleModel } from '../models/module.model';
import { BaseService, BService } from './base.service';

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

  public downloadModule(module: ModuleModel): Observable<any> {
    let headers = new Headers();
    let options = new RequestOptions({ headers: headers, responseType: ResponseContentType.Blob });

    return this.http.get(this.requestUrl + '/' + module.moduleType + '/' + module.moduleName + '/' +
      module.moduleVersion, options)
      .map((res: Response) => {
        let contDispos = res.headers.get('content-disposition');
        return {
          blob: res.blob(),
          filename: contDispos.substring(contDispos.indexOf('filename=') + 9, contDispos.length)
        };
      })
      .catch(this.handleError);
  }

  public uploadModule(file: any) { //TODO Check for image type
    return new Promise((resolve, reject) => {
      let xhr: XMLHttpRequest = new XMLHttpRequest();
      xhr.onreadystatechange = () => {
        if (xhr.readyState === 4) {
          if (xhr.status === 200) {
            resolve(JSON.parse(xhr.response).entity.message);
          } else {
            reject(JSON.parse(xhr.response).entity.message);
          }
        }
      };
      xhr.open('POST', this.requestUrl, true);
      let formData = new FormData();
      formData.append('jar', file, file.name);
      xhr.send(formData);
    });
  }

  public deleteModule(module: ModuleModel): Observable<ModuleModel> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({ headers: headers });
    return this.http.delete(this.requestUrl + '/' + module.moduleType + '/' + module.moduleName + '/' +
      module.moduleVersion, options)
      .map(response => {
        const data = this.extractData(response);
        return data.message;
      })
      .catch(this.handleError);
  }
}
