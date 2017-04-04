import { BaseModel } from './base.model';

export class StreamModel extends BaseModel {
  name: string;
  description: string;
  service: string;
  type: string;
  tags: string[];
  partitions: number;
<<<<<<< HEAD
  generator: {
    generatorType: string;
    service: string;
    instanceCount: number;
  };
  replicationFactor: number;
=======
  'replication-factor': number;
>>>>>>> 01640c1d319f8c567c3e95f4153f5eb27f1e8945
  force: boolean = false;
  primary: string;
}
