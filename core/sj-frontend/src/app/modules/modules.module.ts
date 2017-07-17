import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BrowserModule } from '@angular/platform-browser';

import { SharedModule } from '../shared/shared.module';
import { ModalModule } from 'ngx-bootstrap';
import { ModulesComponent } from './modules.component';


@NgModule({
  imports: [
    CommonModule,
    SharedModule,
    BrowserModule,
    ModalModule
  ],
  declarations: [
    ModulesComponent
  ],
  exports: [
    ModulesComponent
  ]
})
export class ModulesModule {
}
