export class TaskModel {
  id: number;
  state: string;
  stateChange: string;
  reason: string;
  node: string;
  lastNode: string;
  directories: Directory[];
}

export class Directory {
  name: string;
  path: string;
}
