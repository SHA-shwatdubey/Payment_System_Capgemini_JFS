import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { AdminService, KYCRequest } from '../../shared/services/admin.service';

@Component({
  selector: 'app-admin-kyc',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './admin-kyc.component.html',
  styleUrl: './admin-kyc.component.scss'
})
export class AdminKYCComponent implements OnInit {
  kycRequests: KYCRequest[] = [];
  loading = false;
  error: string | null = null;
  processingId: number | null = null;

  actionForms: Record<number, any> = {};

  constructor(
    private readonly adminService: AdminService,
    private readonly fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.loadPendingKYC();
  }

  loadPendingKYC(): void {
    console.log('📋 [ADMIN-KYC] Loading pending KYC requests...');
    this.loading = true;
    this.error = null;

    this.adminService.getPendingKYCRequests().subscribe({
      next: (data) => {
        console.log('✅ [ADMIN-KYC] Loaded:', data.length, 'requests');
        this.kycRequests = data || [];
        data.forEach(req => {
          this.actionForms[req.userId] = this.fb.group({
            verificationDetails: [''],
            reason: ['']
          });
        });
        this.loading = false;
      },
      error: (err) => {
        console.error('❌ [ADMIN-KYC] Error:', err);
        this.error = 'Failed to load KYC requests';
        this.loading = false;
      }
    });
  }

  approveKYC(userId: number): void {
    const form = this.actionForms[userId];
    if (!form) return;

    const details = form.get('verificationDetails')?.value || 'Verified';
    console.log('✅ [ADMIN-KYC] Approving KYC for user:', userId);
    this.processingId = userId;

    this.adminService.approveKYC(userId, details).subscribe({
      next: () => {
        console.log('✅ [ADMIN-KYC] Approved:', userId);
        this.kycRequests = this.kycRequests.filter(r => r.userId !== userId);
        this.processingId = null;
      },
      error: (err) => {
        console.error('❌ [ADMIN-KYC] Approval error:', err);
        this.error = 'Failed to approve KYC';
        this.processingId = null;
      }
    });
  }

  rejectKYC(userId: number): void {
    const form = this.actionForms[userId];
    if (!form) return;

    const reason = form.get('reason')?.value || 'Failed verification';
    console.log('❌ [ADMIN-KYC] Rejecting KYC for user:', userId);
    this.processingId = userId;

    this.adminService.rejectKYC(userId, reason).subscribe({
      next: () => {
        console.log('❌ [ADMIN-KYC] Rejected:', userId);
        this.kycRequests = this.kycRequests.filter(r => r.userId !== userId);
        this.processingId = null;
      },
      error: (err) => {
        console.error('❌ [ADMIN-KYC] Rejection error:', err);
        this.error = 'Failed to reject KYC';
        this.processingId = null;
      }
    });
  }
}

