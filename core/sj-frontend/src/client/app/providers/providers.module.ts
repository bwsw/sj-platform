import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { ProvidersComponent } from './providers.component';
import { SharedModule } from '../shared/shared.module';
import { ValidHostPortDirective } from '../shared/validators/validHostPort.directive';

@NgModule({
  imports: [
    CommonModule,
    SharedModule
  ],
  declarations: [
    ProvidersComponent,
    ValidHostPortDirective
  ],
  exports: [
    ProvidersComponent,
    ValidHostPortDirective
  ]
})
export class ProvidersModule {
}
