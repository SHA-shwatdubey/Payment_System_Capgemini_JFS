import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { JwtUser } from '../models/app.models';
import { TokenStorageService } from './token-storage.service';

@Injectable({ providedIn: 'root' })
export class SessionService {
  // Use inject() so dependency is available during field initialization.
  private readonly tokenStorage = inject(TokenStorageService);
  private readonly userState: BehaviorSubject<JwtUser | null>;
  readonly user$: Observable<JwtUser | null>;

  constructor() {
    console.log('🔐 [SESSION] Initializing SessionService...');
    // Read initial value from storage at construction
    const initialUser = this.readUserFromToken();
    console.log('🔐 [SESSION] Initial user from storage:', initialUser);
    this.userState = new BehaviorSubject<JwtUser | null>(initialUser);
    this.user$ = this.userState.asObservable();
  }

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
    console.log('🔐 [SESSION] Saving token...');
    this.tokenStorage.setToken(token);
    const user = this.readUserFromToken();
    console.log('🔐 [SESSION] Token saved, extracted user:', user);
    this.userState.next(user);
  }

  refreshFromStorage(): void {
    const user = this.readUserFromToken();
    console.log('🔐 [SESSION] Refreshing from storage, user:', user);
    console.log('🔐 [SESSION] Current state before next:', this.userState.value);
    // FORCE emit by setting to null first, then to user
    // This ensures BehaviorSubject emits even if previous value was null
    if (user && this.userState.value?.userId !== user.userId) {
      this.userState.next(user);
      console.log('🔐 [SESSION] Emitted user:', user);
    } else if (user && !this.userState.value) {
      this.userState.next(user);
      console.log('🔐 [SESSION] Emitted user (was null before):', user);
    } else {
      console.warn('🔐 [SESSION] No new user to emit or same user already set');
    }
  }

  logout(): void {
    console.log('🔐 [SESSION] Logging out...');
    this.tokenStorage.clear();
    this.userState.next(null);
  }

  private readUserFromToken(): JwtUser | null {
    const token = this.tokenStorage.getToken();
    if (!token) {
      console.warn('🔐 [SESSION] No token found in storage');
      return null;
    }

    const parts = token.split('.');
    if (parts.length !== 3) {
      console.warn('🔐 [SESSION] Invalid token format (not 3 parts)');
      return null;
    }

    try {
      const encodedPayload = parts[1].replace(/-/g, '+').replace(/_/g, '/');
      const paddedPayload = encodedPayload.padEnd(Math.ceil(encodedPayload.length / 4) * 4, '=');
      const payloadJson = atob(paddedPayload);
      const payload = JSON.parse(payloadJson) as Record<string, unknown>;

      console.log('🔐 [SESSION] Token payload:', payload);

      const userIdRaw = payload['userId'] ?? payload['id'] ?? payload['uid'];
      const userId = typeof userIdRaw === 'number' ? userIdRaw : Number(userIdRaw);

      if (!Number.isFinite(userId) || userId <= 0) {
        console.warn('🔐 [SESSION] Invalid userId extracted:', userIdRaw, 'parsed:', userId);
        return null;
      }

      const username = String(payload['sub'] ?? payload['username'] ?? '');
      const role = String(payload['role'] ?? '').toUpperCase();

      const user = { userId, username, role };
      console.log('✅ [SESSION] User extracted successfully:', user);
      return user;
    } catch (err) {
      console.error('❌ [SESSION] Error parsing token:', err);
      return null;
    }
  }
}
