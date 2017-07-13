import { Injectable } from '@angular/core';
import { BService } from '../shared/service/base.service';
import { FileModel } from './custom.model';
import { CrudFileService } from '../shared/service/crud-file.service';

@Injectable()
@BService({
  entity: 'custom',
  entityModel: FileModel
})
export class CustomService extends CrudFileService<FileModel> { }
