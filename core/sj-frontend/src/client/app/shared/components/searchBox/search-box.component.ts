import { Component, Output, OnInit, EventEmitter } from '@angular/core';

@Component({
  moduleId: module.id,
  selector: 'sj-search-box',
  templateUrl: 'search-box.component.html'
})
export class SearchBoxComponent implements OnInit {
  @Output() public update = new EventEmitter();

  public ngOnInit() {
    this.update.emit('');
  }
}
