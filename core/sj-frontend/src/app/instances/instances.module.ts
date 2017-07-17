import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { InstancesComponent } from './instances.component';
import { SharedModule } from '../shared/shared.module';
import { ValidJsonDirective } from '../shared/validators/validJson.directive';
import { BrowserModule } from '@angular/platform-browser';
import { ModalModule } from 'ngx-bootstrap';

@NgModule({
  imports: [
    CommonModule,
    SharedModule,
    BrowserModule,
    ModalModule
  ],
  declarations: [
    InstancesComponent,
    ValidJsonDirective
  ],
  exports: [
    InstancesComponent,
    ValidJsonDirective
  ]
})
export class InstancesModule {
}
