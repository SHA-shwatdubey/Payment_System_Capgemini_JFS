import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { ApiService } from '../../shared/services/api.service';
import { NotificationStats, UserProfile } from '../../shared/models/app.models';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.scss'
})
export class AdminDashboardComponent implements OnInit {
  kpis: Record<string, number> = {};
  stats: NotificationStats | null = null;
  pendingKyc: UserProfile[] = [];
  actionMessage = '';

  readonly actionForms: Record<number, ReturnType<FormBuilder['group']>> = {};

  constructor(
    private readonly api: ApiService,
    private readonly fb: FormBuilder
  ) {}

  ngOnInit(): void {
    forkJoin({
      dashboard: this.api.getAdminDashboard(),
      notifications: this.api.getNotificationStats(),
      pending: this.api.getPendingKyc()
    }).subscribe({
      next: (res) => {
        this.kpis = res.dashboard;
        this.stats = res.notifications;
        this.pendingKyc = res.pending;
        this.pendingKyc.forEach((user) => {
          if (!user.id) {
            return;
          }
          this.actionForms[user.id] = this.fb.group({
            status: ['APPROVED'],
            reason: ['Reviewed by admin']
          });
        });
      }
    });
  }

  submitStatus(userId: number): void {
    const form = this.actionForms[userId];
    if (!form) {
      return;
    }

    const status = String(form.value.status || 'APPROVED');
    const reason = String(form.value.reason || 'Reviewed by admin');
    this.api.updateKycStatus(userId, status, reason).subscribe({
      next: () => {
        this.actionMessage = `KYC ${status} for user ${userId}`;
        this.pendingKyc = this.pendingKyc.filter((u) => u.id !== userId);
      }
    });
  }
}

