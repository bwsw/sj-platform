import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { ProvidersComponent } from './providers.component';
import { SharedModule } from '../shared/shared.module';

@NgModule({
  imports: [
    CommonModule,
    SharedModule
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
