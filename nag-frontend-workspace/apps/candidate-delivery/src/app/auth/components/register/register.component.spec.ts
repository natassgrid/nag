import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
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

  it('should validate invalid mobile number and invalid email on blur', () => {
    component.mobile = '12345';
    component.validateSingleField('mobile');
    expect(component.fieldErrors()['mobile']).toBe(
      'Mobile number must be a valid 10-digit Indian number starting with 6, 7, 8, or 9.'
    );

    component.clearFieldError('mobile');
    expect(component.fieldErrors()['mobile']).toBeUndefined();

    component.email = 'invalid-email';
    component.validateSingleField('email');
    expect(component.fieldErrors()['email']).toBe('Please enter a valid email address.');
  });

  it('should validate full name length and password minimum length on submit', () => {
    component.fullName = 'Al';
    component.email = 'valid@example.com';
    component.mobile = '9876543210';
    component.password = 'pass';
    component.confirmPassword = 'pass';

    component.handleRegister();

    expect(component.fieldErrors()['fullName']).toBe(
      'Full legal name must be between 3 and 100 characters.'
    );
    expect(component.fieldErrors()['password']).toBe('Password must be at least 8 characters long.');
    expect(component.errorMessage()).toBe(
      'Full legal name must be between 3 and 100 characters.'
    );
    expect(mockAuthFlowService.registerCandidate).not.toHaveBeenCalled();
  });

  it('should parse backend HTTP error response and display field errors without falling back to OTP', () => {
    mockAuthFlowService.registerCandidate.mockReturnValue(
      throwError(() => ({
        status: 400,
        error: {
          fieldErrors: {
            mobile: 'Mobile number is already registered',
          },
        },
      }))
    );

    component.fullName = 'Priya Patel';
    component.email = 'priya@example.com';
    component.mobile = '9876543210';
    component.password = 'Password@123';
    component.confirmPassword = 'Password@123';

    component.handleRegister();

    expect(component.errorMessage()).toBe('Mobile number is already registered');
    expect(component.fieldErrors()['mobile']).toBe('Mobile number is already registered');
    expect(mockAuthFlowService.navigateToVerifyOtp).not.toHaveBeenCalled();
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
