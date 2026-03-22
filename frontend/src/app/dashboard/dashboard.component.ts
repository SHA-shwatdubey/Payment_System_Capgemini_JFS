import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ApiService } from '../shared/services/api.service';
import { SessionService } from '../shared/services/session.service';
import { NotificationMessage, RewardsAccount, TransactionItem } from '../shared/models/app.models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  loading = true;
  balance = 0;
  rewards: RewardsAccount | null = null;
  recentTransactions: TransactionItem[] = [];
  recentNotifications: NotificationMessage[] = [];

  constructor(
    private readonly api: ApiService,
    private readonly session: SessionService
  ) {}

  ngOnInit(): void {
    const userId = this.session.currentUser?.userId;
    if (!userId) {
      this.loading = false;
      return;
    }

    forkJoin({
      balance: this.api.getWalletBalance(userId),
      rewards: this.api.getRewardsSummary(userId),
      transactions: this.api.getTransactionsByUser(userId),
      notifications: this.api.getNotificationHistory(userId)
    }).subscribe({
      next: (result) => {
        this.balance = Number(result.balance.balance || 0);
        this.rewards = result.rewards;
        this.recentTransactions = result.transactions.slice(0, 5);
        this.recentNotifications = result.notifications.slice(0, 5);
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }
}

