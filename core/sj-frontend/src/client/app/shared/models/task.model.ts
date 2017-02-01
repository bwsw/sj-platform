export class TaskModel {
  id: number;
  state: string;
  'state-change': string;
  reason: string;
  node: string;
  'last-node': string;
  directories: string;
}
