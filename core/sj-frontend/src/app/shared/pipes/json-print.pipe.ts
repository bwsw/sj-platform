import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'jsonPrint'
})
export class JsonPrintPipe implements PipeTransform {
  public transform(jsonObject: Object) {
    return JSON.stringify(jsonObject, null, 2)
      .replace(/ /g, '&nbsp;')
      .replace(/\n/g, '<br/>');
  }
}
