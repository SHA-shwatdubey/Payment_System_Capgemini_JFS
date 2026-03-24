import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ApiService } from '../shared/services/api.service';
import { SessionService } from '../shared/services/session.service';
import { NotificationMessage } from '../shared/models/app.models';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="max-w-4xl mx-auto pb-12">
      <div class="mb-8">
        <h1 class="text-3xl font-extrabold text-transparent bg-clip-text bg-gradient-to-r from-white to-gray-400">Notifications</h1>
        <p class="text-gray-400 mt-2">Stay updated with your transaction alerts and system notifications</p>
      </div>

      <!-- Loading -->
      <div *ngIf="loading" class="flex flex-col items-center justify-center h-48 bg-[#131B2C]/80 border border-gray-800 rounded-2xl backdrop-blur-md">
        <div class="animate-spin w-8 h-8 border-2 border-cyan-500/30 border-t-cyan-500 rounded-full mb-3"></div>
        <p class="text-cyan-400 font-medium">Loading notifications...</p>
      </div>

      <!-- Notifications List -->
      <div *ngIf="!loading" class="space-y-3">
        <div *ngFor="let n of notifications; let i = index"
          class="bg-[#131B2C] rounded-xl p-5 border border-gray-800 hover:border-cyan-500/30 transition-all group"
          [style.animation-delay]="i * 50 + 'ms'"
          style="animation: fadeInUp 0.4s ease both;">
          <div class="flex items-start gap-4">
            <div class="w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0"
              [ngClass]="getIconBg(n.type || 'INFO')">
              <span class="text-lg">{{ getIcon(n.type || 'INFO') }}</span>
            </div>
            <div class="flex-1 min-w-0">
              <p class="text-white font-medium text-sm">{{ n.title || n.subject || 'Notification' }}</p>
              <p class="text-gray-400 text-sm mt-1">{{ n.message || n.body || '' }}</p>
              <p class="text-gray-600 text-xs mt-2">{{ n.createdAt ? (n.createdAt | date:'MMM d, y h:mm a') : '' }}</p>
            </div>
          </div>
        </div>

        <!-- Empty State -->
        <div *ngIf="notifications.length === 0" class="text-center p-12 bg-[#131B2C] rounded-2xl border border-gray-800">
          <svg class="w-16 h-16 mx-auto text-gray-600 mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
          </svg>
          <p class="text-gray-400 mb-2">No notifications yet</p>
          <p class="text-gray-500 text-sm">You'll see alerts for transactions, rewards, and account activity here</p>
        </div>
      </div>
    </section>
  `,
  styles: [`
    @keyframes fadeInUp {
      from { opacity: 0; transform: translateY(12px); }
      to { opacity: 1; transform: translateY(0); }
    }
  `]
})
export class NotificationsComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly session = inject(SessionService);

  notifications: NotificationMessage[] = [];
  loading = false;

  ngOnInit(): void {
    this.loadNotifications();
  }

  private loadNotifications(): void {
    const userId = this.session.currentUser?.userId;
    if (!userId || userId <= 0) return;

    this.loading = true;
    this.api.getNotificationHistory(userId).subscribe({
      next: (data) => {
        this.notifications = data ?? [];
        this.loading = false;
      },
      error: () => {
        this.notifications = [];
        this.loading = false;
      }
    });
  }

  getIcon(type: string): string {
    switch (type?.toUpperCase()) {
      case 'TRANSACTION': return '💸';
      case 'REWARD': return '🎁';
      case 'SECURITY': return '🔒';
      case 'KYC': return '📋';
      default: return '🔔';
    }
  }

  getIconBg(type: string): string {
    switch (type?.toUpperCase()) {
      case 'TRANSACTION': return 'bg-cyan-500/10';
      case 'REWARD': return 'bg-purple-500/10';
      case 'SECURITY': return 'bg-red-500/10';
      case 'KYC': return 'bg-yellow-500/10';
      default: return 'bg-gray-500/10';
    }
  }
}
