import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { RegisterComponent } from './register.component';
import { AuthFlowService } from '../../services/auth-flow.service';

describe('Candidate RegisterComponent', () => {
  let component: RegisterComponent;
  let fixture: ComponentFixture<RegisterComponent>;
  let mockAuthFlowService: {
    registerCandidate: jest.Mock;
    navigateToVerifyOtp: jest.Mock;
  };

  beforeEach(async () => {
    mockAuthFlowService = {
      registerCandidate: jest.fn().mockReturnValue(of({ userId: 'cand-123' })),
      navigateToVerifyOtp: jest.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [RegisterComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AuthFlowService, useValue: mockAuthFlowService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should validate matching passwords', () => {
    component.fullName = 'Priya Patel';
    component.email = 'priya@example.com';
    component.mobile = '9876543210';
    component.password = 'Password@123';
    component.confirmPassword = 'DifferentPassword';

    component.handleRegister();

    expect(component.errorMessage()).toBe('Passwords do not match.');
    expect(mockAuthFlowService.registerCandidate).not.toHaveBeenCalled();
  });

  it('should call registerCandidate without identity document fields and navigate to OTP verification on success', () => {
    component.fullName = 'Priya Patel';
    component.email = 'priya@example.com';
    component.mobile = '9876543210';
    component.password = 'Password@123';
    component.confirmPassword = 'Password@123';

    component.handleRegister();

    expect(mockAuthFlowService.registerCandidate).toHaveBeenCalledWith({
      fullName: 'Priya Patel',
      email: 'priya@example.com',
      mobile: '9876543210',
      password: 'Password@123',
    });
    expect(mockAuthFlowService.navigateToVerifyOtp).toHaveBeenCalledWith({
      email: 'priya@example.com',
      mobile: '9876543210',
    });
  });
});
