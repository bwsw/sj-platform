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
<<<<<<< HEAD
  metadataProvider: string;
  metadataNamespace: string;
  dataProvider: string;
  dataNamespace: string;
  lockProvider: string;
  lockNamespace: string;
  zkProvider: string;
  zkNamespace: string;
=======
  prefix: string;
  token: string;
  'zk-provider': string;
  'zk-namespace': string;
>>>>>>> 01640c1d319f8c567c3e95f4153f5eb27f1e8945
  index: string;
  login: string;
  password: string;
}
