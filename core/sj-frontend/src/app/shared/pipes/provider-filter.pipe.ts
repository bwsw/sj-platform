import { Pipe, PipeTransform } from '@angular/core';
import { ProviderModel } from '../../providers/provider.model';

@Pipe({
  name: 'providerFilter'
})
export class ProviderFilterPipe implements PipeTransform {
  public transform(value: [ProviderModel], term: string) {
    const service_types = ['CassDB', 'ESInd', 'KfkQ', 'TstrQ', 'ZKCoord', 'RdsCoord', 'ArspkDB', 'JDBC'];
    const provider_types = ['cassandra', 'ES', 'kafka', 'zookeeper', 'zookeeper', 'redis', 'aerospike', 'JDBC'];
    if (term !== undefined && value !== undefined) {
      if (term === 'DataProvider') {
        return value.filter(function (item) {
          return item.type === 'cassandra' || item.type === 'aerospike';
        });
      } else if (term === 'LockProvider') {
        return value.filter(function (item) {
          return item.type === 'zookeeper' || item.type === 'redis';
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
