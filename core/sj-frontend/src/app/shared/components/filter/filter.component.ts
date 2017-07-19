import { Component, Input, Output, OnInit, EventEmitter } from '@angular/core';
import { TypeModel } from '../../model/type.model';

@Component({
  selector: 'sj-filter',
  templateUrl: 'filter.component.html'
})
export class FilterComponent implements OnInit {

  @Input() public filterList: TypeModel[];
  @Output() public update = new EventEmitter();

  public ngOnInit() {
    this.update.emit('');
  }
}
