import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { RewardsAccount, TransactionItem, NotificationMessage } from '../models/app.models';

export interface DashboardCache {
  balance: number;
  rewards: RewardsAccount | null;
  recentTransactions: TransactionItem[];
  recentNotifications: NotificationMessage[];
  timestamp: number;
  userId: number | null;
}

@Injectable({ providedIn: 'root' })
export class DashboardCacheService {
  private readonly cache = new BehaviorSubject<DashboardCache | null>(null);
  readonly cache$ = this.cache.asObservable();

  private readonly CACHE_VALIDITY_MS = 60000; // 60 seconds

  setCache(data: DashboardCache): void {
    console.log('💾 [DASHBOARD-CACHE] Caching dashboard data:', data);
    this.cache.next({
      ...data,
      timestamp: Date.now()
    });
  }

  getCache(): DashboardCache | null {
    const cached = this.cache.value;
    if (!cached) {
      console.log('💾 [DASHBOARD-CACHE] No cache available');
      return null;
    }

    const age = Date.now() - cached.timestamp;
    if (age > this.CACHE_VALIDITY_MS) {
      console.log('💾 [DASHBOARD-CACHE] Cache expired (age:', age, 'ms)');
      this.cache.next(null);
      return null;
    }

    console.log('💾 [DASHBOARD-CACHE] Using valid cache (age:', age, 'ms)');
    return cached;
  }

  clearCache(): void {
    console.log('💾 [DASHBOARD-CACHE] Clearing cache');
    this.cache.next(null);
  }
}

