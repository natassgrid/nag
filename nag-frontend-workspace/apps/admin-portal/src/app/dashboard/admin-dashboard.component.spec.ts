import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { AdminDashboardComponent } from './admin-dashboard.component';

describe('AdminDashboardComponent', () => {
  let component: AdminDashboardComponent;
  let fixture: ComponentFixture<AdminDashboardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        AdminDashboardComponent,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create admin dashboard', () => {
    expect(component).toBeTruthy();
  });

  it('should have initial system health services and security audit logs', () => {
    expect(component.systemServices().length).toBeGreaterThan(0);
    expect(component.auditEvents().length).toBeGreaterThan(0);
  });
});
