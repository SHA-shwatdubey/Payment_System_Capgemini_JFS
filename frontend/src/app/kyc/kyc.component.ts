import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ApiService } from '../shared/services/api.service';
import { SessionService } from '../shared/services/session.service';
import { UserProfile } from '../shared/models/app.models';

@Component({
  selector: 'app-kyc',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './kyc.component.html',
  styleUrl: './kyc.component.scss'
})
export class KycComponent implements OnInit {
  profile: UserProfile | null = null;
  selectedFile: File | null = null;
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
    this.refreshProfile(userId);
  }

  onFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files?.[0] || null;
  }

  getKycStatusColor(status?: string): string {
    const upperStatus = (status || 'NOT_SUBMITTED').toUpperCase();
    switch (upperStatus) {
      case 'APPROVED':
        return 'bg-green-500/10 text-green-400 border-green-500/20';
      case 'PENDING':
        return 'bg-yellow-500/10 text-yellow-400 border-yellow-500/20';
      case 'REJECTED':
        return 'bg-red-500/10 text-red-400 border-red-500/20';
      default:
        return 'bg-gray-500/10 text-gray-400 border-gray-500/20';
    }
  }

  upload(): void {
    const userId = this.session.currentUser?.userId;
    if (!userId || !this.selectedFile) {
      this.error = 'Please select a document first.';
      return;
    }

    this.message = '';
    this.error = '';
    this.api.submitKyc(userId, this.selectedFile).subscribe({
      next: () => {
        this.message = 'KYC document uploaded successfully.';
        this.selectedFile = null;
        this.refreshProfile(userId);
      },
      error: (err) => {
        this.error = err?.error?.message || 'KYC upload failed.';
      }
    });
  }

  private refreshProfile(userId: number): void {
    this.api.getUserProfile(userId).subscribe({
      next: (res) => (this.profile = res)
    });
  }
}

