import { Injectable } from '@angular/core';

import { StreamModel } from '../models/index';
import { BaseService, BService } from './index';

@Injectable()
@BService({
  entity: 'streams',
  entityModel: StreamModel
})
export class StreamsService extends BaseService<StreamModel> { }
