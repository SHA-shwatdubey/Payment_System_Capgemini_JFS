import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthApiService } from '../auth-api.service';

@Component({
  selector: 'app-signup',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './signup.component.html',
  styleUrl: './signup.component.scss'
})
export class SignupComponent {
  private readonly fb = inject(FormBuilder);

  loading = false;
  message = '';
  error = '';

  readonly signupForm = this.fb.group({
    identifier: ['', [Validators.required]],
    password: ['', [Validators.required, Validators.minLength(4)]],
    role: ['USER', [Validators.required]]
  });

  constructor(
    private readonly authApi: AuthApiService,
    private readonly router: Router
  ) { }

  signup(): void {
    if (this.signupForm.invalid) {
      this.signupForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.error = '';
    this.message = '';
    const { identifier, password, role } = this.signupForm.getRawValue();
    const safeIdentifier = identifier ?? '';
    const safePassword = password ?? '';
    const safeRole = role ?? 'USER';

    this.authApi.signup(safeIdentifier, safePassword, safeRole)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (res) => {
          this.message = res.message || 'Signup successful. Please login.';
          setTimeout(() => this.router.navigate(['/login']), 1200);
        },
        error: (err) => {
          this.error = this.extractApiError(err, 'Signup failed.');
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


