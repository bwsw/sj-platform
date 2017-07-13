import { APP_BASE_HREF } from '@angular/common';
import { BrowserModule } from '@angular/platform-browser';
import { CommonModule } from '@angular/common';
import { NgModule, Injector } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpModule } from '@angular/http';
import { RouterModule } from '@angular/router';
import { ModalModule } from 'ngx-bootstrap';
import { CollapseModule } from 'ngx-bootstrap';

import { SharedModule } from './shared/shared.module';

import { AppComponent } from './app.component';
import { ProvidersModule } from './providers';
import { routes } from './app.routes';
import { Locator } from './shared';
import { ServicesModule } from './services';
import { StreamsModule } from './streams';
import { ModulesModule } from './modules';
import { InstancesModule } from './instances';
import { ConfigSettingsModule } from './config-settings';
import { CustomModule } from './custom';


@NgModule({
  declarations: [
    AppComponent
  ],
  imports: [
    BrowserModule,
    CommonModule,
    FormsModule,
    HttpModule,
    ProvidersModule,
    ServicesModule,
    StreamsModule,
    ModulesModule,
    InstancesModule,
    ConfigSettingsModule,
    CustomModule,
    RouterModule.forRoot(routes),
    ModalModule.forRoot(),
    CollapseModule.forRoot(),
    SharedModule,
  ],
  providers: [{provide: APP_BASE_HREF, useValue : '/' }],
  bootstrap: [AppComponent]
})
export class AppModule {

  constructor(
    private injector: Injector
  ) {
    Locator.injector = this.injector;
  }
}
