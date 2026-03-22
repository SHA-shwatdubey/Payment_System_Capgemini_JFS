import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { ApiService } from '../../shared/services/api.service';
import { SessionService } from '../../shared/services/session.service';

@Component({
  selector: 'app-transfer-money',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './transfer-money.component.html',
  styleUrl: './transfer-money.component.scss'
})
export class TransferMoneyComponent {
  private readonly fb = inject(FormBuilder);

  loading = false;
  message = '';
  error = '';

  readonly form = this.fb.group({
    toUserId: [null as number | null, [Validators.required, Validators.min(1)]],
    amount: [null as number | null, [Validators.required, Validators.min(1)]]
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

    const fromUserId = this.session.currentUser?.userId;
    if (!fromUserId) {
      this.error = 'User session missing.';
      return;
    }

    this.loading = true;
    this.error = '';
    this.message = '';

    const { toUserId, amount } = this.form.getRawValue();
    this.api.transfer(fromUserId, Number(toUserId), Number(amount))
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (res) => {
          this.message = res.message || 'Transfer completed.';
          this.form.reset({ toUserId: null, amount: null });
        },
        error: (err) => {
          this.error = err?.error?.message || 'Transfer failed.';
        }
      });
  }
}


