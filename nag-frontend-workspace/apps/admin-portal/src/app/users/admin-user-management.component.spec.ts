import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { AdminUserManagementComponent } from './admin-user-management.component';
import { UserRoleService } from '@nag-frontend-workspace/shared-data-access-auth';

describe('AdminUserManagementComponent', () => {
  let component: AdminUserManagementComponent;
  let fixture: ComponentFixture<AdminUserManagementComponent>;
  let mockUserRoleService: {
    getUsers: jest.Mock;
    getRoles: jest.Mock;
    getPermissions: jest.Mock;
    getInvitations: jest.Mock;
    getStats: jest.Mock;
  };

  beforeEach(async () => {
    mockUserRoleService = {
      getUsers: jest.fn().mockReturnValue(
        of([
          {
            id: 'u-1',
            username: 'superadmin',
            email: 'admin@nag.gov.in',
            fullName: 'Super Administrator',
            roles: ['ROLE_SUPER_ADMIN'],
            status: 'ACTIVE',
            twoFactorEnabled: true,
          },
          {
            id: 'u-2',
            username: 'controller',
            email: 'controller@nag.gov.in',
            fullName: 'Examination Officer',
            roles: ['EXAM_CONTROLLER'],
            status: 'ACTIVE',
            twoFactorEnabled: false,
          },
        ])
      ),
      getRoles: jest.fn().mockReturnValue(
        of([
          {
            id: 'r-1',
            name: 'ROLE_SUPER_ADMIN',
            displayName: 'Super Administrator',
            description: 'Full root platform access',
            isSystemRole: true,
            permissions: ['ALL'],
          },
        ])
      ),
      getPermissions: jest.fn().mockReturnValue(of([])),
      getInvitations: jest.fn().mockReturnValue(of([])),
      getStats: jest.fn().mockReturnValue(
        of({
          totalUsers: 2,
          activeUsers: 2,
          totalRoles: 1,
          pendingInvitations: 0,
        })
      ),
    };

    await TestBed.configureTestingModule({
      imports: [AdminUserManagementComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: UserRoleService, useValue: mockUserRoleService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminUserManagementComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should initialize users, roles, and stats on load', () => {
    expect(mockUserRoleService.getUsers).toHaveBeenCalled();
    expect(mockUserRoleService.getRoles).toHaveBeenCalled();
    expect(component.users().length).toBe(2);
    expect(component.roles().length).toBe(1);
    expect(component.kpiStats().totalUsers).toBe(2);
  });

  it('should switch tabs between USERS, ROLES, and INVITATIONS', () => {
    component.activeTab.set('ROLES');
    expect(component.activeTab()).toBe('ROLES');

    component.activeTab.set('INVITATIONS');
    expect(component.activeTab()).toBe('INVITATIONS');
  });

  it('should compute non-empty availableFilterRoles merging system roles and users', () => {
    const filterRoles = component.availableFilterRoles();
    expect(filterRoles.length).toBeGreaterThan(0);
    const roleNames = filterRoles.map((r) => r.name);
    expect(roleNames).toContain('SUPER_ADMIN');
    expect(roleNames).toContain('EXAM_CONTROLLER');
  });

  it('should filter users by role matching with or without ROLE_ prefix', () => {
    component.selectedRole.set('SUPER_ADMIN');
    expect(component.filteredUsers().length).toBe(1);
    expect(component.filteredUsers()[0].id).toBe('u-1');

    component.selectedRole.set('EXAM_CONTROLLER');
    expect(component.filteredUsers().length).toBe(1);
    expect(component.filteredUsers()[0].id).toBe('u-2');

    component.selectedRole.set('ALL');
    expect(component.filteredUsers().length).toBe(2);
  });
});
