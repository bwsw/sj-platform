import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'listFilter'
})
export class ListFilterPipe implements PipeTransform {
  public transform(value: [any], term: string) {
    if (term !== undefined && value !== undefined) {
      if (value[0]['module-name'] !== undefined) { //TODO Make better check
        return value.filter((entity)=> entity['module-name'].toLowerCase().indexOf(term.toLowerCase()) > -1);
      } else {
        return value.filter((entity)=> entity.name.toLowerCase().indexOf(term.toLowerCase()) > -1);
      }
    } else {
      return value;
    }

  }
}
