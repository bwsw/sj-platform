import { ModuleModel } from './module.model';

export class InstanceModel {
  'inputs-types': string[] = [''];
  'module': ModuleModel;
  'start-from-timestamp': number;
  'status': string;

  'async-backup-count': number;
  'backup-count': number;
  'checkpoint-interval': number;
  'checkpoint-mode': string;
  'coordination-service': string;
  'default-eviction-policy': string;
  'description': string;
  'duplicate-check': boolean;
  'environment-variables': Object;
  'event-wait-idle-time': number;
  'eviction-policy': string;
  'input': string;
  'inputs': string[] = [''];
  'jvm-options': Object;
  'lookup-history': number;
  'name': string;
  'node-attributes': Object;
  'options': Object;
  'output': string;
  'outputs': string[] = [''];
  'parallelism': string;
  'per-task-cores': number;
  'per-task-ram': number;
  'performance-reporting-interval': number;
  'queue-max-size': number;
  'start-from': string;
  'state-full-checkpoint': string;
  'state-management': string;

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

// export class WindowedStreamingInstance extends SubtypedInstance {}

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
