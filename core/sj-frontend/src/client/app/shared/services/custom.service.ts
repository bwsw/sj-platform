import { Injectable } from '@angular/core';

import { FileModel } from '../models/index';
import { BaseService, BService } from './index';

@Injectable()
@BService({
  entity: 'custom',
  entityModel: FileModel
})
export class CustomService extends BaseService<FileModel> { }
