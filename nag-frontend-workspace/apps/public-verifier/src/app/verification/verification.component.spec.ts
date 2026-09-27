import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { VerificationComponent } from './verification.component';

describe('VerificationComponent', () => {
  let component: VerificationComponent;
  let fixture: ComponentFixture<VerificationComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        VerificationComponent,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(VerificationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create verification component', () => {
    expect(component).toBeTruthy();
    expect(component.verifiedResult()).toBeNull();
  });

  it('should not verify if searchQuery is empty', () => {
    component.searchQuery = '  ';
    component.verify();
    expect(component.verifying()).toBe(false);
    expect(component.verifiedResult()).toBeNull();
  });

  it('should verify certificate hash and produce ledger proof', fakeAsync(() => {
    component.searchQuery = '0x7f83b1657ff1fc53b92dc18148a1d65dfc2d4b1fa3d677284addd200126d9069';
    component.verify();
    expect(component.verifying()).toBe(true);

    tick(1000);
    expect(component.verifying()).toBe(false);
    expect(component.verifiedResult()).toBeTruthy();
    expect(component.verifiedResult()?.ledgerProof.verified).toBe(true);
  }));
});
