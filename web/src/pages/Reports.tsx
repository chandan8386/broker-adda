import { useQuery } from '@tanstack/react-query';
import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { api } from '../lib/api';
import type { ReportsResponse } from '../lib/types';
import { EmptyState, Spinner } from '../components/ui';

export default function Reports() {
  const { data, isLoading } = useQuery({
    queryKey: ['reports'],
    queryFn: async () => (await api.get<ReportsResponse>('/dashboard/reports')).data,
  });

  if (isLoading || !data) return <Spinner />;

  return (
    <div className="space-y-6">
      <h1 className="text-xl font-bold text-slate-800">Reports & Analytics</h1>

      <section className="card p-4">
        <h2 className="mb-3 text-sm font-semibold text-slate-700">Source-wise performance</h2>
        {data.sourceWise.length === 0 ? (
          <EmptyState message="No data." />
        ) : (
          <ResponsiveContainer width="100%" height={280}>
            <BarChart data={data.sourceWise}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} />
              <XAxis dataKey="source" fontSize={11} interval={0} angle={-20} textAnchor="end" height={60} />
              <YAxis fontSize={12} allowDecimals={false} />
              <Tooltip />
              <Legend />
              <Bar dataKey="totalLeads" name="Leads" fill="#0f766e" radius={[4, 4, 0, 0]} />
              <Bar dataKey="converted" name="Converted" fill="#f59e0b" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        )}
      </section>

      <section className="card overflow-x-auto p-4">
        <h2 className="mb-3 text-sm font-semibold text-slate-700">Calling team performance</h2>
        <table className="min-w-full">
          <thead>
            <tr>
              <th className="th">Agent</th><th className="th">Calls</th><th className="th">Connected</th>
              <th className="th">Interested</th><th className="th">Site visits</th><th className="th">Connect rate</th>
            </tr>
          </thead>
          <tbody>
            {data.callingTeam.map((r) => (
              <tr key={r.userId} className="hover:bg-slate-50">
                <td className="td font-medium">{r.name}</td>
                <td className="td">{r.totalCalls}</td>
                <td className="td">{r.connected}</td>
                <td className="td">{r.interested}</td>
                <td className="td">{r.siteVisits}</td>
                <td className="td">{r.connectRate}%</td>
              </tr>
            ))}
            {data.callingTeam.length === 0 && <tr><td className="td text-slate-400" colSpan={6}>No calls logged in range.</td></tr>}
          </tbody>
        </table>
      </section>

      <section className="card overflow-x-auto p-4">
        <h2 className="mb-3 text-sm font-semibold text-slate-700">Sales team performance</h2>
        <table className="min-w-full">
          <thead>
            <tr>
              <th className="th">Executive</th><th className="th">Assigned</th><th className="th">Interested</th>
              <th className="th">Purchased</th><th className="th">Close rate</th>
            </tr>
          </thead>
          <tbody>
            {data.salesTeam.map((r) => (
              <tr key={r.userId} className="hover:bg-slate-50">
                <td className="td font-medium">{r.name}</td>
                <td className="td">{r.assignedLeads}</td>
                <td className="td">{r.interested}</td>
                <td className="td">{r.purchased}</td>
                <td className="td">{r.closeRate}%</td>
              </tr>
            ))}
            {data.salesTeam.length === 0 && <tr><td className="td text-slate-400" colSpan={5}>No assigned leads.</td></tr>}
          </tbody>
        </table>
      </section>
    </div>
  );
}
