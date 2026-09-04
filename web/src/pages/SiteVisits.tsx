import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, apiErrorMessage } from '../lib/api';
import type { Page, SiteVisit } from '../lib/types';
import { useCompanyMeta } from '../lib/meta';
import { Badge, EmptyState, Spinner } from '../components/ui';
import { dateTime } from '../lib/format';

export default function SiteVisits() {
  const qc = useQueryClient();
  const meta = useCompanyMeta();
  const [status, setStatus] = useState('SCHEDULED');

  const { data, isLoading } = useQuery({
    queryKey: ['site-visits', status],
    queryFn: async () => (await api.get<Page<SiteVisit>>('/site-visits', { params: { status, size: 50, sort: 'scheduledAt,asc' } })).data,
  });

  const result = useMutation({
    mutationFn: async ({ id, res }: { id: number; res: string }) =>
      api.post(`/site-visits/${id}/result`, { result: res, feedback: 'Recorded from list' }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['site-visits'] }),
    onError: (e) => alert(apiErrorMessage(e)),
  });

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold text-slate-800">Site Visit Management</h1>
        <select className="input max-w-[200px]" value={status} onChange={(e) => setStatus(e.target.value)}>
          {meta.data?.enums.siteVisitStatus.map((s) => <option key={s}>{s}</option>)}
        </select>
      </div>

      <div className="card overflow-x-auto">
        {isLoading ? (
          <Spinner />
        ) : !data || data.empty ? (
          <EmptyState message="No site visits in this state." />
        ) : (
          <table className="min-w-full">
            <thead>
              <tr>
                <th className="th">Lead</th>
                <th className="th">Property</th>
                <th className="th">Executive</th>
                <th className="th">Scheduled</th>
                <th className="th">Status</th>
                <th className="th">Result</th>
                <th className="th"></th>
              </tr>
            </thead>
            <tbody>
              {data.content.map((v) => (
                <tr key={v.id} className="hover:bg-slate-50">
                  <td className="td">
                    <Link className="font-medium text-brand-700 hover:underline" to={`/leads/${v.leadId}`}>{v.leadName}</Link>
                    <div className="text-xs text-slate-400">{v.leadMobile}</div>
                  </td>
                  <td className="td text-xs">{v.propertyTitle ?? '—'}</td>
                  <td className="td text-xs">{v.salesExecutiveName ?? '—'}</td>
                  <td className="td text-xs">{dateTime(v.scheduledAt)}</td>
                  <td className="td"><Badge value={v.status} /></td>
                  <td className="td"><Badge value={v.result} /></td>
                  <td className="td text-right">
                    {v.status !== 'COMPLETED' && (
                      <select
                        className="input max-w-[160px] py-1 text-xs"
                        defaultValue=""
                        onChange={(e) => e.target.value && result.mutate({ id: v.id, res: e.target.value })}
                      >
                        <option value="" disabled>Record result…</option>
                        {meta.data?.enums.siteVisitResult.map((s) => <option key={s}>{s}</option>)}
                      </select>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
