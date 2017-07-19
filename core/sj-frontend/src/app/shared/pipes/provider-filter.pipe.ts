import { Pipe, PipeTransform } from '@angular/core';
import { ProviderModel } from '../../providers/provider.model';

@Pipe({
  name: 'providerFilter'
})
export class ProviderFilterPipe implements PipeTransform {
  public transform(value: [ProviderModel], term: string) {
    const service_types = ['service.elasticsearch', 'service.apache-kafka', 'service.t-streams', 'service.apache-zookeeper', 'service.sql-database', 'service.restful'];
    const provider_types = ['provider.elasticsearch', 'provider.apache-kafka', 'provider.apache-zookeeper', 'provider.apache-zookeeper', 'provider.sql-database', 'provider.restful'];
    if (term !== undefined && value !== undefined) {
      if (term === 'LockProvider') {
        return value.filter(function (item) {
          return item.type === 'provider.apache-zookeeper';
        });
      } else {
        const index = service_types.indexOf(term);
        const term_type = provider_types[index];
        return value.filter((entity) => entity.type.indexOf(term_type) > -1);
      }
    } else {
      return value;
    }

  }
}
