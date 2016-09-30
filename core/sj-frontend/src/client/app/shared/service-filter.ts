import {Pipe, PipeTransform} from '@angular/core';
import {Service} from '../services/service';

@Pipe({
  name: 'serviceFilter'
})
export class ServiceFilter implements PipeTransform {
  transform(value:[Service], term:string) {
    var stream_types = ['stream.kafka', 'stream.t-stream', 'elasticsearch-output'];
    var service_types = ['KfkQ', 'TstrQ', 'ESInd'];
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
