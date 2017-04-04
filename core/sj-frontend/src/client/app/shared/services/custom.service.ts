import { Injectable } from '@angular/core';

import { FileModel } from '../models/index';
import { CrudFileService, BService } from './index';

@Injectable()
@BService({
  entity: 'custom',
  entityModel: FileModel
})
export class CustomService extends CrudFileService<FileModel> { }
