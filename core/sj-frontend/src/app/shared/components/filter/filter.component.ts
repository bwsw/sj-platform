import { Component, Input, Output, OnInit, EventEmitter } from '@angular/core';

@Component({
  selector: 'sj-filter',
  templateUrl: 'filter.component.html'
})
export class FilterComponent implements OnInit {

  @Input() public filterList: string[];
  @Output() public update = new EventEmitter();

  public ngOnInit() {
    this.update.emit('');
  }
}
