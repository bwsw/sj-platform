import { Injectable } from '@angular/core';
import { Http, Response, Headers, RequestOptions } from '@angular/http';
import { Observable } from 'rxjs/Rx';

import { ServiceModel } from '../models/service.model';
import { BaseModel } from '../models/base.model';
import { BaseService, BService } from './base.service';

interface ISomeObject {
  [key: string]: string[];
}

@Injectable()
@BService({
  entity: 'services',
  entityModel: ServiceModel
})
export class ServicesService extends BaseService<ServiceModel> { }
