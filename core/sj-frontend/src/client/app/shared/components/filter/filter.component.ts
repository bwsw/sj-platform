import { Component, Input, Output, OnInit, EventEmitter } from '@angular/core';

@Component({
  moduleId: module.id,
  selector: 'sj-filter',
  templateUrl: 'filter.component.html',
  styleUrls: ['../searchBox/search-box.component.css']
})
export class FilterComponent implements OnInit {

  @Input() public filterList: string[];
  @Output() public update = new EventEmitter();

  public ngOnInit() {
    this.update.emit('');
  }
}
