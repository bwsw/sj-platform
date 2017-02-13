import { Injectable } from '@angular/core';
import { Response, Headers, RequestOptions, ResponseContentType } from '@angular/http';
import { Observable } from 'rxjs/Rx';

import { FileModel } from '../models/custom.model';
import { BaseService, BService, IResponse } from './base.service';

@Injectable()
@BService({
  entity: 'custom',
  entityModel: FileModel
})
export class CustomService extends BaseService<FileModel> {

  public downloadFile(path: string, file: FileModel): Observable<any> {
    let headers = new Headers();
    let options = new RequestOptions({ headers: headers, responseType: ResponseContentType.Blob });
    let downloadLink: string;
    if (path === 'files') {
      downloadLink = path + '/' + file.name;
    } else {
      downloadLink = path + '/' + file.name + '/' + file.version;
    }
    return this.http.get(this.requestUrl + '/' + downloadLink, options)
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
      xhr.open('POST', this.requestUrl + '/' + path, true);
      xhr.setRequestHeader('enctype', 'multipart/form-data');
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
    return this.http.delete(this.requestUrl + '/' + deleteLink, options)
      .map(response => {
        const data = this.extractData(response);
        return data.message;
      })
      .catch(this.handleError);
  }
}
