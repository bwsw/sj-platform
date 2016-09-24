import { ModuleModel } from './module.model';

export class InstanceModel {
  module: ModuleModel;
  name: string;
  description: string;
  'checkpoint-mode': string;
  'checkpoint-interval': number;
  parallelism: string;
  options: Object;
  'per-task-cores': number;
  'per-task-ram': number;
  'jvm-options': Object;
  'node-attributes': Object;
  'coordination-service': string;
  'environment-variables': Object;
  'performance-reporting-interval': number;
  inputs: string[] = [''];
  'input-type': string[] = [''];
  outputs: string[] = [''];
  'start-from': string;
  'state-management': string;
  'state-full-checkpoint': string;
  'event-wait-time': number;
  'time-windowed': number;
  'window-full-max': number;
  input: string;
  output: string;
  'duplicate-check': boolean;
  'lookup-history': number;
  'queue-max-size': number;
  'default-eviction-policy': string;
  'eviction-policy': string;
  'backup-count': number;
  'async-backup-count': number;
  [key: string]: any;
}
