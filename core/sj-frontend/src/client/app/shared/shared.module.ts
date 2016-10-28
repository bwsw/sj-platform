import { NgModule, ModuleWithProviders } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { ToolbarComponent } from './components/toolbar/toolbar.component';
import { NavbarComponent } from './components/navbar/navbar.component';
import { SearchBoxComponent } from './components/searchBox/search-box.component';
import { ListFilterPipe } from './pipes/list-filter.pipe';
import { OrderByPipe } from './pipes/order-by.pipe';
import { ServiceFilterPipe } from './pipes/service-filter.pipe';
import { ProviderFilterPipe } from './pipes/provider-filter.pipe';
import { InstancesService } from './services/instances.service';
import { ModulesService } from './services/modules.service';
import { ProvidersService } from './services/providers.service';
import { ServicesService } from './services/services.service';
import { SpinnerComponent } from '../shared/spinner/spinner.component';
import { StreamsService } from './services/streams.service';
import { Ng2BootstrapModule } from 'ng2-bootstrap';

@NgModule({
  imports: [
    CommonModule,
    RouterModule,
    Ng2BootstrapModule
  ],
  declarations: [
    // Components
    ToolbarComponent,
    NavbarComponent,
    SearchBoxComponent,
    SpinnerComponent,
    // Pipes
    ListFilterPipe,
    OrderByPipe,
    ServiceFilterPipe,
    ProviderFilterPipe
  ],
  providers: [
    InstancesService,
    ModulesService,
    ProvidersService,
    ServicesService,
    StreamsService
  ],
  exports: [
    // Components
    ToolbarComponent,
    NavbarComponent,
    SearchBoxComponent,
    SpinnerComponent,
    // Pipes
    ListFilterPipe,
    OrderByPipe,
    ServiceFilterPipe,
    ProviderFilterPipe,
    // Modules
    CommonModule,
    FormsModule,
    RouterModule,
    Ng2BootstrapModule
  ]
})
export class SharedModule {
  static forRoot(): ModuleWithProviders {
    return {
      ngModule: SharedModule,
      providers: [
        InstancesService,
        ModulesService,
        ProvidersService,
        ServicesService,
        StreamsService
      ]
    };
  }
}
