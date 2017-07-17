import { BaseModel } from '../shared';

export class StreamModel extends BaseModel {
  name: string;
  description: string;
  service: string;
  type: string;
  tags: string[];
  partitions: number = 1;
  replicationFactor: number;
  force = false;
  primary: string;
}
