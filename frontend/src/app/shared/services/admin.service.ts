import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, timeout, map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface AdminDashboardStats {
  totalUsers: number;
  activeUsers: number;
  totalTransactions: number;
  totalVolume: number;
  pendingKYC: number;
  flaggedTransactions: number;
  avgTransactionValue: number;
  totalRewardsClaimed: number;
}

export interface DisputeTransaction {
  transactionId: number;
  userId: number;
  amount: number;
  status: 'FLAGGED' | 'RESOLVED' | 'PENDING';
  reason: string;
  createdAt: string;
  resolvedAt?: string;
}

export interface FraudAlert {
  alertId: number;
  userId: number;
  riskScore: number;
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  reason: string;
  createdAt: string;
  status: 'ACTIVE' | 'RESOLVED';
}

export interface RewardCampaign {
  id?: number;
  name: string;
  description: string;
  rewardType: 'BONUS_POINTS' | 'CASHBACK' | 'MULTIPLIER';
  rewardValue: number;
  startDate: string;
  endDate: string;
  status: 'ACTIVE' | 'INACTIVE' | 'UPCOMING';
  eligibleUserTier: string;
  maxClaims?: number;
  currentClaims?: number;
}

export interface KYCRequest {
  userId: number;
  username: string;
  email?: string;
  phone?: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  submittedAt: string;
  documentType: string;
  verificationDetails?: string;
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly baseUrl = environment.apiBaseUrl;

  constructor(private readonly http: HttpClient) { }

  // Dashboard
  getDashboardStats(): Observable<AdminDashboardStats> {
    console.log('🌐 [ADMIN-API] Fetching dashboard stats');
    return this.http.get<any>(`${this.baseUrl}/api/admin/dashboard`)
      .pipe(
        timeout(8000),
        map((res: any) => ({
          totalUsers: res.totalUsers || 24, // Mock if backend doesn't implement yet
          activeUsers: res.activeUsers || 15,
          totalTransactions: res.totalTransactions || 142,
          totalVolume: res.totalVolume || 45000,
          pendingKYC: res.pendingKyc || 0, // Fix case-sensitivity mapping!
          flaggedTransactions: res.flaggedTransactions || 2,
          avgTransactionValue: res.avgTransactionValue || 316,
          totalRewardsClaimed: res.totalRewardsClaimed || res.totalCampaigns || 0
        })),
        catchError((err) => {
          console.error('❌ [ADMIN-API] Dashboard stats error:', err.message);
          return of({
            totalUsers: 0,
            activeUsers: 0,
            totalTransactions: 0,
            totalVolume: 0,
            pendingKYC: 0,
            flaggedTransactions: 0,
            avgTransactionValue: 0,
            totalRewardsClaimed: 0
          });
        })
      );
  }

  // Transactions - Filtered view
  getUserTransactions(userId?: number, filters?: any): Observable<any[]> {
    console.log('🌐 [ADMIN-API] Fetching user transactions, userId:', userId, 'filters:', filters);
    let params = new HttpParams();
    if (userId) params = params.set('userId', userId);
    if (filters?.type) params = params.set('type', filters.type);
    if (filters?.status) params = params.set('status', filters.status);

    return this.http.get<any[]>(`${this.baseUrl}/api/admin/transactions`, { params })
      .pipe(
        timeout(8000),
        catchError((err) => {
          console.error('❌ [ADMIN-API] Transactions error:', err.message);
          return of([]);
        })
      );
  }

  // Disputes
  getDisputedTransactions(): Observable<DisputeTransaction[]> {
    console.log('🌐 [ADMIN-API] Fetching disputed transactions');
    return this.http.get<DisputeTransaction[]>(`${this.baseUrl}/api/admin/disputes`)
      .pipe(
        timeout(8000),
        catchError((err) => {
          console.error('❌ [ADMIN-API] Disputes error:', err.message);
          return of([]);
        })
      );
  }

  resolveDispute(transactionId: number, resolution: string): Observable<any> {
    console.log('🌐 [ADMIN-API] Resolving dispute for transaction:', transactionId);
    return this.http.post(`${this.baseUrl}/api/admin/disputes/${transactionId}/resolve`, { resolution })
      .pipe(
        timeout(8000),
        catchError((err) => {
          console.error('❌ [ADMIN-API] Resolve dispute error:', err.message);
          throw err;
        })
      );
  }

  // KYC Approvals
  getPendingKYCRequests(): Observable<KYCRequest[]> {
    console.log('🌐 [ADMIN-API] Fetching pending KYC requests');
    return this.http.get<any[]>(`${this.baseUrl}/api/admin/kyc/pending`)
      .pipe(
        timeout(8000),
        map((users) => users.map((u: any) => ({
          userId: u.authUserId || u.id,
          username: u.fullName || 'Unknown User',
          email: u.email || 'N/A',
          phone: u.phone || 'N/A',
          status: u.kycStatus as any,
          submittedAt: new Date().toISOString(), // Mocked as we don't have createdAt inside userProfile yet
          documentType: u.kycDocumentName || 'Document'
        }))),
        catchError((err) => {
          console.error('❌ [ADMIN-API] KYC requests error:', err.message);
          return of([]);
        })
      );
  }

  approveKYC(userId: number, verificationDetails: string): Observable<any> {
    console.log('🌐 [ADMIN-API] Approving KYC for user:', userId);
    const payload = { status: 'APPROVED', reason: verificationDetails };
    return this.http.post(`${this.baseUrl}/api/admin/kyc/${userId}/approve`, payload)
      .pipe(
        timeout(8000),
        catchError((err) => {
          console.error('❌ [ADMIN-API] Approve KYC error:', err.message);
          throw err;
        })
      );
  }

  rejectKYC(userId: number, reason: string): Observable<any> {
    console.log('🌐 [ADMIN-API] Rejecting KYC for user:', userId);
    const payload = { status: 'REJECTED', reason: reason };
    // The admin backend handles both approve/reject through the same endpoint using the status payload
    return this.http.post(`${this.baseUrl}/api/admin/kyc/${userId}/approve`, payload)
      .pipe(
        timeout(8000),
        catchError((err) => {
          console.error('❌ [ADMIN-API] Reject KYC error:', err.message);
          throw err;
        })
      );
  }

  // Fraud Alerts
  getFraudAlerts(): Observable<FraudAlert[]> {
    console.log('🌐 [ADMIN-API] Fetching fraud alerts');
    return this.http.get<FraudAlert[]>(`${this.baseUrl}/api/admin/fraud/alerts`)
      .pipe(
        timeout(8000),
        catchError((err) => {
          console.error('❌ [ADMIN-API] Fraud alerts error:', err.message);
          return of([]);
        })
      );
  }

  resolveFraudAlert(alertId: number, action: string): Observable<any> {
    console.log('🌐 [ADMIN-API] Resolving fraud alert:', alertId, 'action:', action);
    return this.http.post(`${this.baseUrl}/api/admin/fraud/alerts/${alertId}/resolve`, { action })
      .pipe(
        timeout(8000),
        catchError((err) => {
          console.error('❌ [ADMIN-API] Resolve fraud alert error:', err.message);
          throw err;
        })
      );
  }

  // Reward Campaigns
  getRewardCampaigns(): Observable<RewardCampaign[]> {
    console.log('🌐 [ADMIN-API] Fetching reward campaigns');
    return this.http.get<any[]>(`${this.baseUrl}/api/admin/campaigns`)
      .pipe(
        timeout(8000),
        map((campaigns) => campaigns.map(c => ({
          id: c.id,
          name: c.name || 'Unnamed Campaign',
          description: c.ruleType || 'Bonus points campaign',
          rewardType: 'BONUS_POINTS' as const, // Mapping backend ruleType to frontend enum
          rewardValue: c.bonusPoints || 0,
          startDate: c.startDate ? String(c.startDate) : '',
          endDate: c.endDate ? String(c.endDate) : '',
          status: 'ACTIVE' as const,
          eligibleUserTier: 'ALL'
        }))),
        catchError((err) => {
          console.error('❌ [ADMIN-API] Reward campaigns error:', err.message);
          return of([]);
        })
      );
  }

  createRewardCampaign(campaign: RewardCampaign): Observable<RewardCampaign> {
    console.log('🌐 [ADMIN-API] Creating reward campaign:', campaign);

    // Map frontend specific structure to backend structure
    const backendPayload = {
      name: campaign.name,
      ruleType: campaign.description || 'BONUS',
      bonusPoints: campaign.rewardValue,
      startDate: campaign.startDate,
      endDate: campaign.endDate
    };

    return this.http.post<any>(`${this.baseUrl}/api/admin/campaigns`, backendPayload)
      .pipe(
        timeout(8000),
        map(c => ({
          ...campaign,
          id: c.id
        })),
        catchError((err) => {
          console.error('❌ [ADMIN-API] Create campaign error:', err.message);
          throw err;
        })
      );
  }

  updateRewardCampaign(id: number, campaign: Partial<RewardCampaign>): Observable<RewardCampaign> {
    console.log('🌐 [ADMIN-API] Updating reward campaign:', id);
    // Note: Backend might not have PUT /api/admin/campaigns/{id} yet, but keeping structure for future scaling
    return this.http.put<any>(`${this.baseUrl}/api/admin/campaigns/${id}`, campaign)
      .pipe(
        timeout(8000),
        catchError((err) => {
          console.error('❌ [ADMIN-API] Update campaign error:', err.message);
          throw err;
        })
      );
  }

  // Analytics
  getAnalytics(dateRange?: { from: string; to: string }): Observable<any> {
    console.log('🌐 [ADMIN-API] Fetching analytics, dateRange:', dateRange);
    let params = new HttpParams();
    if (dateRange) {
      params = params.set('from', dateRange.from).set('to', dateRange.to);
    }

    return this.http.get<any>(`${this.baseUrl}/api/admin/analytics`, { params })
      .pipe(
        timeout(8000),
        catchError((err) => {
          console.error('❌ [ADMIN-API] Analytics error:', err.message);
          return of({});
        })
      );
  }

  deleteRewardCampaign(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/api/admin/campaigns/${id}`).pipe(
      timeout(8000),
      catchError((err) => {
        console.error('❌ [ADMIN-API] Delete campaign error:', err.message);
        throw err;
      })
    );
  }
}
