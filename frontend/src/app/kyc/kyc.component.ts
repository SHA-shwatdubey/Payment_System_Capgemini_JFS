import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { ApiService } from '../shared/services/api.service';
import { SessionService } from '../shared/services/session.service';
import { UserProfile } from '../shared/models/app.models';

@Component({
  selector: 'app-kyc',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './kyc.component.html',
  styleUrl: './kyc.component.scss'
})
export class KycComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ApiService);
  private readonly session = inject(SessionService);

  profile: UserProfile | null = null;
  selectedFile: File | null = null;
  message = '';
  error = '';

  kycForm = this.fb.group({
    fullName: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    phone: ['', [Validators.required, Validators.pattern('^[0-9]+$')]]
  });

  constructor() {}

  ngOnInit(): void {
    const userId = this.session.currentUser?.userId;
    if (!userId) {
      return;
    }
    this.refreshProfile(userId);
  }

  onFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files?.[0] || null;
  }

  getKycStatusColor(status?: string): string {
    const upperStatus = (status || 'NOT_SUBMITTED').toUpperCase();
    switch (upperStatus) {
      case 'APPROVED':
        return 'bg-green-500/10 text-green-400 border-green-500/20';
      case 'PENDING':
        return 'bg-yellow-500/10 text-yellow-400 border-yellow-500/20';
      case 'REJECTED':
        return 'bg-red-500/10 text-red-400 border-red-500/20';
      default:
        return 'bg-gray-500/10 text-gray-400 border-gray-500/20';
    }
  }

  upload(): void {
    const userId = this.session.currentUser?.userId;
    if (!userId || !this.selectedFile) {
      this.error = 'Please select a document first.';
      return;
    }

    if (this.kycForm.invalid) {
      this.error = 'Please fill out all required details correctly.';
      return;
    }

    this.message = '';
    this.error = '';

    const formValues = this.kycForm.value;
    const details = {
      fullName: formValues.fullName!,
      email: formValues.email!,
      phone: formValues.phone!
    };

    this.api.submitKyc(userId, this.selectedFile, details).subscribe({
      next: () => {
        this.message = 'KYC document and details uploaded successfully.';
        this.selectedFile = null;
        this.refreshProfile(userId);
      },
      error: (err) => {
        this.error = err?.error?.message || 'KYC upload failed.';
      }
    });
  }

  private refreshProfile(userId: number): void {
    this.api.getUserProfile(userId).subscribe({
      next: (res) => {
        this.profile = res;
        if (res) {
          this.kycForm.patchValue({
             fullName: res.fullName || '',
             email: res.email || '',
             phone: res.phone || ''
          });
        }
      }
    });
  }
}

