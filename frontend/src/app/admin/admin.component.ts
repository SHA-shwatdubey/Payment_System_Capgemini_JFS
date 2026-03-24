import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { SessionService } from '../shared/services/session.service';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.scss'
})
export class AdminComponent {
  activeMenu: string = 'dashboard';

  constructor(
    private readonly session: SessionService,
    private readonly router: Router
  ) {
    // Check if user has admin role
    if (!this.session.hasRole('ADMIN')) {
      console.warn('⛔ [ADMIN] Unauthorized access - not admin role');
      this.router.navigate(['/dashboard']);
    }
  }

  selectMenu(menu: string): void {
    this.activeMenu = menu;
    console.log('📊 [ADMIN] Selected menu:', menu);
  }

  navigateTo(route: string): void {
    this.activeMenu = route;
    this.router.navigate(['/admin', route]);
  }
}

