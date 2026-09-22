import { Routes } from '@angular/router';
import { MainLayoutComponent } from './layout/main-layout/main-layout';
import { HomeComponent } from './pages/home/home';
import { SimuladorComponent } from './pages/simulador/simulador';
import { EmpresasComponent } from './pages/empresas/empresas';
import { ParametrosComponent } from './pages/parametros/parametros';
import { LoginComponent } from './pages/login/login';
import { ParametrosPadraoComponent } from './pages/parametros-padrao/parametros-padrao';
import { UsuariosAdminComponent } from './pages/usuarios-admin/usuarios-admin';
import { AuthGuard } from './services/auth.guard';
import { BankingComponent } from './pages/banking/banking';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  {
    path: '',
    component: MainLayoutComponent,
    canActivate: [AuthGuard],
    children: [
      { path: 'banking', component: BankingComponent },
      { path: 'home', component: HomeComponent },
      { path: 'simulador', component: SimuladorComponent },
      { path: 'empresas', component: EmpresasComponent },
      { path: 'parametros', component: ParametrosComponent },
      { path: 'parametros-padrao', component: ParametrosPadraoComponent },
      { path: 'usuarios-admin', component: UsuariosAdminComponent },
      { path: 'dashboard', redirectTo: 'home', pathMatch: 'full' }
    ]
  }
];
