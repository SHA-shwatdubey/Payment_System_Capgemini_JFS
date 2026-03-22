import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ApiService } from '../shared/services/api.service';
import { SessionService } from '../shared/services/session.service';
import { TransactionItem } from '../shared/models/app.models';

@Component({
  selector: 'app-transactions',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './transactions.component.html',
  styleUrl: './transactions.component.scss'
})
export class TransactionsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);

  allTransactions: TransactionItem[] = [];
  filtered: TransactionItem[] = [];

  page = 1;
  pageSize = 8;

  readonly filterForm = this.fb.group({
    type: [''],
    status: ['']
  });

  constructor(
    private readonly api: ApiService,
    private readonly session: SessionService
  ) {}

  ngOnInit(): void {
    const userId = this.session.currentUser?.userId;
    if (!userId) {
      return;
    }

    this.api.getTransactionsByUser(userId).subscribe({
      next: (rows) => {
        this.allTransactions = rows;
        this.applyFilters();
      }
    });
  }

  applyFilters(): void {
    const type = (this.filterForm.value.type || '').toUpperCase();
    const status = (this.filterForm.value.status || '').toUpperCase();

    this.filtered = this.allTransactions.filter((item) => {
      const typeOk = !type || item.type.toUpperCase().includes(type);
      const statusOk = !status || item.status.toUpperCase().includes(status);
      return typeOk && statusOk;
    });
    this.page = 1;
  }

  get pagedItems(): TransactionItem[] {
    const start = (this.page - 1) * this.pageSize;
    return this.filtered.slice(start, start + this.pageSize);
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.filtered.length / this.pageSize));
  }

  prev(): void {
    if (this.page > 1) {
      this.page--;
    }
  }

  next(): void {
    if (this.page < this.totalPages) {
      this.page++;
    }
  }

  getTypeColor(type: string): string {
    const upperType = type.toUpperCase();
    switch (upperType) {
      case 'TOPUP':
        return 'bg-green-500/10 text-green-400 border-green-500/20';
      case 'TRANSFER':
        return 'bg-blue-500/10 text-blue-400 border-blue-500/20';
      case 'REWARD':
        return 'bg-purple-500/10 text-purple-400 border-purple-500/20';
      case 'REFUND':
        return 'bg-yellow-500/10 text-yellow-400 border-yellow-500/20';
      default:
        return 'bg-gray-500/10 text-gray-400 border-gray-500/20';
    }
  }

  getStatusColor(status: string): string {
    const upperStatus = status.toUpperCase();
    switch (upperStatus) {
      case 'SUCCESS':
        return 'bg-green-500/10 text-green-400 border-green-500/20';
      case 'PENDING':
        return 'bg-yellow-500/10 text-yellow-400 border-yellow-500/20';
      case 'FAILED':
        return 'bg-red-500/10 text-red-400 border-red-500/20';
      case 'CANCELLED':
        return 'bg-gray-500/10 text-gray-400 border-gray-500/20';
      default:
        return 'bg-gray-500/10 text-gray-400 border-gray-500/20';
    }
  }

  exportTransactions(): void {
    if (this.filtered.length === 0) {
      return;
    }

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


