import { Directive, Input, OnChanges, SimpleChanges } from '@angular/core';
import { AbstractControl, NG_VALIDATORS, Validator, ValidatorFn, Validators } from '@angular/forms';

export function validJsonValidator(): ValidatorFn {
  return (control: AbstractControl): {[key: string]: any} => {
    const val = control.value;
    let success: boolean = true;
    if (val) {
      try {
        JSON.parse(val);
      } catch (e) {
        success = false;
      }
    }
    return success ? null : {'validJson': {val}};
  };
}

@Directive({
  selector: '[validJson]',
  providers: [{provide: NG_VALIDATORS, useExisting: ValidJsonDirective, multi: true}]
})
export class ValidJsonDirective implements Validator, OnChanges {
  @Input() validJson: string;
  private valFn = Validators.nullValidator;

  ngOnChanges(changes: SimpleChanges): void {
    const change = changes['validJson'];
    if (change) {
      this.valFn = validJsonValidator();
    } else {
      this.valFn = Validators.nullValidator;
    }
  }

  validate(control: AbstractControl): {[key: string]: any} {
    return this.valFn(control);
  }
}
