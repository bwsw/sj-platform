import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { InstancesComponent } from './instances.component';
import { SharedModule } from '../shared/shared.module';

@NgModule({
  imports: [
    CommonModule,
    SharedModule
  ],
  declarations: [
    InstancesComponent
  ],
  exports: [
    InstancesComponent
  ]
})
export class InstancesModule {
}
