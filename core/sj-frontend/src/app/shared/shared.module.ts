import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { AlertModule } from 'ngx-bootstrap';
import { CollapseModule } from 'ngx-bootstrap';

import { ToolbarComponent } from './components/toolbar/toolbar.component';
import { NavbarComponent } from './components/navbar/navbar.component';
import { SearchBoxComponent } from './components/searchBox/search-box.component';
import { FilterComponent } from './components/filter/filter.component';
import { ListFilterPipe } from './pipes/list-filter.pipe';
import { OrderByPipe } from './pipes/order-by.pipe';
import { ServiceFilterPipe } from './pipes/service-filter.pipe';
import { ProviderFilterPipe } from './pipes/provider-filter.pipe';
import { StreamFilterPipe } from './pipes/stream-filter.pipe';
import { FileSizePipe } from './pipes/file-size.pipe';
import { SpinnerComponent } from './components/spinner/spinner.component';
import { BreadcrumbsComponent } from './components/breadcrumbs/breadcrumbs.component';
import { FooterComponent } from './components/footer/footer.component';
import { AlertsComponent } from './components/alerts/alerts.component';

@NgModule({
  imports: [
    CommonModule,
    RouterModule,
    AlertModule.forRoot(),
    CollapseModule.forRoot()
  ],
  declarations: [
    // Components
    ToolbarComponent,
    NavbarComponent,
    SearchBoxComponent,
    SpinnerComponent,
    BreadcrumbsComponent,
    FooterComponent,
    FilterComponent,
    AlertsComponent,
    // Pipes
    ListFilterPipe,
    OrderByPipe,
    ServiceFilterPipe,
    ProviderFilterPipe,
    StreamFilterPipe,
    FileSizePipe
  ],
  exports: [
    // Components
    ToolbarComponent,
    NavbarComponent,
    SearchBoxComponent,
    SpinnerComponent,
    BreadcrumbsComponent,
    FooterComponent,
    FilterComponent,
    AlertsComponent,
    // Pipes
    ListFilterPipe,
    OrderByPipe,
    ServiceFilterPipe,
    ProviderFilterPipe,
    StreamFilterPipe,
    FileSizePipe,
    // Modules
    CommonModule,
    FormsModule,
    RouterModule
  ]
})
export class SharedModule { }
