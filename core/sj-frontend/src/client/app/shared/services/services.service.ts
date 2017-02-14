import { Injectable } from '@angular/core';
import { ServiceModel } from '../models/index';
import { BaseService, BService } from './index';

@Injectable()
@BService({
  entity: 'services',
  entityModel: ServiceModel
})
export class ServicesService extends BaseService<ServiceModel> { }
