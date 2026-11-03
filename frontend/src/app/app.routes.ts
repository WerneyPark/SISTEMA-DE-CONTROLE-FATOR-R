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
import { ContasAReceberComponent } from './pages/contas-a-receber/contas-a-receber';
import { BankingComponent } from './pages/banking/banking';
import { PayrollComponent } from './pages/payroll/payroll';
import { ReportsComponent } from './pages/reports/reports';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  {
    path: '',
    component: MainLayoutComponent,
    canActivate: [AuthGuard],
    children: [
      { path: 'home', component: HomeComponent },
      { path: 'simulador', component: SimuladorComponent },
      { path: 'contas-a-receber', component: ContasAReceberComponent },
      { path: 'banking', component: BankingComponent },
      { path: 'payroll', component: PayrollComponent },
      { path: 'reports', component: ReportsComponent },
      { path: 'empresas', component: EmpresasComponent },
      { path: 'parametros', component: ParametrosComponent },
      { path: 'parametros-padrao', component: ParametrosPadraoComponent },
      { path: 'usuarios-admin', component: UsuariosAdminComponent },
      { path: 'dashboard', redirectTo: 'home', pathMatch: 'full' }
    ]
  }
];
