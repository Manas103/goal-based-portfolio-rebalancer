import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { AppComponent } from './app.component';
import { ProposalResponse } from './models';

describe('AppComponent', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  const sampleProposal: ProposalResponse = {
    id: 1,
    accountId: 'BREACH-EQ1',
    triggeringBands: 'EQUITY_DRIFT',
    ruleVersion: 'drift-band-v1',
    proposedAt: '2026-08-29T00:00:00Z',
    preEquityPct: 66, preBondPct: 32, preCashPct: 2,
    postEquityPct: 60, postBondPct: 32, postCashPct: 8,
    equityTradeAmount: -6000, bondTradeAmount: 0, cashTradeAmount: 6000,
    explanation: 'equity drift fired. Equity 66.00%->60.00%, bond 32.00%->32.00%, cash 2.00%->8.00%. Proposed trade: equity -$6000, bond +$0, cash +$6000 (rule drift-band-v1).',
  };

  it('creates the console and requests the full proposal list on init', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();

    const req = httpMock.expectOne('http://localhost:8080/api/proposals');
    expect(req.request.method).toBe('GET');
    req.flush([sampleProposal]);

    expect(fixture.componentInstance.proposals()).toEqual([sampleProposal]);
  });

  it('renders the server-built explanation text for a proposal', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    httpMock.expectOne('http://localhost:8080/api/proposals').flush([sampleProposal]);
    fixture.detectChanges();

    const rendered = fixture.nativeElement.textContent as string;
    expect(rendered).toContain('equity drift fired');
  });

  it('filtering by account id calls the per-account endpoint', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    httpMock.expectOne('http://localhost:8080/api/proposals').flush([]);

    fixture.componentInstance.accountIdFilter = 'BREACH-EQ1';
    fixture.componentInstance.filterByAccount();
    const req = httpMock.expectOne('http://localhost:8080/api/proposals/BREACH-EQ1');
    expect(req.request.method).toBe('GET');
    req.flush([sampleProposal]);

    expect(fixture.componentInstance.proposals()).toEqual([sampleProposal]);
  });

  it('surfaces a load error rather than failing silently when the backend is unreachable', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const req = httpMock.expectOne('http://localhost:8080/api/proposals');
    req.error(new ProgressEvent('network error'));

    expect(fixture.componentInstance.loadError()).toContain('rebalancer backend');
  });
});
