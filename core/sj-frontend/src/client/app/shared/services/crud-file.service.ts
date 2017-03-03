import { Response, Headers, RequestOptions, ResponseContentType } from '@angular/http';
import { Observable } from 'rxjs/Rx';
import { BaseService, IRequestParams, IResponse } from './index';
import { BaseModel } from '../models/index';

export abstract class CrudFileService<M extends BaseModel> extends BaseService<M>  {

  public download(params?: IRequestParams): Observable<any> {
    let headers = new Headers();
    let options = new RequestOptions({ headers: headers, responseType: ResponseContentType.Blob });

    return this.http.get( params['type'] ? `${this.requestUrl}/${params['type']}/${params['name']}/${params['version']}` :
      params['path'] === 'files' ? `${this.requestUrl}/${params['path']}/${params['name']}` :
        `${this.requestUrl}/${params['path']}/${params['name']}/${params['version']}`, options)
      .map((res: Response) => {
        let contDispos = res.headers.get('content-disposition');
        return {
          blob: res.blob(),
          filename: contDispos.substring(contDispos.indexOf('filename=') + 9, contDispos.length)
        };
      })
      .catch(this.handleError);
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
}
