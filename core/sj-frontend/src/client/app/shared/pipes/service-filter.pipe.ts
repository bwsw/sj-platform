import { Pipe, PipeTransform } from '@angular/core';

import { ServiceModel } from '../models/service.model';

@Pipe({
  name: 'serviceFilter'
})
export class ServiceFilterPipe implements PipeTransform {
  public transform(value: [ServiceModel], term: string) {
    var stream_types = ['stream.kafka', 'stream.t-stream', 'elasticsearch-output', 'jdbc-output'];
    var service_types = ['KfkQ', 'TstrQ', 'ESInd', 'JDBC'];
    if (term !== undefined) {
      if (term === 'zookeeper') {
        return value.filter((entity)=> entity.type.indexOf('ZKCoord') > -1);
      } else {
        var index = stream_types.indexOf(term);
        var term_type = service_types[index];
        return value.filter((entity)=> entity.type.indexOf(term_type) > -1);
      }
    } else {
      return value;
    }
  }
}
