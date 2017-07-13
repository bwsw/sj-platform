import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'orderBy'
})
export class OrderByPipe implements PipeTransform {
  public transform(obj: any, orderField: string): any {
    let orderType = 'ASC';
    if (orderField[0] === '-') {
      orderField = orderField.substring(1);
      orderType = 'DESC';
    }
    if (typeof obj !== 'undefined') {
      obj.sort(function (a: any, b: any) {
        const nameA = a[orderField].toUpperCase();
        const nameB = b[orderField].toUpperCase();
        if (orderType === 'ASC') {
          if (nameA < nameB) {
            return -1;
          }
          if (nameA > nameB) {
            return 1;
          }
          return 0;
        } else {
          if (nameA < nameB) {
            return 1;
          }
          if (nameA > nameB) {
            return -1;
          }
          return 0;
        }
      });
    }
    return obj;
  }
}
