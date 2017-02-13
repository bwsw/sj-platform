import { Locator } from './locator.service';
import { BaseModel } from "../models/base.model";
import { Http, Response, Headers, RequestOptions } from '@angular/http';
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
  requestPath?: string;
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
    target.prototype.requestPath = data.requestPath;
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
  protected requestPath: string = this.entity;
  protected entityModel: Type<M>;
  protected http: Http;

  protected get requestUrl(): string {
    return `${this.endpoint}/${this.requestPath}`;
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
    let errMsg = (error._body) ? error._body :
      error.status ? `${error.status} - ${error.statusText}` : 'Server error';
    errMsg = JSON.parse(errMsg);
    let errMsgYo = errMsg.entity.message;
    return Observable.throw(errMsgYo);
  }

  public get(name?: string): Observable<IResponse<M>> {
  this.requestPath = this.entity;
  return this.http
    .get(name ? `${this.requestUrl}/${name}` : this.requestUrl, this.getRequestOptions())
    .map(response => {
      const data = this.extractData(response);
      return data;
    })
    .catch(this.handleError);
}

  public getList(): Observable<IResponse<M>> {
    return this.http
      .get(this.requestUrl, this.getRequestOptions())
      .map(response => {
        const data = this.extractData(response);
        return data;
      })
      .catch(this.handleError);
  }

  public getRelatedList(name?: string, type?: string, version?: string): Observable<IRelatedObject> {
    return type && version ?
      this.http
        .get(`${this.requestUrl}/${type}/${name}/${version}` + '/related', this.getRequestOptions())
        .map(response => {
          const data = this.extractData(response);
          return data;
        })
        .catch(this.handleError) :
      this.http
      .get(`${this.requestUrl}/${name}` + '/related', this.getRequestOptions())
      .map(response => {
        const data = this.extractData(response);
        return data;
      })
      .catch(this.handleError);
  }


  public getTypes(): Observable<IResponse<M>> {
    this.requestPath = this.entity + '/_types';
    return this.getList();
  }

  public save(model: M): Observable<IResponse<M>> {
    this.requestPath = this.entity;
    return this.post(model);
  };

  protected post(model: M): Observable<IResponse<M>> {
    return this.http
      .post(this.requestUrl, model, this.getRequestOptions())
      .map(response => this.extractData(response))
      .catch(this.handleError);
  }

  public remove(name: string): Observable<void> {
    this.requestPath = this.entity;
    return this.http
      .delete(`${this.requestUrl}/${name}`, this.getRequestOptions())
      .catch(this.handleError);
  }

}
