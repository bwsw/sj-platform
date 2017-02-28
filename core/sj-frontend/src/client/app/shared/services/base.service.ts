import { Locator } from './index';
import { BaseModel } from '../models/index';
import { Http, Response, Headers, RequestOptions, ResponseContentType } from '@angular/http';
import { Observable } from 'rxjs/Rx';

interface Type<T> {
  new(...args: any[]): T;
}

export class IResponse<M extends BaseModel> {
  message: string;
  connection: boolean;
  types: string[];
  providers: M[];
  services: M[];
  streams: M[];
  instances: M[];
  modules: M[];
  configSettings: M[];
  customFiles: M[];
  customJars: M[];
  specification: M;
  instance: M;
  provider: M;
  service: M;
  stream: M;
  domains: string[];
  [key: string]: any;

  fillFromJSON(json: any) {
    for (let propName in json) {
      this[propName] = json[propName];
    }
  }
}

interface IRelatedObject {
  [key: string]: string[];
}

interface IBaseServiceSettings<T> {
  endpoint?: string;
  entity: string;
  entityModel: Type<T>;
}
const DEFAULT_API_ENDPOINT = '/v1';

export function BService<T>(data: IBaseServiceSettings<T>): ClassDecorator {
  if (!data.endpoint) {
    data.endpoint = DEFAULT_API_ENDPOINT;
  }
  return (target: Function) => {
    target.prototype.endpoint = data.endpoint;
    target.prototype.entity = data.entity;
    target.prototype.entityModel = data.entityModel;
    return target;
  };
}

export interface IRequestParams {
  [key: string]: any;
}

export abstract class BaseService<M extends BaseModel> {
  protected endpoint: string;
  protected entity: string;
  protected entityModel: Type<M>;
  protected http: Http;

  protected get requestUrl(): string {
    return `${this.endpoint}/${this.entity}`;
  }

  protected get requestHeaders(): Headers {
    let headers = {
      'Content-Type': 'application/json'
    };
    return new Headers(headers);
  }

  constructor() {
    this.http = Locator.injector.get(Http);
    this.handleError = this.handleError.bind(this);
  }

  protected getRequestOptions(options?: IRequestParams): RequestOptions {
    let requestOptions = Object.assign({}, { headers: this.requestHeaders }, options || {});
    return new RequestOptions(requestOptions);
  }

  protected extractData(res: Response): IResponse<M> {
    let body = new IResponse<M>();
    body.fillFromJSON(res.json()['entity']);
    return body;
  }

  protected handleError(error: any) {
    let errSatus = error.status ? `${error.status} - ${error.statusText}` : (error._body) ? error._body : 'Server error';
      try {
        let errMsg = (error._body) ? JSON.parse(error._body) : 'Server error';
        let errMsgYo = errMsg.entity ? errMsg.entity.message ?  errMsg.entity.message:  errMsg.entity.errors : errMsg;
        return Observable.throw(errMsgYo);
      } catch (e) {
        return Observable.throw(errSatus);
      }
  }

  public get(name?: string): Observable<IResponse<M>> {
  return this.http
    .get(name ? `${this.requestUrl}/${name}` : this.requestUrl, this.getRequestOptions())
    .map(this.extractData)
    .catch(this.handleError);
}

  public getList(path?: string): Observable<IResponse<M>> {
    return this.http
      .get( path? `${this.requestUrl}/${path}`:this.requestUrl, this.getRequestOptions())
      .map(this.extractData)
      .catch(this.handleError);
  }

  public getRelatedList(name?: string, type?: string, version?: string): Observable<IRelatedObject> {
    return type && version ?
      this.http
        .get(`${this.requestUrl}/${type}/${name}/${version}` + '/related', this.getRequestOptions())
        .map(this.extractData)
        .catch(this.handleError) :
      this.http
      .get(`${this.requestUrl}/${name}` + '/related', this.getRequestOptions())
      .map(this.extractData)
      .catch(this.handleError);
  }


  public getTypes(): Observable<IResponse<M>> {
    return this.getList('_types');
  }

  public save(model: M): Observable<IResponse<M>> {
    return this.post(model);
  };

  protected post(model: M): Observable<IResponse<M>> {
    return this.http
      .post(this.requestUrl, model, this.getRequestOptions())
      .map(this.extractData)
      .catch(this.handleError);
  }

  public remove(name: string): Observable<IResponse<M>> {
    return this.http
      .delete(`${this.requestUrl}/${name}`, this.getRequestOptions())
      .map(this.extractData)
      .catch(this.handleError);
  }

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
