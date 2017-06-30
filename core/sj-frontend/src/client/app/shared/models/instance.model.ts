import { ModuleModel } from './module.model';
import { BaseModel } from './base.model';

export class InstanceModel extends BaseModel {
  inputsTypes: string[] = [''];
  module: ModuleModel;
  startFromDateTime: string;
  status: string;

  moduleType: string;
  moduleName: string;
  moduleVersion: string;
  restAddress: string;

  asyncBackupCount: number = 0;
  backupCount: number = 0;
  checkpointInterval: number;
  checkpointMode: string;
  coordinationService: string;
  defaultEvictionPolicy: string = 'NONE';
  description: string;
  duplicateCheck: boolean = false;
  engine: string;
  environmentVariables: Object;
  eventWaitIdleTime: number = 1000;
  evictionPolicy: string = 'fix-time';
  input: string = '';
  inputs: string[] = [''];
  jvmOptions: Object;
  lookupHistory: number = 0;
  name: string;
  nodeAttributes: Object;
  options: Object;
  output: string = '';
  outputs: string[] = [''];
  parallelism: string = '1';
  perTaskCores: number = 1;
  perTaskRam: number = 1024;
  performanceReportingInterval: number = 60000;
  queueMaxSize: number;
  slidingInterval: number;
  stage: Object;
  startFrom: string = 'newest';
  stateFullCheckpoint: number = 100;
  stateManagement: string = 'none';
  window: number = 1;
  executionPlan: {
    tasks: {}
  };
  tasks: {};

  [key: string]: any;
}

export class SubtypedInstance {
  coordinationService: string;
  description: string;
  environmentVariables: Object;
  jvmOptions: Object;
  name: string;
  nodeAttributes: Object;
  options: Object;
  parallelism: string|number;
  perTaskCores: number;
  perTaskRam: number;
  performanceReportingInterval: number;
  stage: Object;
  checkpointInterval: number;
  checkpointMode: string;
  eventWaitIdleTime: number;
  inputs: string[] = [''];
  outputs: string[] = [''];
  startFrom: string;
  stateFullCheckpoint: number;
  stateManagement: string;
  window: number;
  slidingInterval: number;
  input: string;
  output: string;
  asyncBackupCount: number;
  backupCount: number;
  defaultEvictionPolicy: string;
  duplicateCheck: boolean;
  evictionPolicy: string;
  lookupHistory: number;
  queueMaxSize: number;
  [key: string]: any;
}

export class RegularStreamingInstance extends SubtypedInstance {
  checkpointInterval: number;
  checkpointMode: string;
  eventWaitIdleTime: number;
  inputs: string[] = [''];
  outputs: string[] = [''];
  startFrom: string;
  stateFullCheckpoint: number;
  stateManagement: string;
}

export class BatchStreamingInstance extends SubtypedInstance {
  outputs: string[] = [''];
  window: number;
  slidingInterval: number;
  inputs: string[] = [''];
  startFrom: string;
  stateManagement: string;
  stateFullCheckpoint: number;
  eventWaitTime: number;
}

export class OutputStreamingInstance extends SubtypedInstance {
  checkpointInterval: number;
  checkpointMode: string;
  input: string;
  output: string;
  startFrom: string;
}

export class InputStreamingInstance extends SubtypedInstance {
  asyncBackupCount: number;
  backupCount: number;
  checkpointInterval: number;
  checkpointMode: string;
  defaultEvictionPolicy: string;
  duplicateCheck: boolean;
  evictionPolicy: string;
  lookupHistory: number;
  outputs: string[] = [''];
  queueMaxSize: number;
}
