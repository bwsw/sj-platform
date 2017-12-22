import { Component, Input } from '@angular/core';

@Component({
  selector: 'sj-extended-element',
  templateUrl: 'extended-element.component.html',
})
export class ExtendedElementComponent {
  @Input() public title: string;
  @Input() public content: string;

  public isCollapsed: boolean = true;
}
