import { BaseModel } from '../shared';

export class FileModel extends BaseModel {
  name: string;
  description: string;
  'upload-date': string;
  version: string;
  size: number;
}

export class CustomFileModel extends FileModel {
  description: string;
  'upload-date': string;
}

export class CustomJarModel extends FileModel {
  version: string;
}
