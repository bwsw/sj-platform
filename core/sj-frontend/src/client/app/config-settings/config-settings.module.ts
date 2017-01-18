import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { ConfigSettingsComponent } from './config-settings.component';
import { SharedModule } from '../shared/shared.module';

@NgModule({
  imports: [
    CommonModule,
    SharedModule
  ],
  declarations: [
    ConfigSettingsComponent
  ],
  exports: [
    ConfigSettingsComponent
  ]
})
export class ConfigSettingsModule {
}

