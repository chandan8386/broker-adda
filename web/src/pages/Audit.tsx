import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../lib/api';
import type { Page } from '../lib/types';
import { EmptyState, Pagination, Spinner } from '../components/ui';
import { dateTime } from '../lib/format';

interface AuditRow {
  id: number; actorUsername?: string; action: string; entityType?: string;
  entityId?: number; ipAddress?: string; at: string;
}

export default function Audit() {
  const [page, setPage] = useState(0);
  const [entityType, setEntityType] = useState('');
  const { data, isLoading } = useQuery({
    queryKey: ['audit', page, entityType],
    queryFn: async () =>
      (await api.get<Page<AuditRow>>('/audit-logs', { params: { page, size: 30, sort: 'at,desc', entityType: entityType || undefined } })).data,
  });

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-bold text-slate-800">Audit Logs</h1>
      <div className="card p-3">
        <input className="input max-w-xs" placeholder="Filter by entity type (e.g. Lead)" value={entityType} onChange={(e) => { setPage(0); setEntityType(e.target.value); }} />
      </div>
      <div className="card overflow-x-auto">
        {isLoading ? (
          <Spinner />
        ) : !data || data.empty ? (
          <EmptyState message="No audit entries." />
        ) : (
          <table className="min-w-full">
            <thead>
              <tr>
                <th className="th">When</th><th className="th">Actor</th><th className="th">Action</th>
                <th className="th">Entity</th><th className="th">IP</th>
              </tr>
            </thead>
            <tbody>
              {data.content.map((a) => (
                <tr key={a.id} className="hover:bg-slate-50">
                  <td className="td text-xs">{dateTime(a.at)}</td>
                  <td className="td text-xs">{a.actorUsername ?? '—'}</td>
                  <td className="td text-xs font-medium">{a.action}</td>
                  <td className="td text-xs">{a.entityType}{a.entityId ? ` #${a.entityId}` : ''}</td>
                  <td className="td text-xs">{a.ipAddress ?? '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {data && <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />}
      </div>
    </div>
  );
}
