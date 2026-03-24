import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, catchError, timeout, of, map, switchMap } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  NotificationMessage,
  NotificationStats,
  RewardCatalogItem,
  RewardsAccount,
  TransactionItem,
  UserProfile,
  WalletAccount,
  WalletBalanceResponse
} from '../models/app.models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly baseUrl = environment.apiBaseUrl;
  private readonly routes = {
    walletBalance: '/api/wallet/balance',
    walletTopup: '/api/wallet/topup',
    walletTransfer: '/api/wallet/transfer',
    walletTransactions: '/api/wallet/transactions',
    transactionsByUser: '/transactions/user',
    rewardsSummary: '/api/rewards/summary',
    rewardsCatalog: '/api/rewards/catalog',
    rewardsRedeem: '/api/rewards/redeem',
    userProfile: '/api/users',
    kycUpload: '/api/kyc/upload',
    kycPending: '/api/kyc/pending',
    kycStatus: '/api/kyc',
    adminDashboard: '/api/admin/dashboard',
    notificationsStats: '/api/notifications/admin/stats',
    notificationsHistory: '/api/notifications/history',
    notificationsHistoryAlt: '/notifications/history'
  } as const;

  constructor(private readonly http: HttpClient) { }

  getWalletBalance(userId: number): Observable<WalletBalanceResponse> {
    return this.http.get<WalletBalanceResponse>(`${this.baseUrl}${this.routes.walletBalance}`, {
      params: new HttpParams().set('userId', userId)
    }).pipe(
      timeout(8000),
      catchError((err) => {
        console.warn('⚠️ [API] Wallet balance failed, trying fallback:', err.message);
        return this.http.get<WalletBalanceResponse>(`${this.baseUrl}/wallet/balance`, {
          params: new HttpParams().set('userId', userId)
        }).pipe(
          timeout(8000),
          catchError(() => of({ balance: 0 } as WalletBalanceResponse))
        );
      })
    );
  }

  topup(userId: number, amount: number, method: string): Observable<WalletAccount> {
    return this.http.post<WalletAccount>(`${this.baseUrl}${this.routes.walletTopup}`, {
      userId,
      amount,
      paymentMethod: method
    });
  }

  transfer(fromUserId: number, toUserId: number, amount: number): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.baseUrl}${this.routes.walletTransfer}`, {
      fromUserId,
      toUserId,
      amount
    });
  }

  getWalletTransactions(userId: number): Observable<Array<Record<string, unknown>>> {
    return this.http.get<Array<Record<string, unknown>>>(`${this.baseUrl}${this.routes.walletTransactions}`, {
      params: new HttpParams().set('userId', userId)
    });
  }

  /**
   * 3-tier fallback strategy for transaction history:
   *  1. Transaction-service command side: /transactions/user/{userId}
   *  2. Wallet-service ledger: /api/wallet/transactions?userId={userId}
   *  Falls back if endpoint errors OR returns empty (event projection not synced yet).
   */
  getTransactionsByUser(userId: number): Observable<TransactionItem[]> {
    const endpoint1 = `${this.baseUrl}${this.routes.transactionsByUser}/${userId}`;
    const endpoint2 = `${this.baseUrl}${this.routes.walletTransactions}?userId=${userId}`;

    return this.http.get<TransactionItem[]>(endpoint1).pipe(
      timeout(8000),
      switchMap((rows: TransactionItem[]) => {
        if (rows && rows.length > 0) return of(rows);
        console.warn('⚠️ [API] Transaction-service returned empty, trying wallet ledger...');
        return this.getWalletLedgerFallback(userId);
      }),
      catchError((err) => {
        console.warn('⚠️ [API] Transaction-service failed:', err.message, '— trying wallet ledger...');
        return this.getWalletLedgerFallback(userId);
      })
    );
  }

  private getWalletLedgerFallback(userId: number): Observable<TransactionItem[]> {
    return this.http.get<any[]>(`${this.baseUrl}${this.routes.walletTransactions}`, {
      params: new HttpParams().set('userId', userId)
    }).pipe(
      timeout(8000),
      catchError(() => of([])),
      map((entries: any[]) => (entries ?? []).map((e: any) => ({
        id: e.id || 0,
        userId: e.userId ?? userId,
        senderId: e.entryType === 'DEBIT' ? userId : 0,
        receiverId: e.entryType === 'CREDIT' ? userId : 0,
        amount: Math.abs(e.amount ?? 0),
        type: e.entryType === 'CREDIT' ? 'TOPUP' : 'TRANSFER',
        status: 'SUCCESS',
        createdAt: e.createdAt || new Date().toISOString()
      } as TransactionItem)))
    );
  }

  getRewardsSummary(userId: number): Observable<RewardsAccount> {
    return this.http.get<RewardsAccount>(`${this.baseUrl}${this.routes.rewardsSummary}`, {
      params: new HttpParams().set('userId', userId)
    }).pipe(
      timeout(8000),
      catchError(() => of({ points: 0, tier: 'SILVER' } as unknown as RewardsAccount))
    );
  }

  getRewardsCatalog(): Observable<RewardCatalogItem[]> {
    return this.http.get<RewardCatalogItem[]>(`${this.baseUrl}/api/rewards/catalog`);
  }

  getActiveCampaigns(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/api/campaigns`).pipe(
      timeout(8000),
      catchError(() => of([]))
    );
  }

  redeemReward(userId: number, rewardId: number): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.baseUrl}/api/rewards/redeem`, { userId, rewardId });
  }

  getUserProfile(userId: number): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.baseUrl}/api/users/${userId}`);
  }

  submitKyc(userId: number, file: File, details?: { fullName?: string; email?: string; phone?: string }): Observable<UserProfile> {
    const formData = new FormData();
    formData.append('document', file);
    if (details?.fullName) formData.append('fullName', details.fullName);
    if (details?.email) formData.append('email', details.email);
    if (details?.phone) formData.append('phone', details.phone);
    return this.http.post<UserProfile>(`${this.baseUrl}/api/kyc/upload/${userId}`, formData);
  }

  getPendingKyc(): Observable<UserProfile[]> {
    return this.http.get<UserProfile[]>(`${this.baseUrl}/api/kyc/pending`);
  }

  updateKycStatus(userId: number, status: string, reason: string): Observable<UserProfile> {
    return this.http.put<UserProfile>(`${this.baseUrl}/api/kyc/${userId}/status`, { status, reason });
  }

  getAdminDashboard(): Observable<Record<string, number>> {
    return this.http.get<Record<string, number>>(`${this.baseUrl}/api/admin/dashboard`);
  }

  getNotificationStats(): Observable<NotificationStats> {
    return this.http.get<NotificationStats>(`${this.baseUrl}/api/notifications/admin/stats`);
  }

  getNotificationHistory(userId: number): Observable<NotificationMessage[]> {
    const params = new HttpParams().set('userId', userId);
    return this.http.get<NotificationMessage[]>(`${this.baseUrl}${this.routes.notificationsHistory}`, { params })
      .pipe(
        timeout(8000),
        catchError(() => {
          return this.http.get<NotificationMessage[]>(`${this.baseUrl}${this.routes.notificationsHistoryAlt}`, { params })
            .pipe(
              timeout(8000),
              catchError(() => of([]))
            );
        })
      );
  }

  /** Download transaction statement as PDF or CSV from backend */
  downloadStatement(userId: number, from: string, to: string, format: 'PDF' | 'CSV' = 'PDF'): Observable<ArrayBuffer> {
    const params = new HttpParams()
      .set('userId', userId)
      .set('from', from)
      .set('to', to)
      .set('format', format);

    return this.http.get(`${this.baseUrl}/transactions/statement`, {
      params,
      responseType: 'arraybuffer'
    }).pipe(
      timeout(15000),
      catchError((err) => {
        console.error('❌ [API] Statement download failed:', err.message);
        throw err;
      })
    );
  }

  /** Download individual transaction receipt as PDF */
  downloadReceipt(transactionId: number): Observable<ArrayBuffer> {
    return this.http.get(`${this.baseUrl}/transactions/${transactionId}/receipt`, {
      responseType: 'arraybuffer'
    }).pipe(
      timeout(15000),
      catchError((err) => {
        console.error('❌ [API] Receipt download failed:', err.message);
        throw err;
      })
    );
  }
}
