import { useQuery } from '@tanstack/react-query';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { api } from '../lib/api';
import type { DashboardSummary } from '../lib/types';
import { Spinner, StatCard } from '../components/ui';
import { money, titleCase } from '../lib/format';

const PIE_COLORS = ['#0f766e', '#2dd4bf', '#f59e0b', '#6366f1', '#ef4444', '#8b5cf6', '#10b981', '#0ea5e9'];

export default function Dashboard() {
  const { data, isLoading } = useQuery({
    queryKey: ['dashboard-summary'],
    queryFn: async () => (await api.get<DashboardSummary>('/dashboard/summary')).data,
  });

  if (isLoading || !data) return <Spinner />;

  const statusData = Object.entries(data.leadsByStatus)
    .filter(([, v]) => v > 0)
    .map(([name, value]) => ({ name: titleCase(name), value }));

  const funnel = [
    { stage: 'Total', value: data.totalLeads },
    { stage: 'Interested', value: data.interestedLeads },
    { stage: 'Site Visits', value: data.siteVisitsScheduled + data.siteVisitsCompleted },
    { stage: 'Bookings', value: data.bookings },
    { stage: 'Purchases', value: data.purchases },
  ];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-xl font-bold text-slate-800">Dashboard</h1>
        <p className="text-sm text-slate-400">Shri Trishakti Infra Realtors Pvt Ltd</p>
      </div>

      <div className="grid grid-cols-2 gap-3 md:grid-cols-4 xl:grid-cols-5">
        <StatCard label="Total Leads" value={data.totalLeads} />
        <StatCard label="New Leads" value={data.newLeads} tone="warn" />
        <StatCard label="Today's Follow-ups" value={data.todaysFollowUps} tone="warn" hint={`${data.overdueFollowUps} overdue`} />
        <StatCard label="Interested" value={data.interestedLeads} tone="good" />
        <StatCard label="Site Visits Scheduled" value={data.siteVisitsScheduled} />
        <StatCard label="Site Visits Done" value={data.siteVisitsCompleted} />
        <StatCard label="Bookings" value={data.bookings} tone="good" />
        <StatCard label="Purchases" value={data.purchases} tone="good" />
        <StatCard label="Lost Leads" value={data.lostLeads} tone="bad" />
        <StatCard label="Conversion" value={`${data.leadConversionPercent}%`} tone="good" hint={money(data.totalSalesValue)} />
      </div>

      <div className="grid gap-4 lg:grid-cols-2">
        <div className="card p-4">
          <p className="mb-2 text-sm font-semibold text-slate-700">Sales funnel</p>
          <ResponsiveContainer width="100%" height={260}>
            <BarChart data={funnel}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} />
              <XAxis dataKey="stage" fontSize={12} />
              <YAxis fontSize={12} allowDecimals={false} />
              <Tooltip />
              <Bar dataKey="value" fill="#0f766e" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>

        <div className="card p-4">
          <p className="mb-2 text-sm font-semibold text-slate-700">Leads by status</p>
          <ResponsiveContainer width="100%" height={260}>
            <PieChart>
              <Pie data={statusData} dataKey="value" nameKey="name" outerRadius={95} label>
                {statusData.map((_, i) => (
                  <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} />
                ))}
              </Pie>
              <Tooltip />
            </PieChart>
          </ResponsiveContainer>
        </div>
      </div>
    </div>
  );
}
