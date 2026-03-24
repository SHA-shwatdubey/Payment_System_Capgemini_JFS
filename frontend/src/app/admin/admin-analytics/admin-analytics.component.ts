import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { AdminService, AdminDashboardStats } from '../../shared/services/admin.service';

@Component({
  selector: 'app-admin-analytics',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './admin-analytics.component.html',
  styleUrl: './admin-analytics.component.scss'
})
export class AdminAnalyticsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly adminService = inject(AdminService);

  stats: AdminDashboardStats | null = null;
  analyticsData: any = null;
  loading = false;
  error: string | null = null;

  dateRange = this.fb.group({
    from: [''],
    to: ['']
  });

  // Computed metrics
  transactionSuccessRate = 0;
  avgRevenuePerUser = 0;
  kycCompletionRate = 0;

  constructor() {}

  ngOnInit(): void {
    this.loadAnalytics();
  }

  loadAnalytics(): void {
    this.loading = true;
    this.error = null;

    // Load both dashboard stats and analytics in parallel
    this.adminService.getDashboardStats().subscribe({
      next: (stats) => {
        this.stats = stats;
        this.computeMetrics();

        // Also load detailed analytics
        const range = this.dateRange.value;
        const dateRange = (range.from && range.to) ? { from: range.from!, to: range.to! } : undefined;

        this.adminService.getAnalytics(dateRange).subscribe({
          next: (data) => {
            this.analyticsData = data;
            this.loading = false;
          },
          error: () => {
            // Analytics endpoint might not exist yet, fallback gracefully
            this.analyticsData = {};
            this.loading = false;
          }
        });
      },
      error: (err) => {
        console.error('❌ [ADMIN-ANALYTICS] Error:', err);
        this.error = 'Failed to load analytics';
        this.loading = false;
      }
    });
  }

  private computeMetrics(): void {
    if (!this.stats) return;

    // Compute derived metrics
    this.transactionSuccessRate = this.stats.totalTransactions > 0
      ? Math.round(((this.stats.totalTransactions - this.stats.flaggedTransactions) / this.stats.totalTransactions) * 100)
      : 0;

    this.avgRevenuePerUser = this.stats.totalUsers > 0
      ? Math.round(this.stats.totalVolume / this.stats.totalUsers)
      : 0;

    this.kycCompletionRate = this.stats.totalUsers > 0
      ? Math.round(((this.stats.totalUsers - this.stats.pendingKYC) / this.stats.totalUsers) * 100)
      : 0;
  }

  applyDateRange(): void {
    this.loadAnalytics();
  }
}
