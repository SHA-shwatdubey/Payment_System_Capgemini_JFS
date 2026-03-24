import { Injectable } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { AuthResponse } from '../shared/models/app.models';
import { SessionService } from '../shared/services/session.service';
import { AuthApiService } from './auth-api.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  constructor(
    private readonly authApi: AuthApiService,
    private readonly session: SessionService
  ) {}

  login(identifier: string, password: string): Observable<AuthResponse> {
    return this.authApi.login(identifier, password).pipe(
      tap((response) => {
        if (response?.token) {
          this.session.saveToken(response.token);
        }
      })
    );
  }

  logout(): void {
    this.session.logout();
  }

  restoreSession(): void {
    const token = this.session.token;
    if (!token) {
      console.warn('No token found for session restore');
      this.session.logout();
      return;
    }

    const payload = this.decodePayload(token);
    if (!payload) {
      console.warn('Failed to decode token payload');
      this.session.logout();
      return;
    }

    const expRaw = payload['exp'];
    const exp = typeof expRaw === 'number' ? expRaw : Number(expRaw);
    if (Number.isFinite(exp) && exp > 0 && Date.now() >= exp * 1000) {
      console.warn('Token expired');
      this.session.logout();
      return;
    }

    console.log('Session restored successfully');
    this.session.refreshFromStorage();
  }

  private decodePayload(token: string): Record<string, unknown> | null {
    const parts = token.split('.');
    if (parts.length !== 3) {
      return null;
    }

    try {
      const encodedPayload = parts[1].replace(/-/g, '+').replace(/_/g, '/');
      const paddedPayload = encodedPayload.padEnd(Math.ceil(encodedPayload.length / 4) * 4, '=');
      const payloadJson = atob(paddedPayload);
      return JSON.parse(payloadJson) as Record<string, unknown>;
    } catch {
      return null;
    }
  }
}


