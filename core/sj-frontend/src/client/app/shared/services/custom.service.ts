import { Injectable } from '@angular/core';

import { FileModel } from '../models';
import { BaseService, BService } from './';

@Injectable()
@BService({
  entity: 'custom',
  entityModel: FileModel
})
export class CustomService extends BaseService<FileModel> { }
