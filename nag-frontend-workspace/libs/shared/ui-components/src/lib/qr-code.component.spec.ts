import { ComponentFixture, TestBed } from '@angular/core/testing';
import { QrCodeComponent } from './qr-code.component';

describe('QrCodeComponent', () => {
  let component: QrCodeComponent;
  let fixture: ComponentFixture<QrCodeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [QrCodeComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(QrCodeComponent);
    component = fixture.componentInstance;
  });

  it('should instantiate and accept data input', () => {
    fixture.componentRef.setInput('data', 'otpauth://totp/NAG:admin?secret=JBSWY3DPEHPK3PXP');
    fixture.componentRef.setInput('size', 200);
    fixture.detectChanges();

    expect(component.data()).toBe('otpauth://totp/NAG:admin?secret=JBSWY3DPEHPK3PXP');
    expect(component.size()).toBe(200);
  });
});
