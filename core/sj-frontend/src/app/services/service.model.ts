import { BaseModel } from '../shared';

export class ServiceModel extends BaseModel {
  type: string;
  name: string;
  description: string;
  database: string;
  keyspace: string;
  namespace: string;
  provider: string;
  zkProvider: string;
  zkNamespace: string;
  prefix: string;
  token: string;
  index: string;
  login: string;
  password: string;
}
