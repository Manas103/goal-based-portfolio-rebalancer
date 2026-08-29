import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ProposalService } from './proposal.service';

describe('ProposalService', () => {
  let service: ProposalService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ProposalService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('lists proposals from the backend ledger API', () => {
    service.list().subscribe();
    const req = httpMock.expectOne('http://localhost:8080/api/proposals');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('fetches one account\'s proposals by id', () => {
    service.byAccount('BREACH-EQ1').subscribe();
    const req = httpMock.expectOne('http://localhost:8080/api/proposals/BREACH-EQ1');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });
});
