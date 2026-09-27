import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { Router } from '@angular/router';
import { App } from './app';
import { AuthService } from '@nag-frontend-workspace/shared-data-access-auth';

describe('Admin Portal App', () => {
  let component: App;
  let fixture: ComponentFixture<App>;
  let authServiceMock: {
    logout: jest.Mock;
    isAuthenticated: jest.Mock;
    currentUser: jest.Mock;
    userName: jest.Mock;
    userRole: jest.Mock;
  };
  let router: Router;

  beforeEach(async () => {
    authServiceMock = {
      logout: jest.fn(),
      isAuthenticated: jest.fn().mockReturnValue(true),
      currentUser: jest.fn().mockReturnValue({ userId: 'adm-1', username: 'admin' }),
      userName: jest.fn().mockReturnValue('Admin User'),
      userRole: jest.fn().mockReturnValue('ADMIN'),
    };

    await TestBed.configureTestingModule({
      imports: [
        HttpClientTestingModule,
        RouterTestingModule,
        App,
      ],
      providers: [
        { provide: AuthService, useValue: authServiceMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(App);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
  });

  it('should create admin app component', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should check route active status and handle logout', () => {
    jest.spyOn(router, 'navigate').mockResolvedValue(true);
    component.handleLogout();
    expect(authServiceMock.logout).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });
});
