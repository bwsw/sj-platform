import { Injectable } from '@angular/core';
import { ServiceModel } from '../models';
import { BaseService, BService } from './';

@Injectable()
@BService({
  entity: 'services',
  entityModel: ServiceModel
})
export class ServicesService extends BaseService<ServiceModel> { }
