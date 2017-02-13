import { BaseModel } from "./base.model";

export class FileModel extends BaseModel {
  name: string;
  description: string;
  'upload-date': string;
  version: string;
}

export class CustomFileModel extends FileModel {
  description: string;
  'upload-date': string;
}

export class CustomJarModel extends FileModel {
  version: string;
}
