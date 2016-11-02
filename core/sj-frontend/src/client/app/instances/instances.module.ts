import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { InstancesComponent } from './instances.component';
import { SharedModule } from '../shared/shared.module';
import { ValidJsonDirective } from '../shared/validators/validJson.directive';

@NgModule({
  imports: [
    CommonModule,
    SharedModule,

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
