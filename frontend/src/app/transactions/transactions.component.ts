import { CommonModule } from '@angular/common';
import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Subject, merge, takeUntil, distinctUntilChanged, map } from 'rxjs';
import { ApiService } from '../shared/services/api.service';
import { SessionService } from '../shared/services/session.service';
import { DataRefreshService } from '../shared/services/data-refresh.service';
import { TransactionItem } from '../shared/models/app.models';

@Component({
  selector: 'app-transactions',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './transactions.component.html',
  styleUrl: './transactions.component.scss'
})
export class TransactionsComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly destroy$ = new Subject<void>();

  allTransactions: TransactionItem[] = [];
  filtered: TransactionItem[] = [];
  loading = false;
  error: string | null = null;
  downloading = false;

  page = 1;
  pageSize = 8;

  readonly filterForm = this.fb.group({
    type: [''],
    status: ['']
  });

  constructor(
    private readonly api: ApiService,
    private readonly session: SessionService,
    private readonly dataRefresh: DataRefreshService
  ) {
    // Load immediately if user is already in session
    const existingUser = this.session.currentUser;
    if (existingUser?.userId && existingUser.userId > 0) {
      this.loadTransactions();
    }

    // Subscribe to refresh events
    merge(this.dataRefresh.getTransactionsRefresh$, this.dataRefresh.getGlobalRefresh$)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.loadTransactions();
      });
  }

  ngOnInit(): void {
    this.session.user$
      .pipe(
        map((user) => (user?.userId ?? null) as number | null),
        distinctUntilChanged(),
        takeUntil(this.destroy$)
      )
      .subscribe((userId) => {
        if (userId && userId > 0) {
          this.loadTransactions();
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadTransactions(): void {
    const userId = this.session.currentUser?.userId;

    if (userId == null || !Number.isFinite(userId) || userId <= 0) {
      this.allTransactions = [];
      this.error = 'Invalid user. Please log in again.';
      this.applyFilters();
      return;
    }

    this.loading = true;
    this.error = null;

    this.api.getTransactionsByUser(userId).subscribe({
      next: (rows) => {
        this.allTransactions = (rows ?? []).map((item) => ({
          ...item,
          type: this.normalizeType(item?.type),
          status: this.normalizeStatus(item?.status)
        }));
        this.loading = false;
        this.applyFilters();
      },
      error: (err) => {
        console.error('❌ [TRANSACTIONS] Error loading transactions:', err);
        this.allTransactions = [];
        this.loading = false;
        this.error = 'Failed to load transactions. Please try again.';
        this.applyFilters();
      }
    });
  }

  applyFilters(): void {
    const selectedType = this.normalizeType(this.filterForm.value.type);
    const selectedStatus = this.normalizeStatus(this.filterForm.value.status);

    this.filtered = this.allTransactions.filter((item) => {
      const itemType = this.normalizeType(item?.type);
      const itemStatus = this.normalizeStatus(item?.status);
      const typeOk = !selectedType || itemType === selectedType;
      const statusOk = !selectedStatus || itemStatus === selectedStatus;
      return typeOk && statusOk;
    });

    this.page = 1;
  }

  private normalizeFilterValue(value: string | null | undefined): string {
    return (value ?? '').trim().toUpperCase();
  }

  private normalizeType(value: string | null | undefined): string {
    const normalized = this.normalizeFilterValue(value);
    if (!normalized) return '';
    if (normalized.includes('TRANSFER')) return 'TRANSFER';
    if (normalized.includes('TOPUP') || normalized === 'CREDIT') return 'TOPUP';
    if (normalized.includes('PAYMENT')) return 'PAYMENT';
    if (normalized.includes('REFUND')) return 'REFUND';
    return normalized;
  }

  private normalizeStatus(value: string | null | undefined): string {
    const normalized = this.normalizeFilterValue(value);
    if (!normalized) return '';
    if (normalized === 'COMPLETED' || normalized === 'CAPTURED') return 'SUCCESS';
    return normalized;
  }

  get pagedItems(): TransactionItem[] {
    const start = (this.page - 1) * this.pageSize;
    return this.filtered.slice(start, start + this.pageSize);
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.filtered.length / this.pageSize));
  }

  prev(): void {
    if (this.page > 1) this.page--;
  }

  next(): void {
    if (this.page < this.totalPages) this.page++;
  }

  getTypeColor(type: string): string {
    switch (type.toUpperCase()) {
      case 'TOPUP': return 'bg-green-500/10 text-green-400 border-green-500/20';
      case 'TRANSFER': return 'bg-blue-500/10 text-blue-400 border-blue-500/20';
      case 'PAYMENT': return 'bg-purple-500/10 text-purple-400 border-purple-500/20';
      case 'REFUND': return 'bg-yellow-500/10 text-yellow-400 border-yellow-500/20';
      default: return 'bg-gray-500/10 text-gray-400 border-gray-500/20';
    }
  }

  getStatusColor(status: string): string {
    switch (status.toUpperCase()) {
      case 'SUCCESS': return 'bg-green-500/10 text-green-400 border-green-500/20';
      case 'PENDING': return 'bg-yellow-500/10 text-yellow-400 border-yellow-500/20';
      case 'FAILED': return 'bg-red-500/10 text-red-400 border-red-500/20';
      default: return 'bg-gray-500/10 text-gray-400 border-gray-500/20';
    }
  }

  /** Download as CSV using backend statement endpoint with fallback to client-side */
  exportTransactions(format: 'PDF' | 'CSV' = 'CSV'): void {
    if (this.filtered.length === 0) return;

    const userId = this.session.currentUser?.userId;
    if (!userId) return;

    this.downloading = true;

    // Build date range from transactions (earliest to latest)
    const dates = this.filtered.map(t => new Date(t.createdAt).getTime()).filter(d => !isNaN(d));
    const from = dates.length ? new Date(Math.min(...dates)) : new Date(Date.now() - 90 * 86400000);
    const to = dates.length ? new Date(Math.max(...dates) + 86400000) : new Date();

    this.api.downloadStatement(userId, from.toISOString(), to.toISOString(), format).subscribe({
      next: (blob) => {
        const ext = format === 'CSV' ? 'csv' : 'pdf';
        const mimeType = format === 'CSV' ? 'text/csv' : 'application/pdf';
        const fileBlob = new Blob([blob], { type: mimeType });
        const url = window.URL.createObjectURL(fileBlob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `statement_${userId}_${new Date().getTime()}.${ext}`;
        link.click();
        window.URL.revokeObjectURL(url);
        this.downloading = false;
      },
      error: () => {
        // Fallback: client-side CSV generation
        this.fallbackClientExport();
        this.downloading = false;
      }
    });
  }

  /** Download individual receipt as PDF */
  downloadReceipt(transactionId: number): void {
    this.api.downloadReceipt(transactionId).subscribe({
      next: (blob) => {
        const fileBlob = new Blob([blob], { type: 'application/pdf' });
        const url = window.URL.createObjectURL(fileBlob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `receipt_${transactionId}.pdf`;
        link.click();
        window.URL.revokeObjectURL(url);
      },
      error: (err) => {
        console.error('Receipt download failed:', err);
      }
    });
  }

  private fallbackClientExport(): void {
    const csvHeaders = ['ID', 'Type', 'Amount', 'Status', 'Sender ID', 'Receiver ID', 'Date'];
    const csvRows = this.filtered.map(item => [
      item.id,
      item.type,
      item.amount,
      item.status,
      item.senderId,
      item.receiverId,
      new Date(item.createdAt).toLocaleString()
    ]);

    const csvContent = [
      csvHeaders.join(','),
      ...csvRows.map(row => row.map(cell => `"${cell}"`).join(','))
    ].join('\n');

    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `transactions_${new Date().getTime()}.csv`;
    link.click();
    window.URL.revokeObjectURL(url);
  }
}
