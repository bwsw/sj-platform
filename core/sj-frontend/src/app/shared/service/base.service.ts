import { Locator } from './locator.service';
import { BaseModel } from '../model/base.model';
import { Http, Response, Headers, RequestOptions } from '@angular/http';
import { Observable } from 'rxjs/Rx';
import { TypeModel } from '../model/type.model';

interface Type<T> {
  new(...args: any[]): T;
}

export class IResponse<M extends BaseModel> {
  message: string;
  connection: boolean;
  types: TypeModel[];
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
  domains: TypeModel[];
  [key: string]: any;

  fillFromJSON(json: any) {
    for (const propName of Object.keys(json)) {
      this[propName] = json[propName];
    }
  }
}

export interface IRelatedObject {
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
  protected cache: Observable<IResponse<M>> = null;

  protected get requestUrl(): string {
    return `${this.endpoint}/${this.entity}`;
  }

  protected get requestHeaders(): Headers {
    const headers = {
      'Content-Type': 'application/json'
    };
    return new Headers(headers);
  }

  constructor() {
    this.http = Locator.injector.get(Http);
    this.handleError = this.handleError.bind(this);
  }

  protected getRequestOptions(options?: IRequestParams): RequestOptions {
    const requestOptions = Object.assign({}, { headers: this.requestHeaders }, options || {});
    return new RequestOptions(requestOptions);
  }

  protected extractData(res: Response): IResponse<M> {
    const body = new IResponse<M>();
    body.fillFromJSON(res.json()['entity']);
    return body;
  }

  protected handleError(error: any) {
    const errSatus = error.status ? `${error.status} - ${error.statusText}` : (error._body) ? error._body : 'Server error';
      try {
        const errMsg = (error._body) ? JSON.parse(error._body) : 'Server error';
        const errMsgYo = errMsg.entity ? errMsg.entity.message ?  this.makeUserFriendly(errMsg.entity.message) :
            this.makeUserFriendly(errMsg.entity.errors) : errMsg;
        return Observable.throw(errMsgYo);
      } catch (e) {
        return Observable.throw(errSatus);
      }
  }
  protected makeUserFriendly(msg: string): string {
    return msg.replace(/([a-z])([A-Z])/g, '$1 $2').replace(/([;])/g, '$1 ');
  }

  public get(name?: string): Observable<IResponse<M>> {
  return this.http
    .get(name ? `${this.requestUrl}/${name}` : this.requestUrl, this.getRequestOptions())
    .map(this.extractData)
    .catch(this.handleError);
}

  public getList(path?: string): Observable<IResponse<M>> {
    return this.http
      .get( path ? `${this.requestUrl}/${path}` : this.requestUrl, this.getRequestOptions())
      .map(this.extractData)
      .catch(this.handleError);
  }

  public getTypes(): Observable<IResponse<M>> {
    if (!this.cache) {
      this.cache = this.getList('_types');
    }
    return this.cache;
  }

}
