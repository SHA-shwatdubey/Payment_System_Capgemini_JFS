import { Routes } from '@angular/router';
import { authGuard } from './shared/guards/auth.guard';
import { adminGuard } from './shared/guards/admin.guard';
import { ShellComponent } from './shared/components/shell/shell.component';
import { LoginComponent } from './auth/login/login.component';
import { SignupComponent } from './auth/signup/signup.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { AddMoneyComponent } from './wallet/add-money/add-money.component';
import { TransferMoneyComponent } from './wallet/transfer-money/transfer-money.component';
import { TransactionsComponent } from './transactions/transactions.component';
import { RewardsComponent } from './rewards/rewards.component';
import { KycComponent } from './kyc/kyc.component';
import { AdminDashboardComponent } from './admin/admin-dashboard/admin-dashboard.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'signup', component: SignupComponent },
  {
	path: '',
	component: ShellComponent,
	canActivate: [authGuard],
	children: [
	  { path: 'dashboard', component: DashboardComponent },
	  { path: 'wallet/add-money', component: AddMoneyComponent },
	  { path: 'wallet/transfer', component: TransferMoneyComponent },
	  { path: 'transactions', component: TransactionsComponent },
	  { path: 'rewards', component: RewardsComponent },
	  { path: 'kyc', component: KycComponent },
	  { path: 'admin', component: AdminDashboardComponent, canActivate: [adminGuard] },
	  { path: '', pathMatch: 'full', redirectTo: 'dashboard' }
	]
  },
  { path: '**', redirectTo: 'dashboard' }
];
