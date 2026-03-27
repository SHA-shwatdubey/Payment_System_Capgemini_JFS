import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { ApiService } from '../../shared/services/api.service';
import { SessionService } from '../../shared/services/session.service';
import { DataRefreshService } from '../../shared/services/data-refresh.service';

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
    recipient: ['', [Validators.required, Validators.minLength(1)]],
    amount: [null as number | null, [Validators.required, Validators.min(1)]]
  });

  constructor(
    private readonly api: ApiService,
    private readonly session: SessionService,
    private readonly dataRefresh: DataRefreshService
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

    const { recipient, amount } = this.form.getRawValue();
    
    // First lookup the user if it's not a numeric ID (or check if it's a mobile/upi)
    this.api.lookupUser(recipient!).subscribe({
        next: (user: any) => {
            const toUserId = user.authUserId || user.id;
            this.executeTransfer(fromUserId, toUserId, Number(amount));
        },
        error: (err: any) => {
            this.loading = false;
            this.error = 'Recipient not found. Please check phone/UPI/ID.';
        }
    });
  }

  private executeTransfer(fromUserId: number, toUserId: number, amount: number): void {
    this.api.transfer(fromUserId, toUserId, amount)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (res: any) => {
          this.message = res.message || 'Transfer completed.';
          this.form.reset({ recipient: '', amount: null });
          this.dataRefresh.refreshAll();
        },
        error: (err: any) => {
          this.error = err.error?.message || 'Transfer failed.';
        }
      });
  }
}
