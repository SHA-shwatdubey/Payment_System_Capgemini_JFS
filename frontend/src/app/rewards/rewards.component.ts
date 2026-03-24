import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ApiService } from '../shared/services/api.service';
import { SessionService } from '../shared/services/session.service';
import { RewardCatalogItem, RewardsAccount } from '../shared/models/app.models';

@Component({
  selector: 'app-rewards',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './rewards.component.html',
  styleUrl: './rewards.component.scss'
})
export class RewardsComponent implements OnInit {
  summary: RewardsAccount | null = null;
  catalog: RewardCatalogItem[] = [];
  campaigns: any[] = [];
  message = '';
  error = '';

  constructor(
    private readonly api: ApiService,
    private readonly session: SessionService
  ) {}

  ngOnInit(): void {
    const userId = this.session.currentUser?.userId;
    if (!userId) {
      return;
    }

    this.api.getRewardsSummary(userId).subscribe((res) => (this.summary = res));
    this.api.getRewardsCatalog().subscribe((res) => (this.catalog = res));
    this.api.getActiveCampaigns().subscribe((res) => (this.campaigns = res));
  }

  getTierColor(): string {
    const tier = this.summary?.tier || 'SILVER';
    switch (tier.toUpperCase()) {
      case 'GOLD':
        return 'bg-yellow-500/10 text-yellow-400 border border-yellow-500/20';
      case 'PLATINUM':
        return 'bg-slate-400/10 text-slate-300 border border-slate-400/20';
      case 'DIAMOND':
        return 'bg-cyan-500/10 text-cyan-400 border border-cyan-500/20';
      default:
        return 'bg-gray-500/10 text-gray-400 border border-gray-500/20';
    }
  }

  redeem(itemId: number): void {
    const userId = this.session.currentUser?.userId;
    if (!userId) {
      return;
    }

    this.message = '';
    this.error = '';
    this.api.redeemReward(userId, itemId).subscribe({
      next: (res) => {
        this.message = res.message || 'Reward redeemed.';
        this.api.getRewardsSummary(userId).subscribe((data) => (this.summary = data));
        this.api.getRewardsCatalog().subscribe((data) => (this.catalog = data));
      },
      error: (err) => {
        this.error = err?.error?.message || 'Redemption failed.';
      }
    });
  }
}

