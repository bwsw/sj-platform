import { Injectable } from '@angular/core';
import { Http, Response, Headers, RequestOptions, ResponseContentType } from '@angular/http';
import { Observable } from 'rxjs/Rx';

import { ModuleModel } from '../models/module.model';
import { BaseResponse } from '../models/base-response.model';

@Injectable()
export class ModulesService {
  private _dataUrl = '/v1/';

  constructor(private http: Http) {
  }

  public getModuleList(): Observable<ModuleModel[]> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({ headers: headers });
    return this.http.get(this._dataUrl + 'modules', options)
      .map(response => {
        const data = this.extractData(response);
        return data.modules;
      })
      .catch(this.handleError);
  }

  public getModuileTypes(): Observable<string[]> {
    return this.http.get(this._dataUrl + 'modules/_types')
      .map(response => {
        const data = this.extractData(response);
        return data.types;
      })
      .catch(this.handleError);
  }

  public getRelatedInstancesList(module: ModuleModel): Observable<string[]> {
    return this.http.get(this._dataUrl + 'modules/' + module.moduleType + '/' + module.moduleName + '/' +
      module.moduleVersion + '/related')
      .map(response => {
        const data = this.extractData(response);
        return data.instances;
      })
      .catch(this.handleError);
  }

  public getModuleSpecification(module: ModuleModel): Observable<ModuleModel> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({ headers: headers });
    return this.http.get(this._dataUrl + 'modules/' + module.moduleType + '/' + module.moduleName + '/' +
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

    return this.http.get(this._dataUrl + 'modules/' + module.moduleType + '/' + module.moduleName + '/' +
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
      xhr.open('POST', this._dataUrl + 'modules', true);
      let formData = new FormData();
      formData.append('jar', file, file.name);
      xhr.send(formData);
    });
  }

  public deleteModule(module: ModuleModel): Observable<ModuleModel> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({ headers: headers });
    return this.http.delete(this._dataUrl + 'modules/' + module.moduleType + '/' + module.moduleName + '/' +
      module.moduleVersion, options)
      .map(response => {
        const data = this.extractData(response);
        return data.message;
      })
      .catch(this.handleError);
  }

  private extractData(res: Response) {
    let body = new BaseResponse();
    body.fillFromJSON(res.json()['entity']);
    return body;
  }

  private handleError(error: any) {
    let errMsg = (error._body) ? error._body :
      error.status ? `${error.status} - ${error.statusText}` : 'Server error';
    errMsg = JSON.parse(errMsg);
    let errMsgYo = errMsg.entity.message;
    return Observable.throw(errMsgYo);
  }
}
