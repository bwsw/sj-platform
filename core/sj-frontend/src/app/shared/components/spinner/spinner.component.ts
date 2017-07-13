import { Component, Input, OnDestroy } from '@angular/core';

@Component({
  selector: 'sj-spinner',
  templateUrl: 'spinner.component.html',
  styleUrls: ['spinner.component.scss']
})

export class SpinnerComponent implements OnDestroy {
  @Input() public delay: number = 0;
  @Input() public progress: boolean = false;

  private _visible: boolean = false;
  private _timeout: any;

  public get visible(): boolean {
    return this._visible;
  }

  public get timeout(): any {
    return this._timeout;
  }

  @Input()
  public set isRunning(value: boolean) {
    if (!value) {
      this.cancel();
      this._visible = false;
    }

    if (this._timeout) {
      return;
    }

    this._timeout = setTimeout(() => {
      this._visible = value;
      this.cancel();
    }, this.delay);
  }

  cancel(): void {
    clearTimeout(this._timeout);
    this._timeout = undefined;
  }

  ngOnDestroy(): any {
    this.cancel();
  }
}
