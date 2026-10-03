import { TestBed } from '@angular/core/testing';
import { Router, UrlTree } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { authGuard, rootGuard } from './auth.guard';

describe('Auth Guards', () => {
  let authServiceMock: { isAuthenticated: jest.Mock };
  let router: Router;

  beforeEach(() => {
    authServiceMock = {
      isAuthenticated: jest.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authServiceMock },
      ],
    });

    router = TestBed.inject(Router);
  });

  describe('authGuard', () => {
    it('should allow navigation when user is authenticated', () => {
      authServiceMock.isAuthenticated.mockReturnValue(true);

      const result = TestBed.runInInjectionContext(() =>
        authGuard({} as any, {} as any)
      );

      expect(result).toBe(true);
    });

    it('should redirect to /login when user is not authenticated', () => {
      authServiceMock.isAuthenticated.mockReturnValue(false);

      const result = TestBed.runInInjectionContext(() =>
        authGuard({} as any, {} as any)
      ) as UrlTree;

      expect(result instanceof UrlTree).toBe(true);
      expect(router.serializeUrl(result)).toBe('/login');
    });
  });

  describe('rootGuard', () => {
    it('should redirect to /dashboard when user is authenticated', () => {
      authServiceMock.isAuthenticated.mockReturnValue(true);

      const result = TestBed.runInInjectionContext(() =>
        rootGuard({} as any, {} as any)
      );

      expect(result instanceof UrlTree).toBe(true);
      expect(router.serializeUrl(result)).toBe('/dashboard');
    });

    it('should redirect to /login when user is not authenticated', () => {
      authServiceMock.isAuthenticated.mockReturnValue(false);

      const result = TestBed.runInInjectionContext(() =>
        rootGuard({} as any, {} as any)
      );

      expect(result instanceof UrlTree).toBe(true);
      expect(router.serializeUrl(result)).toBe('/login');
    });
  });
});
