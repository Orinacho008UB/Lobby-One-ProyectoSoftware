import { NgModule, provideBrowserGlobalErrorListeners, provideZoneChangeDetection } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HttpClientModule } from '@angular/common/http';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { AppRouterModule } from './app-router/app-router-module';
import { App } from './app';
import { Reservas } from './reservas/reservas';
import { Habitaciones } from './habitaciones/habitaciones';
import { Servicios } from './servicios/servicios';
import { Ofertas } from './ofertas/ofertas';
import { Perfiles } from './perfiles/perfiles';

@NgModule({
  declarations: [App, Reservas, Habitaciones, Servicios, Ofertas, Perfiles],
  imports: [BrowserModule, HttpClientModule, FormsModule, ReactiveFormsModule, AppRouterModule],
  providers: [provideZoneChangeDetection(), provideBrowserGlobalErrorListeners()],
  bootstrap: [App],
})
export class AppModule {}
