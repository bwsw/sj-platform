import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'fileSize'
})
export class FileSizePipe implements PipeTransform {
  public transform(size: number, decimals: number) {
    if (size === 0) return '0 Bytes';
    let k = 1000;
    let dm = decimals + 1 || 3;
    let sizes = ['Bytes', 'KB', 'MB'];
    let i = Math.floor(Math.log(size) / Math.log(k));
    return parseFloat((size / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
  }
}
