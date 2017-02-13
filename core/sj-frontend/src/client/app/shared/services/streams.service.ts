import { Injectable } from '@angular/core';
import { Http, Response, Headers, RequestOptions } from '@angular/http';
import { Observable } from 'rxjs/Rx';

import { StreamModel } from '../models/stream.model';
import { BaseModel } from '../models/base.model';
import { BaseService, BService } from './base.service';

@Injectable()
@BService({
  entity: 'streams',
  entityModel: StreamModel
})
export class StreamsService extends BaseService<StreamModel> { }
