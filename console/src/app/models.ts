/** Mirrors ProposalResponse.java field for field; the console never guesses a shape the backend does not send. */
export interface ProposalResponse {
  id: number;
  accountId: string;
  triggeringBands: string;
  ruleVersion: string;
  proposedAt: string;
  preEquityPct: number;
  preBondPct: number;
  preCashPct: number;
  postEquityPct: number;
  postBondPct: number;
  postCashPct: number;
  equityTradeAmount: number;
  bondTradeAmount: number;
  cashTradeAmount: number;
  explanation: string;
}
