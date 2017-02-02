export class StreamModel {
  name: string;
  description: string;
  service: string;
  'stream-type': string;
  tags: string[];
  partitions: number;
  'replication-factor': number;
  force: boolean = false;
  primary: string;
}
