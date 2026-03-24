import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { AdminService, FraudAlert } from '../../shared/services/admin.service';

@Component({
  selector: 'app-admin-fraud',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-fraud.component.html',
  styleUrl: './admin-fraud.component.scss'
})
export class AdminFraudComponent implements OnInit {
  alerts: FraudAlert[] = [];
  loading = false;
  error: string | null = null;

  constructor(private readonly adminService: AdminService) {}

  ngOnInit(): void {
    this.loadFraudAlerts();
  }

  loadFraudAlerts(): void {
    console.log('⚠️ [ADMIN-FRAUD] Loading fraud alerts...');
    this.loading = true;
    this.adminService.getFraudAlerts().subscribe({
      next: (data) => {
        console.log('✅ [ADMIN-FRAUD] Loaded:', data.length, 'alerts');
        this.alerts = data || [];
        this.loading = false;
      },
      error: (err) => {
        console.error('❌ [ADMIN-FRAUD] Error:', err);
        this.error = 'Failed to load fraud alerts';
        this.loading = false;
      }
    });
  }

  resolveAlert(alertId: number): void {
    console.log('⚠️ [ADMIN-FRAUD] Resolving alert:', alertId);
    this.adminService.resolveFraudAlert(alertId, 'RESOLVED').subscribe({
      next: () => {
        console.log('✅ [ADMIN-FRAUD] Resolved:', alertId);
        this.alerts = this.alerts.filter(a => a.alertId !== alertId);
      },
      error: (err) => {
        console.error('❌ [ADMIN-FRAUD] Resolve error:', err);
      }
    });
  }

  getRiskLevelColor(level: string): string {
    switch(level) {
      case 'CRITICAL': return 'bg-red-500/10 text-red-400 border-red-500/20';
      case 'HIGH': return 'bg-orange-500/10 text-orange-400 border-orange-500/20';
      case 'MEDIUM': return 'bg-yellow-500/10 text-yellow-400 border-yellow-500/20';
      case 'LOW': return 'bg-green-500/10 text-green-400 border-green-500/20';
      default: return 'bg-gray-500/10 text-gray-400 border-gray-500/20';
    }
  }
}

