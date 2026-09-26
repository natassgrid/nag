export interface AuditRecord {
  id: string;
  blockNumber: number;
  timestamp: string;
  service: string;
  actor: string;
  actorRole?: string;
  action: string;
  target: string;
  severity: 'INFO' | 'WARN' | 'ERROR' | 'CRITICAL';
  status: 'SUCCESS' | 'FAILURE';
  ipAddress?: string;
  hash: string;
  previousHash?: string;
  digitalSignature?: string;
  details?: Record<string, unknown>;
}
