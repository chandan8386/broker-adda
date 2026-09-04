import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, apiErrorMessage } from '../lib/api';
import type { LeadListItem, Page, UserSummary } from '../lib/types';
import { Badge, EmptyState, Spinner } from '../components/ui';

export default function Assignments() {
  const qc = useQueryClient();
  const [selected, setSelected] = useState<number[]>([]);
  const [toUserId, setToUserId] = useState<number | ''>('');

  const unassigned = useQuery({
    queryKey: ['unassigned-leads'],
    queryFn: async () =>
      (await api.get<Page<LeadListItem>>('/leads', { params: { unassigned: true, size: 100, sort: 'createdAt,asc' } })).data,
  });
  const callers = useQuery({
    queryKey: ['users', 'CALLING_TEAM'],
    queryFn: async () => (await api.get<UserSummary[]>('/users/by-role/CALLING_TEAM')).data,
  });

  const assign = useMutation({
    mutationFn: async () =>
      api.post('/lead-assignments/assign', { leadIds: selected, toUserId, reason: 'Bulk assignment' }),
    onSuccess: () => {
      setSelected([]);
      setToUserId('');
      qc.invalidateQueries({ queryKey: ['unassigned-leads'] });
    },
    onError: (e) => alert(apiErrorMessage(e)),
  });

  const autoAssign = useMutation({
    mutationFn: async () => {
      const ids = (unassigned.data?.content ?? []).map((l) => l.id);
      const userIds = (callers.data ?? []).map((u) => u.id);
      return api.post('/lead-assignments/auto-assign', { leadIds: ids, userIds, strategy: 'ROUND_ROBIN' });
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: ['unassigned-leads'] }),
    onError: (e) => alert(apiErrorMessage(e)),
  });

  const toggle = (id: number) =>
    setSelected((s) => (s.includes(id) ? s.filter((x) => x !== id) : [...s, id]));

  if (unassigned.isLoading) return <Spinner />;

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-bold text-slate-800">Lead Assignment</h1>

      <div className="card flex flex-wrap items-center gap-2 p-3">
        <select className="input max-w-xs" value={toUserId} onChange={(e) => setToUserId(Number(e.target.value) || '')}>
          <option value="">Assign selected to…</option>
          {callers.data?.map((u) => <option key={u.id} value={u.id}>{u.fullName}</option>)}
        </select>
        <button className="btn-primary" disabled={!selected.length || !toUserId || assign.isPending} onClick={() => assign.mutate()}>
          Assign {selected.length || ''}
        </button>
        <button className="btn-ghost" disabled={!unassigned.data?.content.length || autoAssign.isPending} onClick={() => autoAssign.mutate()}>
          Auto-assign all (round-robin)
        </button>
      </div>

      <div className="card overflow-x-auto">
        {!unassigned.data || unassigned.data.empty ? (
          <EmptyState message="Every lead is assigned." />
        ) : (
          <table className="min-w-full">
            <thead>
              <tr>
                <th className="th"></th>
                <th className="th">Ref</th>
                <th className="th">Customer</th>
                <th className="th">Mobile</th>
                <th className="th">Source</th>
                <th className="th">Status</th>
              </tr>
            </thead>
            <tbody>
              {unassigned.data.content.map((l) => (
                <tr key={l.id} className="hover:bg-slate-50">
                  <td className="td"><input type="checkbox" checked={selected.includes(l.id)} onChange={() => toggle(l.id)} /></td>
                  <td className="td font-mono text-xs">{l.reference}</td>
                  <td className="td font-medium">{l.customerName}</td>
                  <td className="td">{l.mobile}</td>
                  <td className="td text-xs">{l.sourceChannel ?? '—'}</td>
                  <td className="td"><Badge value={l.status} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
