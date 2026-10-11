import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ComponentRef } from '@angular/core';
import { SettingsSecurityTabComponent } from './settings-security-tab.component';
import { DEFAULT_SYSTEM_SETTINGS } from '../../models/settings.model';

describe('SettingsSecurityTabComponent', () => {
  let component: SettingsSecurityTabComponent;
  let componentRef: ComponentRef<SettingsSecurityTabComponent>;
  let fixture: ComponentFixture<SettingsSecurityTabComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SettingsSecurityTabComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(SettingsSecurityTabComponent);
    component = fixture.componentInstance;
    componentRef = fixture.componentRef;

    componentRef.setInput('settings', DEFAULT_SYSTEM_SETTINGS);
    fixture.detectChanges();
  });

  it('should create and render settings security tab', () => {
    expect(component).toBeTruthy();
  });

  it('should emit updated state when authRiskIpEnabled is changed', (done) => {
    component.settingsChange.subscribe((updated) => {
      expect(updated.authRiskIpEnabled).toBe(false);
      done();
    });

    component.update('authRiskIpEnabled', false);
  });

  it('should emit updated state when authRiskIpTtlDays is changed', (done) => {
    component.settingsChange.subscribe((updated) => {
      expect(updated.authRiskIpTtlDays).toBe(45);
      done();
    });

    component.update('authRiskIpTtlDays', 45);
  });
});
