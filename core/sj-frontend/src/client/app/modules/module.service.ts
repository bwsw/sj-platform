import { Injectable } from '@angular/core';
import { Http, Response, Headers, RequestOptions } from '@angular/http';
import { Observable } from 'rxjs/Rx';
import 'rxjs/Rx';

import { Module } from './module';

@Injectable()
export class ModuleService {

    private dataUrl = '/v1/';
    constructor(private http: Http) {}

    public getModuleList(): Observable<Module[]> {
        let headers = new Headers();
        headers.append('Content-Type', 'application/json');
        let options = new RequestOptions({ headers: headers });
        return this.http.get(this.dataUrl + 'modules', options)
            .map(this.extractData)
            .catch(this.handleError);
    }
    public getModuleSpecification(module: Module): Observable<Module> {
        let headers = new Headers();
        headers.append('Content-Type', 'application/json');
        let options = new RequestOptions({ headers: headers });
        return this.http.get(this.dataUrl + 'modules/' + module['module-type'] + '/' + module['module-name'] + '/' +
                module['module-version'] + '/specification', options)
            .map(this.extractData)
            .catch(this.handleError);
    }
  public downloadModule(module: Module): Observable<any> {
    let headers = new Headers();
    headers.append('responseType', 'arraybuffer');
    let options = new RequestOptions({ headers: headers });
    return this.http.get(this.dataUrl + 'modules/' + module['module-type'] + '/' + module['module-name'] + '/' +
        module['module-version'], options)
      .map(res => new Blob([res['_body']],{ type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }))
      .catch(this.handleError);
  }
    public uploadModule(file: any) { //TODO Check for image type
        return new Promise((resolve, reject) => {
            let xhr:XMLHttpRequest = new XMLHttpRequest();
            xhr.onreadystatechange = () => {
                if (xhr.readyState === 4) {
                    if (xhr.status === 200) {
                        resolve(JSON.parse(xhr.response));
                    } else {
                        reject(JSON.parse(xhr.response));
                    }
                }
            };
            xhr.open('POST', this.dataUrl + 'modules', true);
            let formData = new FormData();
            formData.append('jar', file, file.name);
            xhr.send(formData);
        });
    }
    public deleteModule(module: Module): Observable<Module> {
        let headers = new Headers();
        headers.append('Content-Type', 'application/json');
        let options = new RequestOptions({ headers: headers });
        return this.http.delete(this.dataUrl + 'modules/'  + module['module-type'] + '/' + module['module-name'] + '/' +
                module['module-version'], options)
            .map(this.extractData)
            .catch(this.handleError);
    }
    private extractData(res: Response) {
        let  body = {};
        if (typeof res.json()['entity']['specification'] !== 'undefined') {
            body = res.json()['entity']['specification'];
        } else {
            if (typeof res.json()['entity']['modules'] !== 'undefined') {
                body = res.json()['entity']['modules'];
            }  else if (typeof res.json()['entity']['message'] !== 'undefined') {
                body = res.json()['entity']['message'];
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
