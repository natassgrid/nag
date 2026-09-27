import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { UserRoleService } from './user-role.service';
import { AdminCreateUserPayload, AdminInvitePayload } from '../models/user-management.model';

describe('UserRoleService', () => {
  let service: UserRoleService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [UserRoleService],
    });

    service = TestBed.inject(UserRoleService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should fetch users list and map properties properly', (done) => {
    const rawUsers = [
      {
        id: 'usr-1',
        username: 'rajesh.sharma',
        email: 'rajesh.sharma@nag.gov.in',
        fullName: 'Rajesh Sharma',
        roles: ['EXAM_CONTROLLER'],
        status: 'ACTIVE',
        mfaEnabled: true,
      },
    ];

    service.getUsers().subscribe((users) => {
      expect(users.length).toBe(1);
      expect(users[0].username).toBe('rajesh.sharma');
      expect(users[0].twoFactorEnabled).toBe(true);
      done();
    });

    const req = httpMock.expectOne('/api/v1/identity/users');
    expect(req.request.method).toBe('GET');
    req.flush({ data: rawUsers });
  });

  it('should create user', (done) => {
    const payload: AdminCreateUserPayload = {
      fullName: 'Anita Roy',
      email: 'anita.roy@nag.gov.in',
      roles: ['QUESTION_AUTHOR'],
    };

    service.createUser(payload).subscribe((created) => {
      expect(created.fullName).toBe('Anita Roy');
      expect(created.status).toBe('ACTIVE');
      done();
    });

    const req = httpMock.expectOne('/api/v1/identity/users');
    expect(req.request.method).toBe('POST');
    req.flush({ data: { id: 'u-created', ...payload } });
  });

  it('should update user status and roles', (done) => {
    service.updateUser('u-101', { fullName: 'Updated Name', roles: ['SUPER_ADMIN'] }).subscribe((res) => {
      expect(res.fullName).toBe('Updated Name');
      done();
    });

    const req = httpMock.expectOne('/api/v1/identity/users/u-101');
    expect(req.request.method).toBe('PUT');
    req.flush({ data: { id: 'u-101', fullName: 'Updated Name', roles: ['SUPER_ADMIN'] } });
  });

  it('should get roles with permissions mapped', (done) => {
    service.getRoles().subscribe((roles) => {
      expect(roles.length).toBe(1);
      expect(roles[0].name).toBe('ROLE_ADMIN');
      done();
    });

    const req = httpMock.expectOne('/api/v1/identity/roles/definitions?page=0&size=100');
    expect(req.request.method).toBe('GET');
    req.flush({
      data: [
        {
          id: 'r-1',
          code: 'ROLE_ADMIN',
          name: 'Administrator',
          description: 'Full access',
          systemRole: true,
          permissions: ['exam:create', 'exam:publish'],
        },
      ],
    });
  });

  it('should send admin invite', (done) => {
    const payload: AdminInvitePayload = {
      email: 'invitee@nag.gov.in',
      fullName: 'Invitee',
      roles: ['QUESTION_AUTHOR'],
    };

    service.sendInvitation(payload).subscribe((res) => {
      expect(res).toBeDefined();
      done();
    });

    const req = httpMock.expectOne('/api/v1/identity/invitations');
    expect(req.request.method).toBe('POST');
    req.flush({ data: { id: 'inv-123', email: 'invitee@nag.gov.in', fullName: 'Invitee', assignedRoles: ['QUESTION_AUTHOR'], status: 'PENDING' } });
  });
});
