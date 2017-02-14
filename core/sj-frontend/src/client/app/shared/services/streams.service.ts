import { Injectable } from '@angular/core';

import { StreamModel } from '../models';
import { BaseService, BService } from './';

@Injectable()
@BService({
  entity: 'streams',
  entityModel: StreamModel
})
export class StreamsService extends BaseService<StreamModel> { }
