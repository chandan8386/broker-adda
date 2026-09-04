import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { api, apiErrorMessage } from '../lib/api';
import type { Page } from '../lib/types';
import { EmptyState, Spinner } from '../components/ui';
import { dateTime, fromNow } from '../lib/format';

interface FollowUp {
  id: number; leadId: number; leadName: string; ownerName?: string;
  dueAt: string; channel: string; status: string; notes?: string;
}

export default function FollowUps() {
  const qc = useQueryClient();
  const { data, isLoading } = useQuery({
    queryKey: ['my-followups'],
    queryFn: async () => (await api.get<Page<FollowUp>>('/calls/follow-ups/mine', { params: { size: 50, sort: 'dueAt,asc' } })).data,
  });

  const complete = useMutation({
    mutationFn: async (id: number) => api.post(`/calls/follow-ups/${id}/complete`, { note: 'Completed from dashboard' }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['my-followups'] }),
    onError: (e) => alert(apiErrorMessage(e)),
  });

  if (isLoading) return <Spinner />;

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-bold text-slate-800">Calling / Follow-up Management</h1>
      <div className="card overflow-x-auto">
        {!data || data.empty ? (
          <EmptyState message="No pending follow-ups. Great work!" />
        ) : (
          <table className="min-w-full">
            <thead>
              <tr>
                <th className="th">Lead</th>
                <th className="th">Channel</th>
                <th className="th">Due</th>
                <th className="th">Notes</th>
                <th className="th"></th>
              </tr>
            </thead>
            <tbody>
              {data.content.map((f) => {
                const overdue = new Date(f.dueAt) < new Date();
                return (
                  <tr key={f.id} className="hover:bg-slate-50">
                    <td className="td">
                      <Link className="font-medium text-brand-700 hover:underline" to={`/leads/${f.leadId}`}>{f.leadName}</Link>
                    </td>
                    <td className="td text-xs">{f.channel}</td>
                    <td className={`td text-xs ${overdue ? 'font-semibold text-rose-600' : ''}`} title={dateTime(f.dueAt)}>
                      {fromNow(f.dueAt)}
                    </td>
                    <td className="td text-xs text-slate-500">{f.notes ?? '—'}</td>
                    <td className="td text-right">
                      <button className="btn-ghost px-3 py-1 text-xs" onClick={() => complete.mutate(f.id)}>Mark done</button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
