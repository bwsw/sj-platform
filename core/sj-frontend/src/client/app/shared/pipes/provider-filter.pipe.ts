import { Pipe, PipeTransform } from '@angular/core';
import { ProviderModel } from '../models/provider.model';

@Pipe({
  name: 'providerFilter'
})
export class ProviderFilterPipe implements PipeTransform {
  public transform(value: [ProviderModel], term: string) {
    var service_types = ['CassDB', 'ESInd', 'KfkQ', 'TstrQ', 'ZKCoord', 'RdsCoord', 'ArspkDB'];
    var provider_types = ['cassandra', 'ES', 'kafka', '', 'zookeeper', 'redis', 'aerospike'];
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
        var index = service_types.indexOf(term);
        var term_type = provider_types[index];
        return value.filter((entity)=> entity.type.indexOf(term_type) > -1);
      }
    } else {
      return value;
    }

  }
}
