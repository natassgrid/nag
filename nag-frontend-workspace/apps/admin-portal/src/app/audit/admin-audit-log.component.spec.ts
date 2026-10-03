import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of } from 'rxjs';
import { AdminAuditLogComponent } from './admin-audit-log.component';
import { AdminAuditService } from './services/admin-audit.service';

describe('AdminAuditLogComponent', () => {
  let component: AdminAuditLogComponent;
  let fixture: ComponentFixture<AdminAuditLogComponent>;
  let mockAuditService: {
    getAuditEvents: jest.Mock;
    verifyLedgerIntegrity: jest.Mock;
  };

  beforeEach(async () => {
    mockAuditService = {
      getAuditEvents: jest.fn().mockReturnValue(
        of([
          {
            id: 'aud-001',
            serviceName: 'IDENTITY_SERVICE',
            action: 'ADMIN_INVITE',
            severity: 'INFO',
            status: 'SUCCESS',
            actorId: 'superadmin',
            timestamp: new Date().toISOString(),
          },
        ])
      ),
      verifyLedgerIntegrity: jest.fn().mockReturnValue(of({ verified: true, blockCount: 100 })),
    };

    await TestBed.configureTestingModule({
      imports: [AdminAuditLogComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AdminAuditService, useValue: mockAuditService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminAuditLogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should load audit records on init', () => {
    expect(mockAuditService.getAuditEvents).toHaveBeenCalled();
    expect(component.records().length).toBe(1);
  });

  it('should open and close record detail drawer', () => {
    const rec = component.records()[0];
    component.onInspectRecord(rec);
    expect(component.selectedRecord()).toBe(rec);
    expect(component.isDetailDrawerOpen()).toBe(true);

    component.onCloseDrawer();
    expect(component.isDetailDrawerOpen()).toBe(false);
  });
});
