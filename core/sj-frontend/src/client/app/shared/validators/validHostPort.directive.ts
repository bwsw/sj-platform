import { Directive } from '@angular/core';
import { AbstractControl, NG_VALIDATORS, Validator } from '@angular/forms';

@Directive({
  selector: '[validHostPort]',
  providers: [{provide: NG_VALIDATORS, useExisting: ValidHostPortDirective, multi: true}]
})
export class ValidHostPortDirective implements Validator {

  public validate(control: AbstractControl): { [key: string]: any } {
    const val = control.value;
    let success: boolean = true;
    if (val) {
      success = new RegExp('[^\:]+:[0-9]').test(val);
    }
    if (!success) {  return {validHostPort: success}; }
    return null;
  }
}
