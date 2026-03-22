import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { SessionService } from '../services/session.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const session = inject(SessionService);
  const router = inject(Router);

  const token = session.token;
  const shouldSkip = req.url.includes('/api/auth/login') || req.url.includes('/api/auth/signup');

  const authReq = token && !shouldSkip
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authReq).pipe(
    catchError((error) => {
      if (error.status === 401) {
        session.logout();
        router.navigate(['/login']);
      }
      return throwError(() => error);
    })
  );
};

