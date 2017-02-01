import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { CustomComponent } from './custom.component';
import { SharedModule } from '../shared/shared.module';

@NgModule({
  imports: [
    CommonModule,
    SharedModule
  ],
  declarations: [
    CustomComponent
  ],
  exports: [
    CustomComponent
  ]
})
export class CustomModule {
}
