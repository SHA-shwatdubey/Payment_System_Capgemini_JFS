import { Injectable, inject } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { JwtUser } from '../models/app.models';
import { TokenStorageService } from './token-storage.service';

@Injectable({ providedIn: 'root' })
export class SessionService {
  // Use inject() so dependency is available during field initialization.
  private readonly tokenStorage = inject(TokenStorageService);
  private readonly userState = new BehaviorSubject<JwtUser | null>(this.readUserFromToken());
  readonly user$ = this.userState.asObservable();

  get currentUser(): JwtUser | null {
    return this.userState.value;
  }

  get token(): string | null {
    return this.tokenStorage.getToken();
  }

  isLoggedIn(): boolean {
    return !!this.currentUser;
  }

  hasRole(role: string): boolean {
    const currentRole = this.currentUser?.role || '';
    return currentRole.toUpperCase() === role.toUpperCase();
  }

  saveToken(token: string): void {
    this.tokenStorage.setToken(token);
    this.userState.next(this.readUserFromToken());
  }

  logout(): void {
    this.tokenStorage.clear();
    this.userState.next(null);
  }

  private readUserFromToken(): JwtUser | null {
    const token = this.tokenStorage.getToken();
    if (!token) {
      return null;
    }

    const parts = token.split('.');
    if (parts.length !== 3) {
      return null;
    }

    try {
      const payloadJson = atob(parts[1].replace(/-/g, '+').replace(/_/g, '/'));
      const payload = JSON.parse(payloadJson) as Record<string, unknown>;

      const userIdRaw = payload['userId'];
      const userId = typeof userIdRaw === 'number' ? userIdRaw : Number(userIdRaw ?? 0);
      const username = String(payload['sub'] ?? '');
      const role = String(payload['role'] ?? '').toUpperCase();

      return {
        userId: Number.isNaN(userId) ? null : userId,
        username,
        role
      };
    } catch {
      return null;
    }
  }
}

