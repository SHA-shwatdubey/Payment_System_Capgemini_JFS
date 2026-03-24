import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { AdminService } from '../../shared/services/admin.service';

@Component({
  selector: 'app-admin-transactions',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './admin-transactions.component.html',
  styleUrl: './admin-transactions.component.scss'
})
export class AdminTransactionsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly adminService = inject(AdminService);

  transactions: any[] = [];
  loading = false;
  error: string | null = null;
  page = 1;
  pageSize = 10;

  filterForm = this.fb.group({
    userId: [''],
    type: [''],
    status: [''],
    amount_min: [''],
    amount_max: ['']
  });

  constructor() {}

  ngOnInit(): void {
    this.loadTransactions();
  }

  loadTransactions(): void {
    console.log('📋 [ADMIN-TRANSACTIONS] Loading transactions...');
    this.loading = true;
    this.error = null;

    this.adminService.getUserTransactions(undefined, this.filterForm.value).subscribe({
      next: (data) => {
        console.log('✅ [ADMIN-TRANSACTIONS] Loaded:', data.length, 'transactions');
        this.transactions = data || [];
        this.loading = false;
      },
      error: (err) => {
        console.error('❌ [ADMIN-TRANSACTIONS] Error:', err);
        this.error = 'Failed to load transactions';
        this.loading = false;
      }
    });
  }

  applyFilters(): void {
    console.log('📋 [ADMIN-TRANSACTIONS] Applying filters:', this.filterForm.value);
    this.page = 1;
    this.loadTransactions();
  }

  resetFilters(): void {
    this.filterForm.reset();
    this.loadTransactions();
  }

  get paginatedTransactions(): any[] {
    const start = (this.page - 1) * this.pageSize;
    return this.transactions.slice(start, start + this.pageSize);
  }

  get totalPages(): number {
    return Math.ceil(this.transactions.length / this.pageSize);
  }

  prev(): void {
    if (this.page > 1) this.page--;
  }

  next(): void {
    if (this.page < this.totalPages) this.page++;
  }

  getStatusColor(status: string): string {
    switch(status?.toUpperCase()) {
      case 'SUCCESS': return 'bg-green-500/10 text-green-400 border-green-500/20';
      case 'PENDING': return 'bg-yellow-500/10 text-yellow-400 border-yellow-500/20';
      case 'FAILED': return 'bg-red-500/10 text-red-400 border-red-500/20';
      default: return 'bg-gray-500/10 text-gray-400 border-gray-500/20';
    }
  }
}


