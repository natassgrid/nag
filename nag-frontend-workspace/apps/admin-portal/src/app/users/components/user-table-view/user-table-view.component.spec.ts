import { ComponentFixture, TestBed } from '@angular/core/testing';
import { UserTableViewComponent } from './user-table-view.component';
import { AdminUserAccount, RoleDefinition } from '@nag-frontend-workspace/shared-data-access-auth';

describe('UserTableViewComponent', () => {
  let component: UserTableViewComponent;
  let fixture: ComponentFixture<UserTableViewComponent>;

  const mockUsers: AdminUserAccount[] = [
    {
      id: 'usr-1',
      username: 'amit.patel',
      email: 'amit.patel@nag.gov.in',
      fullName: 'Amit Patel',
      roles: ['SUPER_ADMIN', 'EXAM_CONTROLLER'],
      status: 'ACTIVE',
      twoFactorEnabled: true,
      twoFactorMethod: 'TOTP',
      createdAt: '2025-01-01T00:00:00Z',
    },
    {
      id: 'usr-2',
      username: 'neha.sharma',
      email: 'neha.sharma@nag.gov.in',
      fullName: 'Neha Sharma',
      roles: ['QUESTION_AUTHOR'],
      status: 'DEACTIVATED',
      twoFactorEnabled: false,
      createdAt: '2025-02-01T00:00:00Z',
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserTableViewComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(UserTableViewComponent);
    component = fixture.componentInstance;
  });

  it('should fall back to default system roles when roles input is empty', () => {
    fixture.componentRef.setInput('users', mockUsers);
    fixture.componentRef.setInput('roles', []);
    fixture.detectChanges();

    const roles = component.filterRoles();
    expect(roles.length).toBeGreaterThan(0);
    expect(roles.some((r) => r.name === 'SUPER_ADMIN')).toBe(true);
    expect(roles.some((r) => r.name === 'QUESTION_AUTHOR')).toBe(true);
  });

  it('should format role names correctly using getRoleName', () => {
    fixture.componentRef.setInput('users', mockUsers);
    fixture.detectChanges();

    expect(component.getRoleName('SUPER_ADMIN')).toBe('Super Administrator');
    expect(component.getRoleName('ROLE_SUPER_ADMIN')).toBe('Super Administrator');
    expect(component.getRoleName('EXAM_CONTROLLER')).toBe('Examination Controller');
    expect(component.getRoleName('CUSTOM_ROLE')).toBe('Custom Role');
  });

  it('should render the role filter options in the select dropdown', () => {
    fixture.componentRef.setInput('users', mockUsers);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    const select = compiled.querySelector('#role-filter') as HTMLSelectElement;
    expect(select).toBeTruthy();

    const options = select.querySelectorAll('option');
    // "All Roles" + system roles
    expect(options.length).toBeGreaterThan(1);
    expect(options[0].textContent?.trim()).toBe('All Roles');
  });

  it('should use provided custom roles when passed via input', () => {
    const customRoles: RoleDefinition[] = [
      {
        id: 'r-custom-1',
        name: 'REGIONAL_OBSERVER',
        displayName: 'Regional Observer',
        description: 'Oversees regional examination centers',
        systemRole: false,
        permissions: ['EXAM:READ'],
      },
    ];

    fixture.componentRef.setInput('users', mockUsers);
    fixture.componentRef.setInput('roles', customRoles);
    fixture.detectChanges();

    const roles = component.filterRoles();
    expect(roles.some((r) => r.name === 'REGIONAL_OBSERVER')).toBe(true);
    expect(component.getRoleName('REGIONAL_OBSERVER')).toBe('Regional Observer');
  });
});
