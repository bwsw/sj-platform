import { BaseModel } from './base.model';

export class ServiceModel extends BaseModel {
  type: string;
  name: string;
  description: string;
  database: string;
  driver: string;
  keyspace: string;
  namespace: string;
  provider: string;
  metadataProvider: string;
  metadataNamespace: string;
  dataProvider: string;
  dataNamespace: string;
  lockProvider: string;
  lockNamespace: string;
  zkProvider: string;
  zkNamespace: string;
  index: string;
  login: string;
  password: string;
}
