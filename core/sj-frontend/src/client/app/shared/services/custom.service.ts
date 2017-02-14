import { Injectable } from '@angular/core';

import { FileModel } from '../models/custom.model';
import { BaseService, BService } from './base.service';

@Injectable()
@BService({
  entity: 'custom',
  entityModel: FileModel
})
export class CustomService extends BaseService<FileModel> { }
