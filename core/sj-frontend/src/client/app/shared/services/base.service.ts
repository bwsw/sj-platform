import { BaseResponse } from "../models/base-response.model";

interface Type<T> {
  new(...args: any[]): T;
}

interface IBaseServiceSettings<T> {
  endpoint?: string;
  version?: string;
  entity: string;
  entityModel: Type<T>;
  cache?: boolean;
}
const DEFAULT_API_ENDPOINT = '/v1/';

export function BService<T>(data: IBaseServiceSettings<T>): ClassDecorator {
  if (!data.endpoint) {
    data.endpoint = DEFAULT_API_ENDPOINT;
  }
  return (target: Function) => {
    target.prototype.endpoint = data.endpoint;
    target.prototype.entity = data.entity;
    target.prototype.entityModel = data.entityModel;
    if (data.hasOwnProperty('cache')) {
      target.prototype.cache = data.cache;
    }
    return target;
  };
}

export interface IRequestParams {
  [key: string]: any;
}

export abstract class BaseService<M extends BaseResponse> {
  protected endpoint: string;
  protected entity: string;

  protected get requestUrl(): string {
    return `${this.endpoint}/${this.entity}`;
  }




}
