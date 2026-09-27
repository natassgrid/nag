import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { App } from './app';
import { AuthService } from '@nag-frontend-workspace/shared-data-access-auth';

describe('Candidate Delivery App', () => {
  let component: App;
  let fixture: ComponentFixture<App>;
  let authServiceMock: {
    logout: jest.Mock;
    isAuthenticated: jest.Mock;
    currentUser: jest.Mock;
    userName: jest.Mock;
  };

  beforeEach(async () => {
    authServiceMock = {
      logout: jest.fn(),
      isAuthenticated: jest.fn().mockReturnValue(true),
      currentUser: jest.fn().mockReturnValue({ userId: 'u1', username: 'cand1' }),
      userName: jest.fn().mockReturnValue('Candidate 1'),
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
  });

  it('should create app component', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should toggle and close navigation drawer', () => {
    expect(component.isDrawerOpen()).toBe(false);
    component.toggleDrawer();
    expect(component.isDrawerOpen()).toBe(true);
    component.closeDrawer();
    expect(component.isDrawerOpen()).toBe(false);
  });

  it('should invoke auth logout', () => {
    component.handleLogout();
    expect(authServiceMock.logout).toHaveBeenCalled();
  });
});
