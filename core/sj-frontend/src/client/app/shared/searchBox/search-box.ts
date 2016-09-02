import {Component, Output, OnInit, EventEmitter} from '@angular/core';

@Component({
  moduleId: module.id,
  selector:'search-box',
  template: `<div class="right-inner-addon">
       <i class="fa fa-search" aria-hidden="true"></i>
      <input #input type="text" (input)="update.emit(input.value)" class="form-control" placeholder="Search" />
  </div>`,
  styleUrls: ['search-box.css'],
})

export class SearchBoxComponent implements OnInit {
  @Output() update = new EventEmitter();

  ngOnInit() {
    this.update.emit('');
  }
}
