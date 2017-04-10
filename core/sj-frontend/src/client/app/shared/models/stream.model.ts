import { BaseModel } from './base.model';

export class StreamModel extends BaseModel {
  name: string;
  description: string;
  service: string;
  type: string;
  tags: string[];
  partitions: number;
  replicationFactor: number;
  force: boolean = false;
  primary: string;
}
