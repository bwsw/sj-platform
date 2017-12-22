import { BaseModel } from '../shared';

export class FileModel extends BaseModel {
  name: string;
  description: string;
  uploadDate: string;
  version: string;
  size: number;
}

export class CustomFileModel extends FileModel {
  description: string;
}

export class CustomJarModel extends FileModel {
  version: string;
}
