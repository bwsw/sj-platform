import { Pipe, PipeTransform } from '@angular/core';
import { StreamModel } from '../../streams/stream.model';

@Pipe({
  name: 'streamFilter'
})
export class StreamFilterPipe implements PipeTransform {
  public transform(value: [StreamModel], terms: string) {
    let result: any[] = [];
    const terms_array = terms.split(',');
    for (const term of terms_array) {
      const stream_types = ['stream.kafka', 'stream.t-stream', 'elasticsearch-output', 'jdbc-output'];
      if (term !== undefined) {
        const index = stream_types.indexOf(term);
        const term_type = stream_types[index];
        result = result.concat(value.filter((entity) => entity.type.indexOf(term_type) > -1));
      } else {
        result = result.concat(value);
      }
    }
    return result;
  }
}
