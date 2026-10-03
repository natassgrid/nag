import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { VerificationComponent } from './verification.component';

describe('VerificationComponent', () => {
  let component: VerificationComponent;
  let fixture: ComponentFixture<VerificationComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        HttpClientTestingModule,
        VerificationComponent,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(VerificationComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create verification component', () => {
    expect(component).toBeTruthy();
    expect(component.verifiedPaperResult()).toBeNull();
    expect(component.verifiedCandidateResult()).toBeNull();
  });

  it('should switch tabs between paper and candidate', () => {
    component.setTab('candidate');
    expect(component.activeTab()).toBe('candidate');

    component.setTab('paper');
    expect(component.activeTab()).toBe('paper');
  });

  it('should not verify if searchQuery is empty', () => {
    component.searchQuery = '  ';
    component.verify();
    expect(component.verifying()).toBe(false);
    expect(component.verifiedPaperResult()).toBeNull();
  });

  it('should verify paper root hash and produce ledger proof via API', fakeAsync(() => {
    const hash = '0xe3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855';
    component.setTab('paper');
    component.searchQuery = hash;
    component.verify();
    expect(component.verifying()).toBe(true);

    const req = httpMock.expectOne(`/api/v1/papers/public/verify?hash=${encodeURIComponent(hash)}`);
    expect(req.request.method).toBe('GET');
    req.flush({
      verified: true,
      paperId: 'p100',
      examId: 'e100',
      variant: 'SET-A',
      paperRootHash: hash,
      manifestDigest: 'd41d8cd98f00b204e9800998ecf8427e',
      ledgerTxHash: '0x3a92ff19b882ac0018f2894b9812cc93198ba11124ad90019283ba8712399182',
      consensusTimestamp: '1727956000.184920000',
      blockNumber: 1849202,
      ledgerExplorerUrl: 'https://hashscan.io/testnet/transaction/0x3a92ff19b882ac0018f2894b9812cc93198ba11124ad90019283ba8712399182',
      ledgerNetwork: 'HEDERA_CONSENSUS_SERVICE_TESTNET',
      tamperDetected: false,
    });

    expect(component.verifying()).toBe(false);
    expect(component.verifiedPaperResult()).toBeTruthy();
    expect(component.verifiedPaperResult()?.verified).toBe(true);
    expect(component.verifiedPaperResult()?.ledgerNetwork).toContain('HEDERA');
  }));

  it('should verify candidate credential and produce proof', fakeAsync(() => {
    component.setTab('candidate');
    component.searchQuery = '849202';
    component.verify();
    expect(component.verifying()).toBe(true);

    tick(1000);
    expect(component.verifying()).toBe(false);
    expect(component.verifiedCandidateResult()).toBeTruthy();
    expect(component.verifiedCandidateResult()?.ledgerProof.verified).toBe(true);
  }));
});
