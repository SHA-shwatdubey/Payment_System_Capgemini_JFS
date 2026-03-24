import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { AdminService, DisputeTransaction } from '../../shared/services/admin.service';

@Component({
  selector: 'app-admin-disputes',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './admin-disputes.component.html',
  styleUrl: './admin-disputes.component.scss'
})
export class AdminDisputesComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly adminService = inject(AdminService);

  disputes: DisputeTransaction[] = [];
  loading = false;
  error: string | null = null;
  resolvingId: number | null = null;
  showResolveForm: number | null = null;

  resolutionForm = this.fb.group({
    resolution: ['']
  });

  constructor() {}

  ngOnInit(): void {
    this.loadDisputes();
  }

  loadDisputes(): void {
    this.loading = true;
    this.error = null;

    this.adminService.getDisputedTransactions().subscribe({
      next: (data) => {
        this.disputes = data || [];
        this.loading = false;
      },
      error: (err) => {
        console.error('❌ [ADMIN-DISPUTES] Error:', err);
        this.error = 'Failed to load disputes';
        this.loading = false;
      }
    });
  }

  toggleResolveForm(txId: number): void {
    this.showResolveForm = this.showResolveForm === txId ? null : txId;
    this.resolutionForm.reset();
  }

  resolveDispute(txId: number): void {
    const resolution = this.resolutionForm.value.resolution || 'Resolved by admin';
    this.resolvingId = txId;

    this.adminService.resolveDispute(txId, resolution).subscribe({
      next: () => {
        this.disputes = this.disputes.filter(d => d.transactionId !== txId);
        this.resolvingId = null;
        this.showResolveForm = null;
      },
      error: (err) => {
        console.error('❌ [ADMIN-DISPUTES] Resolve error:', err);
        this.error = 'Failed to resolve dispute';
        this.resolvingId = null;
      }
    });
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'FLAGGED': return 'bg-red-500/10 text-red-400 border-red-500/20';
      case 'PENDING': return 'bg-yellow-500/10 text-yellow-400 border-yellow-500/20';
      case 'RESOLVED': return 'bg-green-500/10 text-green-400 border-green-500/20';
      default: return 'bg-gray-500/10 text-gray-400 border-gray-500/20';
    }
  }
}
