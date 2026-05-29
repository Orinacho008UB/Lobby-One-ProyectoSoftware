import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

// Componente "App Router" (C4 Nivel 3): enrutamiento de la SPA.
// Scaffolding vacio: las rutas se definiran al implementar el frontend.
const routes: Routes = [];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRouterModule { }
