import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { StreamsComponent } from './streams.component';
import { SharedModule } from '../shared/shared.module';

@NgModule({
  imports: [
    CommonModule,
    SharedModule
  ],
  declarations: [
    StreamsComponent
  ],
  exports: [
    StreamsComponent
  ]
})
export class StreamsModule {
}
