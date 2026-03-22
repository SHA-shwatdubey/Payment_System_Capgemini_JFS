import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthResponse } from '../shared/models/app.models';

interface AuthRequest {
  username: string;
  password: string;
  role?: string;
}

@Injectable({ providedIn: 'root' })
export class AuthApiService {
  private readonly baseUrl = environment.apiBaseUrl;

  constructor(private readonly http: HttpClient) {}

  login(identifier: string, password: string): Observable<AuthResponse> {
    const payload: AuthRequest = { username: identifier, password };
    return this.http.post<AuthResponse>(`${this.baseUrl}/api/auth/login`, payload);
  }

  signup(identifier: string, password: string, role: string): Observable<AuthResponse> {
    const payload: AuthRequest = { username: identifier, password, role };
    return this.http.post<AuthResponse>(`${this.baseUrl}/api/auth/signup`, payload);
  }

  generateOtp(email: string, phoneNumber: string, otpType: 'EMAIL' | 'SMS'): Observable<Record<string, string>> {
    return this.http.post<Record<string, string>>(`${this.baseUrl}/api/auth/otp/generate`, {
      email,
      phoneNumber,
      otpType
    });
  }

  verifyOtp(email: string, phoneNumber: string, otp: string): Observable<Record<string, unknown>> {
    return this.http.post<Record<string, unknown>>(`${this.baseUrl}/api/auth/otp/verify`, {
      email,
      phoneNumber,
      otp
    });
  }
}

