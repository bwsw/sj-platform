import { BaseModel } from '../shared';

export class ServiceModel extends BaseModel {
  type: string;
  name: string;
  description: string;
  database: string;
  namespace: string;
  provider: string;
  zkProvider: string;
  zkNamespace: string;
  prefix: string;
  token: string;
  index: string;
  basePath: string = '/';
  httpVersion: string = '1.1';
  headers: Object;
  httpScheme: string = 'http';
}
