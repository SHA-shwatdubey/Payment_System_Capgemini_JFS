import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { ApiService } from '../../shared/services/api.service';
import { SessionService } from '../../shared/services/session.service';

@Component({
  selector: 'app-add-money',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './add-money.component.html',
  styleUrl: './add-money.component.scss'
})
export class AddMoneyComponent {
  private readonly fb = inject(FormBuilder);

  loading = false;
  message = '';
  error = '';

  readonly form = this.fb.group({
    amount: [null as number | null, [Validators.required, Validators.min(1)]],
    method: ['UPI', [Validators.required]]
  });

  constructor(
    private readonly api: ApiService,
    private readonly session: SessionService
  ) {}

  setAmount(amount: number): void {
    this.form.patchValue({ amount });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const userId = this.session.currentUser?.userId;
    if (!userId) {
      this.error = 'User session missing.';
      return;
    }

    this.loading = true;
    this.error = '';
    this.message = '';

    const { amount, method } = this.form.getRawValue();
    this.api.topup(userId, Number(amount), method || 'UPI')
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (res) => {
          this.message = `Wallet credited. New balance: ₹${res.balance}`;
          this.form.reset({ amount: null, method: 'UPI' });
        },
        error: (err) => {
          this.error = err?.error?.message || 'Topup failed.';
        }
      });
  }
}


