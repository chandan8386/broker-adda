import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, apiErrorMessage } from '../lib/api';
import type { Activity, LeadResponse } from '../lib/types';
import { useCompanyMeta } from '../lib/meta';
import { Badge, Spinner } from '../components/ui';
import { dateTime, money, titleCase } from '../lib/format';
import { useAuth } from '../auth/AuthContext';

export default function LeadDetail() {
  const { id } = useParams();
  const qc = useQueryClient();
  const meta = useCompanyMeta();
  const { hasRole } = useAuth();
  const [tab, setTab] = useState<'timeline' | 'call' | 'followup' | 'visit'>('timeline');

  const leadQ = useQuery({
    queryKey: ['lead', id],
    queryFn: async () => (await api.get<LeadResponse>(`/leads/${id}`)).data,
  });
  const actsQ = useQuery({
    queryKey: ['lead-activities', id],
    queryFn: async () => (await api.get<Activity[]>(`/leads/${id}/activities`)).data,
  });

  const invalidate = () => {
    qc.invalidateQueries({ queryKey: ['lead', id] });
    qc.invalidateQueries({ queryKey: ['lead-activities', id] });
  };

  const transition = useMutation({
    mutationFn: async (body: Record<string, unknown>) => (await api.post(`/leads/${id}/transition`, body)).data,
    onSuccess: invalidate,
    onError: (e) => alert(apiErrorMessage(e)),
  });

  if (leadQ.isLoading || !leadQ.data) return <Spinner />;
  const lead = leadQ.data;
  const nextStates = meta.data?.leadWorkflow[lead.status] ?? [];

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="font-mono text-xs text-slate-400">{lead.reference}</p>
          <h1 className="text-xl font-bold text-slate-800">{lead.customerName}</h1>
          <p className="text-sm text-slate-500">{lead.mobile}{lead.email ? ` · ${lead.email}` : ''}</p>
        </div>
        <div className="flex items-center gap-2">
          <Badge value={lead.priority} />
          <Badge value={lead.status} />
        </div>
      </div>

      <div className="grid gap-4 lg:grid-cols-3">
        <div className="card space-y-2 p-4 text-sm">
          <Row label="Source" value={`${lead.source ?? '—'}${lead.sourceChannel ? ` (${titleCase(lead.sourceChannel)})` : ''}`} />
          <Row label="Property type" value={titleCase(lead.propertyType)} />
          <Row label="Budget" value={`${money(lead.budgetMin)} – ${money(lead.budgetMax)}`} />
          <Row label="Preferred location" value={lead.preferredLocation ?? '—'} />
          <Row label="Assigned to" value={lead.assignedUserName ?? 'Unassigned'} />
          <Row label="Next follow-up" value={dateTime(lead.nextFollowUpAt)} />
          <Row label="Last contacted" value={dateTime(lead.lastContactedAt)} />
          <Row label="Created" value={`${dateTime(lead.createdAt)} by ${lead.createdBy ?? '—'}`} />
          {lead.lostReason && <Row label="Lost reason" value={lead.lostReason} />}
          {lead.notes && <div className="pt-2 text-slate-600"><p className="label">Notes</p>{lead.notes}</div>}
        </div>

        <div className="card p-4 lg:col-span-2">
          <div className="mb-3 flex gap-1 border-b border-slate-100 text-sm">
            {(['timeline', 'call', 'followup', 'visit'] as const).map((t) => (
              <button
                key={t}
                className={`px-3 py-2 font-medium ${tab === t ? 'border-b-2 border-brand-600 text-brand-700' : 'text-slate-500'}`}
                onClick={() => setTab(t)}
              >
                {t === 'timeline' ? 'Timeline' : t === 'call' ? 'Log Call' : t === 'followup' ? 'Follow-up' : 'Site Visit'}
              </button>
            ))}
          </div>

          {tab === 'timeline' && (
            <ol className="relative space-y-4 border-l-2 border-slate-100 pl-4">
              {actsQ.data?.map((a) => (
                <li key={a.id} className="relative">
                  <span className="absolute -left-[21px] top-1 h-3 w-3 rounded-full bg-brand-500" />
                  <p className="text-sm font-medium text-slate-700">{a.summary}</p>
                  {a.detail && <p className="text-sm text-slate-500">{a.detail}</p>}
                  <p className="text-xs text-slate-400">{a.actorName} · {dateTime(a.occurredAt)}</p>
                </li>
              ))}
              {!actsQ.data?.length && <p className="text-sm text-slate-400">No activity yet.</p>}
            </ol>
          )}

          {tab === 'call' && <LogCallForm leadId={lead.id} onDone={invalidate} />}
          {tab === 'followup' && <FollowUpForm leadId={lead.id} onDone={invalidate} />}
          {tab === 'visit' && <SiteVisitForm leadId={lead.id} onDone={invalidate} />}
        </div>
      </div>

      <div className="card p-4">
        <p className="label">Move to next status</p>
        <div className="flex flex-wrap gap-2">
          {nextStates.length === 0 && <p className="text-sm text-slate-400">This lead is in a terminal state.</p>}
          {nextStates.map((s) => (
            <button
              key={s}
              className="btn-ghost"
              disabled={transition.isPending}
              onClick={() => {
                const body: Record<string, unknown> = { targetStatus: s };
                if (s === 'LOST') body.lostReason = prompt('Reason for marking lost?') ?? 'Not specified';
                if (s === 'SITE_VISIT_SCHEDULED') {
                  const when = prompt('Site visit date-time (ISO, e.g. 2026-09-20T10:00:00Z)');
                  if (!when) return;
                  body.siteVisitAt = when;
                }
                transition.mutate(body);
              }}
            >
              → {titleCase(s)}
            </button>
          ))}
        </div>
        {hasRole('CALLING_TEAM', 'SALES_MANAGER', 'ADMIN', 'SALES_EXECUTIVE') && (
          <button
            className="mt-3 text-sm text-brand-700 hover:underline"
            onClick={async () => {
              const note = prompt('Add a note to the timeline');
              if (note) {
                await api.post(`/leads/${lead.id}/notes`, { note });
                invalidate();
              }
            }}
          >
            + Add note
          </button>
        )}
      </div>
    </div>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between gap-4">
      <span className="text-slate-400">{label}</span>
      <span className="text-right font-medium text-slate-700">{value}</span>
    </div>
  );
}

function LogCallForm({ leadId, onDone }: { leadId: number; onDone: () => void }) {
  const meta = useCompanyMeta();
  const [f, setF] = useState({ outcome: 'CONNECTED', disposition: 'FOLLOW_UP', durationSeconds: '', notes: '', nextFollowUpAt: '' });
  const mut = useMutation({
    mutationFn: async () =>
      (await api.post('/calls', {
        leadId,
        outcome: f.outcome,
        disposition: f.disposition,
        durationSeconds: f.durationSeconds ? Number(f.durationSeconds) : undefined,
        notes: f.notes || undefined,
        nextFollowUpAt: f.nextFollowUpAt || undefined,
      })).data,
    onSuccess: onDone,
    onError: (e) => alert(apiErrorMessage(e)),
  });
  return (
    <form className="space-y-3" onSubmit={(e) => { e.preventDefault(); mut.mutate(); }}>
      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="label">Outcome</label>
          <select className="input" value={f.outcome} onChange={(e) => setF({ ...f, outcome: e.target.value })}>
            {meta.data?.enums.callOutcome.map((s) => <option key={s}>{s}</option>)}
          </select>
        </div>
        <div>
          <label className="label">Disposition</label>
          <select className="input" value={f.disposition} onChange={(e) => setF({ ...f, disposition: e.target.value })}>
            {meta.data?.enums.callDisposition.map((s) => <option key={s}>{s}</option>)}
          </select>
        </div>
        <div>
          <label className="label">Duration (sec)</label>
          <input className="input" type="number" value={f.durationSeconds} onChange={(e) => setF({ ...f, durationSeconds: e.target.value })} />
        </div>
        <div>
          <label className="label">Next follow-up</label>
          <input className="input" type="datetime-local" value={f.nextFollowUpAt} onChange={(e) => setF({ ...f, nextFollowUpAt: e.target.value ? new Date(e.target.value).toISOString() : '' })} />
        </div>
      </div>
      <div>
        <label className="label">Notes</label>
        <textarea className="input" rows={3} value={f.notes} onChange={(e) => setF({ ...f, notes: e.target.value })} />
      </div>
      <button className="btn-primary" disabled={mut.isPending}>Save call</button>
    </form>
  );
}

function FollowUpForm({ leadId, onDone }: { leadId: number; onDone: () => void }) {
  const [dueAt, setDueAt] = useState('');
  const [notes, setNotes] = useState('');
  const mut = useMutation({
    mutationFn: async () => (await api.post('/calls/follow-ups', { leadId, dueAt, notes: notes || undefined })).data,
    onSuccess: onDone,
    onError: (e) => alert(apiErrorMessage(e)),
  });
  return (
    <form className="space-y-3" onSubmit={(e) => { e.preventDefault(); mut.mutate(); }}>
      <div>
        <label className="label">Due at *</label>
        <input className="input" type="datetime-local" required onChange={(e) => setDueAt(e.target.value ? new Date(e.target.value).toISOString() : '')} />
      </div>
      <div>
        <label className="label">Notes</label>
        <textarea className="input" rows={3} value={notes} onChange={(e) => setNotes(e.target.value)} />
      </div>
      <button className="btn-primary" disabled={mut.isPending}>Schedule follow-up</button>
    </form>
  );
}

function SiteVisitForm({ leadId, onDone }: { leadId: number; onDone: () => void }) {
  const [scheduledAt, setAt] = useState('');
  const [pickupRequired, setPickup] = useState(false);
  const mut = useMutation({
    mutationFn: async () => (await api.post('/site-visits', { leadId, scheduledAt, pickupRequired })).data,
    onSuccess: onDone,
    onError: (e) => alert(apiErrorMessage(e)),
  });
  return (
    <form className="space-y-3" onSubmit={(e) => { e.preventDefault(); mut.mutate(); }}>
      <div>
        <label className="label">Scheduled at *</label>
        <input className="input" type="datetime-local" required onChange={(e) => setAt(e.target.value ? new Date(e.target.value).toISOString() : '')} />
      </div>
      <label className="flex items-center gap-2 text-sm">
        <input type="checkbox" checked={pickupRequired} onChange={(e) => setPickup(e.target.checked)} />
        Pickup required
      </label>
      <button className="btn-primary" disabled={mut.isPending}>Schedule site visit</button>
    </form>
  );
}
