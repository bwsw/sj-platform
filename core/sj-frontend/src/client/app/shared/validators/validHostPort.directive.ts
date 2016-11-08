import { Directive, Input, OnChanges, SimpleChanges } from '@angular/core';
import { AbstractControl, NG_VALIDATORS, Validator, ValidatorFn, Validators } from '@angular/forms';

export function validHostPortValidator(): ValidatorFn {
  return (control: AbstractControl): {[key: string]: any} => {
    const val = control.value;
    let success: boolean = true;
    if (val) {
      success = new RegExp('[^\:]+:[0-9]').test(val);
    }
    return success ? null : {'validHostPort': {val}};
  };
}

@Directive({
  selector: '[validHostPort]',
  providers: [{provide: NG_VALIDATORS, useExisting: ValidHostPortDirective, multi: true}]
})
export class ValidHostPortDirective implements Validator, OnChanges {
  @Input() validHostPort: string;
  private valFn = Validators.nullValidator;

  ngOnChanges(changes: SimpleChanges): void {
    const change = changes['validHostPort'];
    if (change) {
      this.valFn = validHostPortValidator();
    } else {
      this.valFn = Validators.nullValidator;
    }
  }

  validate(control: AbstractControl): {[key: string]: any} {
    return this.valFn(control);
  }
}
