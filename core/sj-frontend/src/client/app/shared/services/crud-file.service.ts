import { Headers, RequestOptions } from '@angular/http';
import { Observable } from 'rxjs/Rx';
import { BaseService, IRequestParams, IResponse } from './index';
import { BaseModel } from '../models/index';

export abstract class CrudFileService<M extends BaseModel> extends BaseService<M>  {

  public download(params?: IRequestParams) {
    return new Promise((resolve, reject) => {
      let xhr: XMLHttpRequest = new XMLHttpRequest();
      xhr.onreadystatechange = () => {
        if (xhr.readyState === 4) {
          if (xhr.status === 200) {
            let contDispos = xhr.getResponseHeader('content-disposition');
            resolve({
              blob: xhr.response,
              filename: contDispos.substring(contDispos.indexOf('filename=') + 10, contDispos.length-1)
            });
          } else {
            reject(this.handleError(xhr));
          }
        }
      };
      xhr.onprogress = event => {
        (<HTMLInputElement>document.getElementById('spinner-progress')).value = (event.loaded/params['size']*100).toString();
      };
      xhr.open('GET',  params['type'] ? `${this.requestUrl}/${params['type']}/${params['name']}/${params['version']}` :
        params['path'] === 'files' ? `${this.requestUrl}/${params['path']}/${params['name']}` :
          `${this.requestUrl}/${params['path']}/${params['name']}/${params['version']}`, true);
      xhr.responseType = 'blob';
      xhr.send();
    });
  }

  public upload(params?: IRequestParams) {
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
      xhr.upload.onprogress = event => {
        (<HTMLInputElement>document.getElementById('spinner-progress')).value = (event.loaded/event.total*100).toString();
      };
      xhr.open('POST', params['path'] ? `${this.requestUrl}/${params['path']}` : this.requestUrl, true);
      xhr.setRequestHeader('enctype', 'multipart/form-data');
      let formData = new FormData();
      if (params['path'] === 'jars' || !params['path']) {
        formData.append('jar', params['file'], params['name']);
      } else {
        formData.append('file', params['file']);
        if (params['description']) {
          formData.append('description', params['description']);
        }
      }
      xhr.send(formData);
    });
  }

  public removeFile(params?: IRequestParams): Observable<IResponse<M>> {
    let headers = new Headers();
    headers.append('Content-Type', 'application/json');
    let options = new RequestOptions({ headers: headers });

    return this.http.delete(params['type'] ? `${this.requestUrl}/${params['type']}/${params['name']}/${params['version']}` :
      params['path'] === 'files' ? `${this.requestUrl}/${params['path']}/${params['name']}` :
        `${this.requestUrl}/${params['path']}/${params['name']}/${params['version']}`, options)
      .map(response => {
        const data = this.extractData(response);
        return data;
      })
      .catch(this.handleError);
  }

  public sizeView(size: number, decimals: number): string {
    if(size === 0) return '0 Bytes';
    let k = 1000;
    let dm = decimals + 1 || 3;
    let sizes = ['Bytes', 'KB', 'MB'];
    let i = Math.floor(Math.log(size) / Math.log(k));
    return parseFloat((size / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
  }
}
