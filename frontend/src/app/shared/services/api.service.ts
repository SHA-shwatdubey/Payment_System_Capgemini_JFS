import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
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

  constructor(private readonly http: HttpClient) {}

  getWalletBalance(userId: number): Observable<WalletBalanceResponse> {
    return this.http.get<WalletBalanceResponse>(`${this.baseUrl}/api/wallet/balance`, {
      params: new HttpParams().set('userId', userId)
    });
  }

  topup(userId: number, amount: number, method: string): Observable<WalletAccount> {
    return this.http.post<WalletAccount>(`${this.baseUrl}/api/wallet/topup`, { userId, amount, method });
  }

  transfer(fromUserId: number, toUserId: number, amount: number): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.baseUrl}/api/wallet/transfer`, {
      fromUserId,
      toUserId,
      amount
    });
  }

  getWalletTransactions(userId: number): Observable<Array<Record<string, unknown>>> {
    return this.http.get<Array<Record<string, unknown>>>(`${this.baseUrl}/api/wallet/transactions`, {
      params: new HttpParams().set('userId', userId)
    });
  }

  getTransactionsByUser(userId: number): Observable<TransactionItem[]> {
    return this.http.get<TransactionItem[]>(`${this.baseUrl}/transactions/user/${userId}`);
  }

  getRewardsSummary(userId: number): Observable<RewardsAccount> {
    return this.http.get<RewardsAccount>(`${this.baseUrl}/api/rewards/summary`, {
      params: new HttpParams().set('userId', userId)
    });
  }

  getRewardsCatalog(): Observable<RewardCatalogItem[]> {
    return this.http.get<RewardCatalogItem[]>(`${this.baseUrl}/api/rewards/catalog`);
  }

  redeemReward(userId: number, rewardId: number): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.baseUrl}/api/rewards/redeem`, { userId, rewardId });
  }

  getUserProfile(userId: number): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${this.baseUrl}/api/users/${userId}`);
  }

  submitKyc(userId: number, file: File): Observable<UserProfile> {
    const formData = new FormData();
    formData.append('document', file);
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
    return this.http.get<NotificationMessage[]>(`${this.baseUrl}/api/notifications/history`, {
      params: new HttpParams().set('userId', userId)
    });
  }
}

