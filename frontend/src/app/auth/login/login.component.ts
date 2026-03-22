import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthApiService } from '../auth-api.service';
import { SessionService } from '../../shared/services/session.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);

  loading = false;
  error = '';
  otpMessage = '';
  showOtpSection = false;

  readonly loginForm = this.fb.group({
    identifier: ['', [Validators.required]],
    password: ['', [Validators.required, Validators.minLength(4)]]
  });

  readonly otpForm = this.fb.group({
    email: [''],
    phoneNumber: [''],
    otpType: ['EMAIL', [Validators.required]],
    otp: ['']
  });

  constructor(
    private readonly authApi: AuthApiService,
    private readonly session: SessionService,
    private readonly router: Router
  ) {}

  toggleOtpSection(): void {
    this.showOtpSection = !this.showOtpSection;
  }

  login(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.error = '';

    const { identifier, password } = this.loginForm.getRawValue();
    this.authApi.login(identifier!, password!)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (response) => {
          if (!response.token) {
            this.error = 'Login succeeded but token missing.';
            return;
          }
          this.session.saveToken(response.token);
          this.router.navigate(['/dashboard']);
        },
        error: (err) => {
          this.error = this.extractApiError(err, 'Login failed. Check credentials.');
        }
      });
  }

  generateOtp(): void {
    this.showOtpSection = true;
    const { email, phoneNumber, otpType } = this.otpForm.getRawValue();
    this.otpMessage = '';
    this.authApi.generateOtp(email || '', phoneNumber || '', otpType as 'EMAIL' | 'SMS').subscribe({
      next: (res) => {
        this.otpMessage = String(res['message'] || res['success'] || 'OTP sent successfully.');
      },
      error: (err) => {
        this.otpMessage = err?.error?.error || 'OTP generation failed.';
      }
    });
  }

  verifyOtp(): void {
    this.showOtpSection = true;
    const { email, phoneNumber, otp } = this.otpForm.getRawValue();
    this.authApi.verifyOtp(email || '', phoneNumber || '', otp || '').subscribe({
      next: () => {
        this.otpMessage = 'OTP verified. You can proceed with login.';
      },
      error: (err) => {
        this.otpMessage = err?.error?.error || 'OTP verification failed.';
      }
    });
  }

  private extractApiError(err: unknown, fallback: string): string {
    const anyErr = err as {
      status?: number;
      message?: string;
      error?: { message?: string; error?: string } | string;
    };

    if (anyErr?.status === 0) {
      return 'Backend unreachable. Please check gateway/auth service.';
    }

    if (typeof anyErr?.error === 'string' && anyErr.error.trim()) {
      return anyErr.error;
    }

    if (typeof anyErr?.error === 'object') {
      const msg = anyErr.error?.message || anyErr.error?.error;
      if (msg) {
        return msg;
      }
    }

    if (anyErr?.message) {
      return anyErr.message;
    }

    return fallback;
  }
}


