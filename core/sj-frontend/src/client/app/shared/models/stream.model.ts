export class StreamModel {
  name: string;
  description: string;
  service: string;
  type: string;
  tags: string[];
  partitions: number;
  generator: {
    generatorType: string;
    service: string;
    instanceCount: number;
  };
  replicationFactor: number;
  force: boolean = false;
  primary: string;
}
