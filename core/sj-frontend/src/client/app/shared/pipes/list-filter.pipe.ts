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
      let nameFilter = term.name;
      let typeFilter = term.type;
      let result: any[] = value;
      if(typeFilter) {
        if (value[0].type !== undefined) {
          result = result.filter((entity)=> entity.type.toLowerCase().indexOf(typeFilter.toLowerCase()) > -1);
        } else if (value[0]['stream-type'] !== undefined) {
          result = result.filter((entity)=> entity['stream-type'].toLowerCase().indexOf(typeFilter.toLowerCase()) > -1);
        } else if (value[0]['module-type'] !== undefined) {
          result = result.filter((entity)=> entity['module-type'].toLowerCase().indexOf(typeFilter.toLowerCase()) > -1);
        } else if (value[0].domain !== undefined) {
          result = result.filter((entity)=> entity.domain.toLowerCase().indexOf(typeFilter.toLowerCase()) > -1);
        }
      }
      if(nameFilter) {
        if (value[0].name !== undefined) {
          result = result.filter((entity)=> entity.name.toLowerCase().indexOf(nameFilter.toLowerCase()) > -1);
        } else if (value[0]['module-name'] !== undefined) {
          result = result.filter((entity)=> entity['module-name'].toLowerCase().indexOf(nameFilter.toLowerCase()) > -1);
        }
      }
      return result;
    } else {
      return value;
    }

  }
}
