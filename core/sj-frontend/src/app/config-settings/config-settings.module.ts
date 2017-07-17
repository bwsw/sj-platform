import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { ConfigSettingsComponent } from './config-settings.component';
import { SharedModule } from '../shared/shared.module';
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
    ConfigSettingsComponent
  ],
  exports: [
    ConfigSettingsComponent
  ]
})
export class ConfigSettingsModule {
}

