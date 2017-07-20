import { Pipe, PipeTransform } from '@angular/core';
import { ServiceModel } from '../../services/service.model';

@Pipe({
  name: 'serviceFilter'
})
export class ServiceFilterPipe implements PipeTransform {
  public transform(value: [ServiceModel], term: string) {
    const stream_types = ['stream.apache-kafka', 'stream.t-streams', 'stream.elasticsearch', 'stream.sql-database', 'stream.restful'];
    const service_types = ['service.apache-kafka', 'service.t-streams', 'service.elasticsearch', 'service.sql-database', 'service.restful'];
    if (term !== undefined) {
      if (term === 'service.apache-zookeeper') {
        return value.filter((entity) => entity.type.indexOf('service.apache-zookeeper') > -1);
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
