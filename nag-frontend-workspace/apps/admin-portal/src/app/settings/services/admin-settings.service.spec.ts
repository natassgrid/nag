import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AdminSettingsService } from './admin-settings.service';
import { DEFAULT_SYSTEM_SETTINGS, SystemSettingsState } from '../models/settings.model';

describe('AdminSettingsService', () => {
  let service: AdminSettingsService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AdminSettingsService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(AdminSettingsService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should map backend config map including Risk Signal 3 IP & Geo settings to state', (done) => {
    const mockMap: Record<string, string> = {
      'auth.mfa.enforced': 'true',
      'auth.risk.ip.enabled': 'true',
      'auth.risk.ip.country-change.enabled': 'true',
      'auth.risk.ip.vpn-tor.enabled': 'true',
      'auth.risk.ip.ttl.days': '45',
    };

    service.getSettings().subscribe((state) => {
      expect(state.authMfaEnforced).toBe(true);
      expect(state.authRiskIpEnabled).toBe(true);
      expect(state.authRiskIpCountryChangeEnabled).toBe(true);
      expect(state.authRiskIpVpnTorEnabled).toBe(true);
      expect(state.authRiskIpTtlDays).toBe(45);
      done();
    });

    const req = httpTesting.expectOne('/api/v1/admin/config/map');
    expect(req.request.method).toBe('GET');
    req.flush(mockMap);
  });

  it('should serialize Risk Signal 3 IP & Geo settings to backend bulk update payload', (done) => {
    const updatedState: SystemSettingsState = {
      ...DEFAULT_SYSTEM_SETTINGS,
      authRiskIpEnabled: false,
      authRiskIpCountryChangeEnabled: false,
      authRiskIpVpnTorEnabled: false,
      authRiskIpTtlDays: 60,
    };

    service.saveSettings(updatedState).subscribe(() => {
      done();
    });

    const req = httpTesting.expectOne('/api/v1/admin/config/bulk');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.configs['auth.risk.ip.enabled']).toBe('false');
    expect(req.request.body.configs['auth.risk.ip.country-change.enabled']).toBe('false');
    expect(req.request.body.configs['auth.risk.ip.vpn-tor.enabled']).toBe('false');
    expect(req.request.body.configs['auth.risk.ip.ttl.days']).toBe('60');
    req.flush({});
  });
});
