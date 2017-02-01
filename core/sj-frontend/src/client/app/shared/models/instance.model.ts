import { ModuleModel } from './module.model';

export class InstanceModel {
  'inputs-types': string[] = [''];
  'main-stream-type': string = '';
  'related-streams-type': string[] = [];
  'module': ModuleModel;
  'start-from-timestamp': number;
  'status': string;

  'rest-address': string;

  'async-backup-count': number = 0;
  'backup-count': number = 0;
  'batch-fill-type' : {};
  'batch-fill-type-name': string;
  'batch-fill-type-value': number;
  'checkpoint-interval': number;
  'checkpoint-mode': string;
  'coordination-service': string;
  'default-eviction-policy': string = 'NONE';
  'description': string;
  'duplicate-check': boolean = false;
  engine: string;
  'environment-variables': Object;
  'event-wait-idle-time': number = 1000;
  'eviction-policy': string = 'fix-time';
  'input': string;
  'inputs': string[] = [''];
  'jvm-options': Object;
  'lookup-history': number = 0;
  'main-stream' : string;
  'name': string;
  'node-attributes': Object;
  'options': Object;
  'output': string;
  'outputs': string[] = [''];
  'parallelism': string = '1';
  'per-task-cores': number = 1;
  'per-task-ram': number = 1024;
  'performance-reporting-interval': number = 60000;
  'queue-max-size': number;
  'related-streams' : string[] = [];
  'sliding-interval' : number;
  'stages': Object;
  'start-from': string = 'newest';
  'state-full-checkpoint': number = 100;
  'state-management': string = 'none';
  'window' : number = 1;

  [key: string]: any;
}

export class SubtypedInstance {
  'coordination-service': string;
  'description': string;
  'environment-variables': Object;
  'jvm-options': Object;
  'name': string;
  'node-attributes': Object;
  'options': Object;
  'parallelism': string|number;
  'per-task-cores': number;
  'per-task-ram': number;
  'performance-reporting-interval': number;
  'stages': Object;

  [key: string]: any;
}

export class RegularStreamingInstance extends SubtypedInstance {
  'checkpoint-interval': number;
  'checkpoint-mode': string;
  'event-wait-idle-time': number;
  'inputs': string[] = [''];
  'outputs': string[] = [''];
  'start-from': string|number;
  'state-full-checkpoint': string;
  'state-management': string;
}

export class WindowedStreamingInstance extends SubtypedInstance {
  'outputs': string[] = [''];
  'window' : number;
  'sliding-interval' : number;
  'main-stream' : string;
  'related-streams' : string[] = [];
  'batch-fill-type' : Object;
  'start-from' : string|number;
  'state-management' : string;
  'state-full-checkpoint' : number;
  'event-wait-time' : number;
}

export class OutputStreamingInstance extends SubtypedInstance {
  'checkpoint-interval': number;
  'checkpoint-mode': string;
  'input': string;
  'output': string;
  'start-from': string|number;
}

export class InputStreamingInstance extends SubtypedInstance {
  'async-backup-count': number;
  'backup-count': number;
  'checkpoint-interval': number;
  'checkpoint-mode': string;
  'default-eviction-policy': string;
  'duplicate-check': boolean;
  'eviction-policy': string;
  'lookup-history': number;
  'outputs': string[] = [''];
  'queue-max-size': number;
}
