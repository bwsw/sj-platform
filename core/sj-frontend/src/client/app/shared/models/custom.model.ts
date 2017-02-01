export class FileModel {
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
