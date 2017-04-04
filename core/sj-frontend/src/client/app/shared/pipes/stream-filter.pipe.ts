import { Pipe, PipeTransform } from '@angular/core';

import { StreamModel } from '../models/stream.model';

@Pipe({
  name: 'streamFilter'
})
export class StreamFilterPipe implements PipeTransform {
  public transform(value: [StreamModel], terms: string) {
    let result: any[] = [];
    let terms_array = terms.split(',');
    for (let term of terms_array) {
      let stream_types = ['stream.kafka', 'stream.t-stream', 'elasticsearch-output', 'jdbc-output'];
      if (term !== undefined) {
        let index = stream_types.indexOf(term);
        let term_type = stream_types[index];
        result = result.concat(value.filter((entity) => entity.type.indexOf(term_type) > -1));
      } else {
        result = result.concat(value);
      }
    }
    return result;
  }
}
