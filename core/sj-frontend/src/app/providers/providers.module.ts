import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BrowserModule } from '@angular/platform-browser';

import { ProvidersComponent } from './providers.component';
import { SharedModule } from '../shared/shared.module';
import { ModalModule } from 'ngx-bootstrap';

@NgModule({
  imports: [
    CommonModule,
    BrowserModule,
    SharedModule,
    ModalModule.forRoot()
  ],
  declarations: [
    ProvidersComponent
  ],
  exports: [
    ProvidersComponent
  ]
})
export class ProvidersModule {
}
