import { Injectable } from '@angular/core';
import { ServiceModel } from '../models/service.model';
import { BaseService, BService } from './base.service';

@Injectable()
@BService({
  entity: 'services',
  entityModel: ServiceModel
})
export class ServicesService extends BaseService<ServiceModel> { }
