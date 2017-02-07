import { Injectable } from '@angular/core';
import { Http, Response, Headers, RequestOptions, ResponseContentType } from '@angular/http';
import { Observable } from 'rxjs/Rx';

import { FileModel } from '../models/custom.model';
import { BaseResponse } from '../models/base-response.model';

@Injectable()
export class CustomService {
  private _dataUrl = '/v1/';

  constructor(private http: Http) {
  }

  public getCustomList(path: string): Observable<FileModel[]> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({ headers: headers });
    return this.http.get(this._dataUrl + 'custom/' + path, options)
      .map(response => {
        const data = this.extractData(response);
        if (path === 'files') {
          return data.customFiles
        } else {
          return data.customJars;
        }
      })
      .catch(this.handleError);
  }

  public downloadFile(path: string, file: FileModel): Observable<any> {
    let headers = new Headers();
    let options = new RequestOptions({ headers: headers, responseType: ResponseContentType.Blob });
    let downloadLink: string;
    if (path === 'files') {
      downloadLink = path + '/' + file.name;
    } else {
      downloadLink = path + '/' + file.name + '/' + file.version;
    }
    return this.http.get(this._dataUrl + 'custom/' + downloadLink, options)
      .map((res: Response) => {
        let contDispos = res.headers.get('content-disposition');
        return {
          blob: res.blob(),
          filename: contDispos.substring(contDispos.indexOf('filename=') + 9, contDispos.length)
        };
      })
      .catch(this.handleError);
  }

  public uploadFile(path: string, file: any, description?: string) { //TODO Check for image type
    return new Promise((resolve, reject) => {
      let xhr: XMLHttpRequest = new XMLHttpRequest();
      xhr.onreadystatechange = () => {
        if (xhr.readyState === 4) {
          if (xhr.status === 200) {
            resolve(JSON.parse(xhr.response).entity.message);
          } else {
            reject(xhr.response);
          }
        }
      };
      xhr.open('POST', this._dataUrl + 'custom/' + path, true);
      xhr.setRequestHeader("enctype", "multipart/form-data");
      let formData = new FormData();
      if (path === 'jars') {
        formData.append('jar', file, file.name);
      } else {
        formData.append('file', file);
        if (description) {
          formData.append('description', description);
        }
      }
      xhr.send(formData);
    });
  }

  public deleteFile(path: string, file: FileModel): Observable<FileModel> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({headers: headers});
    let deleteLink: string;
    if (path === 'files') {
      deleteLink = path + '/' + file.name;
    } else {
      deleteLink = path + '/' + file.name + '/' + file.version;
    }
    return this.http.delete(this._dataUrl + 'custom/' + deleteLink, options)
      .map(response => {
        const data = this.extractData(response);
        let node: string = 'custom'+path;
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
