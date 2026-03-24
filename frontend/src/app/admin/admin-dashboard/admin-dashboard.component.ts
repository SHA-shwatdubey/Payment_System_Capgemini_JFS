import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { AdminService, AdminDashboardStats } from '../../shared/services/admin.service';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.scss'
})
export class AdminDashboardComponent implements OnInit {
  stats: AdminDashboardStats | null = null;
  loading = false;
  error: string | null = null;

  constructor(private readonly adminService: AdminService) {}

  ngOnInit(): void {
    this.loadDashboardStats();
  }

  loadDashboardStats(): void {
    console.log('📊 [ADMIN-DASHBOARD] Loading statistics...');
    this.loading = true;
    this.error = null;

    this.adminService.getDashboardStats().subscribe({
      next: (stats) => {
        console.log('✅ [ADMIN-DASHBOARD] Stats loaded:', stats);
        this.stats = stats;
        this.loading = false;
      },
      error: (err: any) => {
        console.error('❌ [ADMIN-DASHBOARD] Error loading stats:', err);
        this.error = 'Failed to load dashboard statistics';
        this.loading = false;
      }
    });
  }
}


