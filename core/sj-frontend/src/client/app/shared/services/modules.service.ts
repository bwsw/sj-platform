import { Injectable } from '@angular/core';
import { Http, Response, Headers, RequestOptions, ResponseContentType } from '@angular/http';
import { Observable } from 'rxjs/Rx';

import { ModuleModel } from '../models/module.model';

@Injectable()
export class ModulesService {
  private _dataUrl = '/v1/';

  constructor(private _http: Http) {
  }

  public getModuleList(): Observable<ModuleModel[]> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({ headers: headers });
    return this._http.get(this._dataUrl + 'modules', options)
      .map(this.extractData)
      .catch(this.handleError);
  }

  public getRelatedInstancesList(module: ModuleModel): Observable<string[]> {
    return this._http.get(this._dataUrl + 'modules/' + module['module-type'] + '/' + module['module-name'] + '/' +
      module['module-version'] + '/related')
      .map(this.extractData)
      .catch(this.handleError);
  }

  public getModuleSpecification(module: ModuleModel): Observable<ModuleModel> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({ headers: headers });
    return this._http.get(this._dataUrl + 'modules/' + module['module-type'] + '/' + module['module-name'] + '/' +
      module['module-version'] + '/specification', options)
      .map(this.extractData)
      .catch(this.handleError);
  }

  public downloadModule(module: ModuleModel): Observable<any> {
    let headers = new Headers();
    let options = new RequestOptions({ headers: headers, responseType: ResponseContentType.Blob });

    return this._http.get(this._dataUrl + 'modules/' + module['module-type'] + '/' + module['module-name'] + '/' +
      module['module-version'], options)
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
    return this._http.delete(this._dataUrl + 'modules/' + module['module-type'] + '/' + module['module-name'] + '/' +
      module['module-version'], options)
      .map(this.extractData)
      .catch(this.handleError);
  }

  private extractData(res: Response) {
    let body = {};
    if (typeof res.json()['entity']['specification'] !== 'undefined') {
      body = res.json()['entity']['specification'];
    } else {
      if (typeof res.json()['entity']['modules'] !== 'undefined') {
        body = res.json()['entity']['modules'];
      } else if (typeof res.json()['entity']['message'] !== 'undefined') {
        body = res.json()['entity']['message'];
      } else if (typeof res.json()['entity']['instances'] !== 'undefined') {
        body = res.json()['entity']['instances'];
      } else {
        body = res.json();
      }
    }
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
