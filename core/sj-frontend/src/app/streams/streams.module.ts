import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BrowserModule } from '@angular/platform-browser';

import { SharedModule } from '../shared/shared.module';
import { ModalModule } from 'ngx-bootstrap';
import {StreamsComponent} from './streams.component';

@NgModule({
  imports: [
    CommonModule,
    SharedModule,
    BrowserModule,
    ModalModule
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
