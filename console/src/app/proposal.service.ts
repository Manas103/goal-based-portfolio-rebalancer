import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ProposalResponse } from './models';

/** Thin wrapper around ProposalController's read-only REST surface. */
@Injectable({ providedIn: 'root' })
export class ProposalService {
  private readonly baseUrl = 'http://localhost:8080/api/proposals';

  constructor(private readonly http: HttpClient) {}

  list(): Observable<ProposalResponse[]> {
    return this.http.get<ProposalResponse[]>(this.baseUrl);
  }

  byAccount(accountId: string): Observable<ProposalResponse[]> {
    return this.http.get<ProposalResponse[]>(`${this.baseUrl}/${accountId}`);
  }
}
