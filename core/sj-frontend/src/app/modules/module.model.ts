import { BaseModel } from '../shared';

export class ModuleModel extends BaseModel {
  moduleType: string;
  moduleName: string;
  moduleVersion: string;
  name: string;
  size: number;
  description: string;
  version: string;
  author: string;
  license: string;
  engineName: string;
  engineVersion: string;
  options: Object;
  validateClass: string;
  executorClass: string;
  inputs: {
    cardinality: number[];
    types: string[];
  };
  outputs: {
    cardinality: number[];
    types: string[];
  };
  uploadDate: Date;

  public get engine(): string {
    return this.engineName + ' ' + this.engineVersion;
  }
}
