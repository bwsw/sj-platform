import {Component, Input, OnDestroy} from '@angular/core';

@Component({
  moduleId: module.id,
  selector: 'sj-spinner',
  templateUrl: './spinner.component.html',
  styleUrls: ['./spinner.component.css']
})

export class SpinnerComponent implements OnDestroy {
  visible: boolean = false;
  timeout: any;

  @Input()
  public delay: number = 0;

  @Input()
  public set isRunning(value: boolean) {
    if (!value) {
      this.cancel();
      this.visible = false;
    }

    if (this.timeout) {
      return;
    }

    this.timeout = setTimeout(() => {
      this.visible = value;
      this.cancel();
    }, this.delay);
  }

  cancel(): void {
    clearTimeout(this.timeout);
    this.timeout = undefined;
  }

  ngOnDestroy(): any {
    this.cancel();
  }
}
