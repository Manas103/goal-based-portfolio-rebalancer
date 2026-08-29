import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProposalService } from './proposal.service';
import { ProposalResponse } from './models';

/**
 * Disclosure-ledger console: a list of every proposal the engine has written,
 * each rendered with the server-built plain-English explanation
 * (ProposalResponse.explanation) rather than the console re-deriving the
 * rebalancing rule itself. Filtering by account id calls the same read-only
 * API a compliance reviewer would use to pull one account's full history.
 */
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css',
})
export class AppComponent {
  readonly proposals = signal<ProposalResponse[]>([]);
  readonly loadError = signal<string | null>(null);
  accountIdFilter = '';

  constructor(private readonly proposalService: ProposalService) {
    this.refresh();
  }

  refresh(): void {
    this.loadError.set(null);
    this.proposalService.list().subscribe({
      next: (proposals) => this.proposals.set(proposals),
      error: () => this.loadError.set('Could not reach the rebalancer backend at localhost:8080.'),
    });
  }

  filterByAccount(): void {
    const accountId = this.accountIdFilter.trim();
    if (!accountId) {
      this.refresh();
      return;
    }
    this.loadError.set(null);
    this.proposalService.byAccount(accountId).subscribe({
      next: (proposals) => this.proposals.set(proposals),
      error: () => this.loadError.set(`Could not load proposals for ${accountId}.`),
    });
  }
}
