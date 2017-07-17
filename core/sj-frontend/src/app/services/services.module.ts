import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BrowserModule } from '@angular/platform-browser';

import { ServicesComponent } from './services.component';
import { SharedModule } from '../shared/shared.module';
import { ModalModule } from 'ngx-bootstrap';

@NgModule({
  imports: [
    CommonModule,
    SharedModule,
    BrowserModule,
    ModalModule
  ],
  declarations: [
    ServicesComponent
  ],
  exports: [
    ServicesComponent
  ]
})
export class ServicesModule {
}
