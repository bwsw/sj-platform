import { Injectable } from '@angular/core';

import { StreamModel } from '../models/stream.model';
import { BaseService, BService } from './base.service';

@Injectable()
@BService({
  entity: 'streams',
  entityModel: StreamModel
})
export class StreamsService extends BaseService<StreamModel> { }
