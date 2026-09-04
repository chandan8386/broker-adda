import clsx from 'clsx';
import type { ReactNode } from 'react';
import { titleCase } from '../lib/format';

const STATUS_COLORS: Record<string, string> = {
  NEW: 'bg-slate-100 text-slate-700',
  ASSIGNED: 'bg-blue-100 text-blue-700',
  CALLING: 'bg-indigo-100 text-indigo-700',
  CONNECTED: 'bg-cyan-100 text-cyan-700',
  NOT_CONNECTED: 'bg-amber-100 text-amber-700',
  INTERESTED: 'bg-emerald-100 text-emerald-700',
  NOT_INTERESTED: 'bg-rose-100 text-rose-700',
  SITE_VISIT_SCHEDULED: 'bg-violet-100 text-violet-700',
  SITE_VISIT_DONE: 'bg-purple-100 text-purple-700',
  NEGOTIATION: 'bg-orange-100 text-orange-700',
  BOOKING: 'bg-teal-100 text-teal-700',
  PURCHASED: 'bg-green-100 text-green-800',
  CLOSED: 'bg-slate-200 text-slate-700',
  LOST: 'bg-red-100 text-red-700',
  HOT: 'bg-red-100 text-red-700',
  WARM: 'bg-amber-100 text-amber-700',
  COLD: 'bg-sky-100 text-sky-700',
  SCHEDULED: 'bg-violet-100 text-violet-700',
  COMPLETED: 'bg-green-100 text-green-800',
  CANCELLED: 'bg-slate-200 text-slate-600',
  AVAILABLE: 'bg-emerald-100 text-emerald-700',
  BOOKED: 'bg-teal-100 text-teal-700',
  SOLD: 'bg-green-100 text-green-800',
  HELD: 'bg-amber-100 text-amber-700',
};

export function Badge({ value }: { value?: string | null }) {
  if (!value) return <span className="text-slate-400">—</span>;
  return <span className={clsx('badge', STATUS_COLORS[value] ?? 'bg-slate-100 text-slate-700')}>{titleCase(value)}</span>;
}

export function Spinner() {
  return (
    <div className="flex items-center justify-center p-8 text-slate-400">
      <div className="h-6 w-6 animate-spin rounded-full border-2 border-slate-300 border-t-brand-600" />
    </div>
  );
}

export function StatCard({ label, value, hint, tone = 'default' }: {
  label: string; value: ReactNode; hint?: string; tone?: 'default' | 'good' | 'warn' | 'bad';
}) {
  const toneClass = {
    default: 'text-slate-900', good: 'text-emerald-600', warn: 'text-amber-600', bad: 'text-rose-600',
  }[tone];
  return (
    <div className="card p-4">
      <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</p>
      <p className={clsx('mt-1 text-2xl font-bold', toneClass)}>{value}</p>
      {hint && <p className="mt-0.5 text-xs text-slate-400">{hint}</p>}
    </div>
  );
}

export function Pagination({ page, totalPages, onChange }: {
  page: number; totalPages: number; onChange: (p: number) => void;
}) {
  if (totalPages <= 1) return null;
  return (
    <div className="flex items-center justify-between px-3 py-2 text-sm text-slate-500">
      <span>Page {page + 1} of {totalPages}</span>
      <div className="flex gap-2">
        <button className="btn-ghost px-3 py-1" disabled={page === 0} onClick={() => onChange(page - 1)}>Prev</button>
        <button className="btn-ghost px-3 py-1" disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>Next</button>
      </div>
    </div>
  );
}

export function EmptyState({ message }: { message: string }) {
  return <div className="p-8 text-center text-sm text-slate-400">{message}</div>;
}
