import { CommonModule } from '@angular/common';
import { Component, OnInit, OnDestroy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { EMPTY, Subject, catchError, distinctUntilChanged, filter, forkJoin, map, merge, of, switchMap, tap, timeout, timer, takeUntil } from 'rxjs';
import { ApiService } from '../shared/services/api.service';
import { SessionService } from '../shared/services/session.service';
import { DataRefreshService } from '../shared/services/data-refresh.service';
import { DashboardCacheService } from '../shared/services/dashboard-cache.service';
import { NotificationMessage, RewardsAccount, TransactionItem, WalletBalanceResponse } from '../shared/models/app.models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit, OnDestroy {
  loading = true;
  loadingError: string | null = null;
  balance = 0;
  rewards: RewardsAccount | null = null;
  recentTransactions: TransactionItem[] = [];
  recentNotifications: NotificationMessage[] = [];
  private currentUserId: number | null = null;
  private firstLoadCompleted = false;
  private requestInFlight = false;
  private readonly destroy$ = new Subject<void>();

  constructor(
    private readonly api: ApiService,
    private readonly session: SessionService,
    private readonly dataRefresh: DataRefreshService,
    private readonly dashboardCache: DashboardCacheService
  ) {
    // RESTORE FROM CACHE if available
    const cachedData = this.dashboardCache.getCache();
    if (cachedData && cachedData.userId === this.session.currentUser?.userId) {
      this.balance = cachedData.balance;
      this.rewards = cachedData.rewards;
      this.recentTransactions = cachedData.recentTransactions;
      this.recentNotifications = cachedData.recentNotifications;
      this.loading = false;
      this.firstLoadCompleted = true;
    }

    // IMMEDIATE: If user is already loaded, start immediately
    const existingUser = this.session.currentUser;
    if (existingUser?.userId && existingUser.userId > 0) {
      this.currentUserId = existingUser.userId;
      if (!cachedData) {
        this.loading = true;
      }
      this.loadDashboardData(this.currentUserId, !cachedData);
    }

    // Listen for refresh events
    merge(
      this.dataRefresh.getTransactionsRefresh$,
      this.dataRefresh.getNotificationsRefresh$,
      this.dataRefresh.getBalanceRefresh$,
      this.dataRefresh.getGlobalRefresh$
    )
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        if (this.currentUserId != null && this.currentUserId > 0) {
          this.loadDashboardData(this.currentUserId);
        }
      });
  }

  ngOnInit(): void {
    // Safety timeout to prevent infinite loading
    const loadingSafetyTimeout = setTimeout(() => {
      if (this.loading) {
        this.loading = false;
        this.loadingError = 'Data is taking longer than expected. Showing default view.';
      }
    }, 10000);

    // If user is already available, load immediately
    if (this.currentUserId && this.currentUserId > 0) {
      this.loading = true;
      this.loadDashboardData(this.currentUserId, true);
    }

    // Subscribe to user changes with proper destroy
    this.session.user$
      .pipe(
        map((user) => (user?.userId ?? null) as number | null),
        distinctUntilChanged(),
        filter((userId): userId is number => {
          const isValid = userId != null && Number.isFinite(userId) && userId > 0;
          if (!isValid) {
            this.balance = 0;
            this.rewards = null;
            this.recentTransactions = [];
            this.recentNotifications = [];
            this.loading = false;
            this.loadingError = null;
          }
          return isValid;
        }),
        switchMap((userId: number) => {
          this.currentUserId = userId;
          this.firstLoadCompleted = false;
          this.requestInFlight = false;
          this.loadingError = null;

          return timer(0, 15000).pipe(
            map((tick) => ({
              userId,
              showLoader: tick === 0 && !this.firstLoadCompleted,
              tick
            })),
            timeout(45000)
          );
        }),
        catchError((err: any) => {
          this.loading = false;
          this.loadingError = 'Connection error. Please try again.';
          return EMPTY;
        }),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: ({ userId, showLoader, tick }: { userId: number; showLoader: boolean; tick: number }) => {
          this.loadDashboardData(userId, showLoader);
        },
        error: (err: any) => {
          this.loading = false;
          this.loadingError = 'Failed to load dashboard';
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadDashboardData(userIdFromSession?: number, showLoader = false): void {
    const userId = userIdFromSession ?? this.session.currentUser?.userId;

    if (userId == null || !Number.isFinite(userId) || userId <= 0) {
      this.loading = false;
      this.requestInFlight = false;
      return;
    }

    if (this.requestInFlight) return;

    this.requestInFlight = true;
    if (showLoader) this.loading = true;
    if (this.firstLoadCompleted && !this.loading) this.loading = true;

    forkJoin({
      balance: this.api.getWalletBalance(userId).pipe(
        timeout(9000),
        catchError(() => of({ balance: 0 } as WalletBalanceResponse))
      ),
      rewards: this.api.getRewardsSummary(userId).pipe(
        timeout(9000),
        catchError(() => of({ points: 0, tier: 'SILVER' } as unknown as RewardsAccount))
      ),
      transactions: this.api.getTransactionsByUser(userId).pipe(
        timeout(9000),
        catchError(() => of([] as TransactionItem[]))
      ),
      notifications: this.api.getNotificationHistory(userId).pipe(
        timeout(9000),
        catchError(() => of([] as NotificationMessage[]))
      )
    })
      .pipe(
        timeout(12000),
        catchError(() => of({
          balance: { balance: 0 } as WalletBalanceResponse,
          rewards: { points: 0, tier: 'SILVER' } as unknown as RewardsAccount,
          transactions: [] as TransactionItem[],
          notifications: [] as NotificationMessage[]
        })),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (result) => {
          try {
            this.balance = Number(result.balance?.balance || 0);
            this.rewards = result.rewards;
            this.recentTransactions = (result.transactions ?? []).slice(0, 5);
            this.recentNotifications = (result.notifications ?? []).slice(0, 5);
            this.firstLoadCompleted = true;
            this.loadingError = null;

            // CACHE the data
            if (this.currentUserId) {
              this.dashboardCache.setCache({
                balance: this.balance,
                rewards: this.rewards,
                recentTransactions: this.recentTransactions,
                recentNotifications: this.recentNotifications,
                timestamp: Date.now(),
                userId: this.currentUserId
              });
            }
          } catch (err) {
            this.loadingError = 'Error processing data';
          } finally {
            this.requestInFlight = false;
            this.loading = false;
          }
        },
        error: () => {
          this.requestInFlight = false;
          this.loading = false;
          this.loadingError = 'Failed to load data';
        }
      });
  }
}
