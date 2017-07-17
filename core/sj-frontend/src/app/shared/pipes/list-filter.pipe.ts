import { Pipe, PipeTransform } from '@angular/core';

interface IFilterObject {
  name: string;
  type: string;
}

@Pipe({
  name: 'listFilter'
})
export class ListFilterPipe implements PipeTransform {
  public transform(value: [any], term: IFilterObject) {
    if (term !== undefined && value !== undefined && value.length > 0) {
      const nameFilter = term.name;
      const typeFilter = term.type;
      let result: any[] = value;
      if (typeFilter) {
        if (value[0].type !== undefined) {
          result = result.filter((entity) => entity.type.toLowerCase().indexOf(typeFilter.toLowerCase()) > -1);
        } else if (value[0].moduleType !== undefined) {
          result = result.filter((entity) => entity.moduleType.toLowerCase().indexOf(typeFilter.toLowerCase()) > -1);
        } else if (value[0].domain !== undefined) {
          result = result.filter((entity) => entity.domain.toLowerCase().indexOf(typeFilter.toLowerCase()) > -1);
        }
      }
      if (nameFilter) {
        if (value[0].name !== undefined) {
          result = result.filter((entity) => entity.name.toLowerCase().indexOf(nameFilter.toLowerCase()) > -1);
        } else if (value[0].moduleName !== undefined) {
          result = result.filter((entity) => entity.moduleName.toLowerCase().indexOf(nameFilter.toLowerCase()) > -1);
        }
      }
      return result;
    } else {
      return value;
    }

  }
}
