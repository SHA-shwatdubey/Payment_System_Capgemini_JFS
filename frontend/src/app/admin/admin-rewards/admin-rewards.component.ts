import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { AdminService, RewardCampaign } from '../../shared/services/admin.service';

@Component({
  selector: 'app-admin-rewards',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './admin-rewards.component.html',
  styleUrl: './admin-rewards.component.scss'
})
export class AdminRewardsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly adminService = inject(AdminService);

  campaigns: RewardCampaign[] = [];
  loading = false;
  error: string | null = null;
  showCreateForm = false;
  editingId: number | null = null;
  saving = false;

  campaignForm = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(3)]],
    description: ['', [Validators.required]],
    rewardType: ['BONUS_POINTS' as 'BONUS_POINTS' | 'CASHBACK' | 'MULTIPLIER', [Validators.required]],
    rewardValue: [0, [Validators.required, Validators.min(1)]],
    startDate: ['', [Validators.required]],
    endDate: ['', [Validators.required]],
    status: ['ACTIVE' as 'ACTIVE' | 'INACTIVE' | 'UPCOMING', [Validators.required]],
    eligibleUserTier: ['ALL', [Validators.required]],
    maxClaims: [0]
  });

  constructor() { }

  ngOnInit(): void {
    this.loadCampaigns();
  }

  loadCampaigns(): void {
    this.loading = true;
    this.error = null;

    this.adminService.getRewardCampaigns().subscribe({
      next: (data) => {
        this.campaigns = data || [];
        this.loading = false;
      },
      error: (err) => {
        console.error('❌ [ADMIN-REWARDS] Error:', err);
        this.error = 'Failed to load campaigns';
        this.loading = false;
      }
    });
  }

  toggleCreateForm(): void {
    this.showCreateForm = !this.showCreateForm;
    this.editingId = null;
    if (this.showCreateForm) {
      this.campaignForm.reset({
        rewardType: 'BONUS_POINTS',
        status: 'ACTIVE',
        eligibleUserTier: 'ALL',
        rewardValue: 0,
        maxClaims: 0
      });
    }
  }

  editCampaign(campaign: RewardCampaign): void {
    this.editingId = campaign.id ?? null;
    this.showCreateForm = true;
    this.campaignForm.patchValue({
      name: campaign.name,
      description: campaign.description,
      rewardType: campaign.rewardType,
      rewardValue: campaign.rewardValue,
      startDate: campaign.startDate?.split('T')[0] || '',
      endDate: campaign.endDate?.split('T')[0] || '',
      status: campaign.status,
      eligibleUserTier: campaign.eligibleUserTier,
      maxClaims: campaign.maxClaims || 0
    });
  }

  saveCampaign(): void {
    if (this.campaignForm.invalid) {
      this.campaignForm.markAllAsTouched();
      return;
    }

    this.saving = true;
    const formValue = this.campaignForm.getRawValue();
    const campaignData: RewardCampaign = {
      name: formValue.name!,
      description: formValue.description!,
      rewardType: formValue.rewardType as 'BONUS_POINTS' | 'CASHBACK' | 'MULTIPLIER',
      rewardValue: formValue.rewardValue!,
      startDate: formValue.startDate!,
      endDate: formValue.endDate!,
      status: formValue.status as 'ACTIVE' | 'INACTIVE' | 'UPCOMING',
      eligibleUserTier: formValue.eligibleUserTier!,
      maxClaims: formValue.maxClaims || undefined
    };

    if (this.editingId) {
      this.adminService.updateRewardCampaign(this.editingId, campaignData).subscribe({
        next: () => {
          this.saving = false;
          this.showCreateForm = false;
          this.editingId = null;
          this.loadCampaigns();
        },
        error: (err) => {
          console.error('❌ [ADMIN-REWARDS] Update error:', err);
          this.error = 'Failed to update campaign';
          this.saving = false;
        }
      });
    } else {
      this.adminService.createRewardCampaign(campaignData).subscribe({
        next: () => {
          this.saving = false;
          this.showCreateForm = false;
          this.loadCampaigns();
        },
        error: (err) => {
          console.error('❌ [ADMIN-REWARDS] Create error:', err);
          this.error = 'Failed to create campaign';
          this.saving = false;
        }
      });
    }
  }

  toggleCampaignStatus(campaign: RewardCampaign): void {
    if (!campaign.id) return;
    const newStatus = campaign.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    this.adminService.updateRewardCampaign(campaign.id, { status: newStatus as any }).subscribe({
      next: () => {
        campaign.status = newStatus as any;
      },
      error: (err) => {
        console.error('❌ [ADMIN-REWARDS] Toggle status error:', err);
        this.error = 'Failed to toggle campaign status';
      }
    });
  }

  deleteCampaign(campaign: RewardCampaign): void {
    if (!campaign.id) return;
    if (!confirm(`Are you sure you want to permanently delete "${campaign.name}"?`)) return;
    
    this.adminService.deleteRewardCampaign(campaign.id).subscribe({
      next: () => {
        this.campaigns = this.campaigns.filter(c => c.id !== campaign.id);
      },
      error: (err) => {
        console.error('❌ [ADMIN-REWARDS] Delete error:', err);
        this.error = 'Failed to delete campaign';
      }
    });
  }

  getRewardTypeColor(type: string): string {
    switch (type) {
      case 'BONUS_POINTS': return 'bg-purple-500/10 text-purple-400 border-purple-500/20';
      case 'CASHBACK': return 'bg-green-500/10 text-green-400 border-green-500/20';
      case 'MULTIPLIER': return 'bg-blue-500/10 text-blue-400 border-blue-500/20';
      default: return 'bg-gray-500/10 text-gray-400 border-gray-500/20';
    }
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'ACTIVE': return 'bg-green-500/10 text-green-400 border-green-500/20';
      case 'INACTIVE': return 'bg-red-500/10 text-red-400 border-red-500/20';
      case 'UPCOMING': return 'bg-yellow-500/10 text-yellow-400 border-yellow-500/20';
      default: return 'bg-gray-500/10 text-gray-400 border-gray-500/20';
    }
  }
}
