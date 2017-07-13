import { Pipe, PipeTransform } from '@angular/core';
import { ServiceModel } from '../../services/service.model';

@Pipe({
  name: 'serviceFilter'
})
export class ServiceFilterPipe implements PipeTransform {
  public transform(value: [ServiceModel], term: string) {
    const stream_types = ['stream.kafka', 'stream.t-stream', 'elasticsearch-output', 'jdbc-output'];
    const service_types = ['KfkQ', 'TstrQ', 'ESInd', 'JDBC'];
    if (term !== undefined) {
      if (term === 'zookeeper') {
        return value.filter((entity) => entity.type.indexOf('ZKCoord') > -1);
      } else {
        const index = stream_types.indexOf(term);
        const term_type = service_types[index];
        return value.filter((entity) => entity.type.indexOf(term_type) > -1);
      }
    } else {
      return value;
    }
  }
}
