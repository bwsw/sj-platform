import { BaseModel } from './base.model';

export class ModuleModel extends BaseModel {
  moduleType: string;
  moduleName: string;
  moduleVersion: string;
  name: string;
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
}
