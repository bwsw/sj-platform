import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { APP_BASE_HREF } from '@angular/common';
import { RouterModule } from '@angular/router';
import { HttpModule } from '@angular/http';
import { ReactiveFormsModule } from '@angular/forms';

import { routes } from './app.routes';
import { AppComponent } from './app.component';
import { SharedModule } from './shared/shared.module';
import { InstancesModule } from './instances/instances.module';
import { ModulesModule } from './modules/modules.module';
import { ProvidersModule } from './providers/providers.module';
import { ServicesModule } from './services/services.module';
import { StreamsModule } from './streams/streams.module';
import { ConfigSettingsModule } from './config-settings/config-settings.module';


@NgModule({
  imports: [
    BrowserModule,
    HttpModule,
    ReactiveFormsModule,
    RouterModule.forRoot(routes),
    SharedModule.forRoot(),
    InstancesModule,
    ModulesModule,
    ProvidersModule,
    ServicesModule,
    StreamsModule,
    ConfigSettingsModule
  ],
  declarations: [
    AppComponent
  ],
  providers: [{
    provide: APP_BASE_HREF,
    useValue: '<%= APP_BASE %>'
  }],
  bootstrap: [
    AppComponent
  ]
})
export class AppModule {
}
