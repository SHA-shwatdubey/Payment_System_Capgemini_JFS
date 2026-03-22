import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { SessionService } from '../services/session.service';

export const adminGuard: CanActivateFn = () => {
  const session = inject(SessionService);
  const router = inject(Router);

  if (session.isLoggedIn() && session.hasRole('ADMIN')) {
    return true;
  }

  return router.createUrlTree(['/dashboard']);
};

