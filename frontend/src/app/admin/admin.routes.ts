import { Routes } from '@angular/router';
import { AdminDashboardComponent } from './admin-dashboard/admin-dashboard.component';
import { AdminTransactionsComponent } from './admin-transactions/admin-transactions.component';
import { AdminKYCComponent } from './admin-kyc/admin-kyc.component';
import { AdminFraudComponent } from './admin-fraud/admin-fraud.component';
import { AdminDisputesComponent } from './admin-disputes/admin-disputes.component';
import { AdminRewardsComponent } from './admin-rewards/admin-rewards.component';
import { AdminAnalyticsComponent } from './admin-analytics/admin-analytics.component';

export const adminRoutes: Routes = [
  { path: 'dashboard', component: AdminDashboardComponent },
  { path: 'transactions', component: AdminTransactionsComponent },
  { path: 'kyc', component: AdminKYCComponent },
  { path: 'fraud', component: AdminFraudComponent },
  { path: 'disputes', component: AdminDisputesComponent },
  { path: 'rewards', component: AdminRewardsComponent },
  { path: 'analytics', component: AdminAnalyticsComponent },
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
];
