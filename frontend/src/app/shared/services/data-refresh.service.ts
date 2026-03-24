import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class DataRefreshService {
  private transactionsRefresh$ = new Subject<void>();
  private notificationsRefresh$ = new Subject<void>();
  private balanceRefresh$ = new Subject<void>();
  private globalRefresh$ = new Subject<void>();

  getTransactionsRefresh$ = this.transactionsRefresh$.asObservable();
  getNotificationsRefresh$ = this.notificationsRefresh$.asObservable();
  getBalanceRefresh$ = this.balanceRefresh$.asObservable();
  getGlobalRefresh$ = this.globalRefresh$.asObservable();

  refreshTransactions(): void {
    this.transactionsRefresh$.next();
  }

  refreshNotifications(): void {
    this.notificationsRefresh$.next();
  }

  refreshBalance(): void {
    this.balanceRefresh$.next();
  }

  refreshAll(): void {
    this.globalRefresh$.next();
  }
}

