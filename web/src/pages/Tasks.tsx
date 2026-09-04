import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, apiErrorMessage } from '../lib/api';
import type { Page } from '../lib/types';
import { Badge, EmptyState, Spinner } from '../components/ui';
import { Modal } from '../components/Modal';
import { dateTime } from '../lib/format';

interface Task {
  id: number; title: string; description?: string; type: string;
  leadReference?: string; dueAt?: string; reminderAt?: string; priority: string; status: string;
}

export default function Tasks() {
  const qc = useQueryClient();
  const [status, setStatus] = useState('OPEN');
  const [showCreate, setShowCreate] = useState(false);

  const { data, isLoading } = useQuery({
    queryKey: ['my-tasks', status],
    queryFn: async () => (await api.get<Page<Task>>('/tasks/mine', { params: { status, size: 50, sort: 'dueAt,asc' } })).data,
  });

  const update = useMutation({
    mutationFn: async ({ id, next }: { id: number; next: string }) => api.put(`/tasks/${id}`, { status: next }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['my-tasks'] }),
    onError: (e) => alert(apiErrorMessage(e)),
  });

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold text-slate-800">Task & Reminder Management</h1>
        <div className="flex gap-2">
          <select className="input max-w-[150px]" value={status} onChange={(e) => setStatus(e.target.value)}>
            {['OPEN', 'IN_PROGRESS', 'DONE', 'CANCELLED'].map((s) => <option key={s}>{s}</option>)}
          </select>
          <button className="btn-primary" onClick={() => setShowCreate(true)}>+ Task</button>
        </div>
      </div>

      <div className="card overflow-x-auto">
        {isLoading ? (
          <Spinner />
        ) : !data || data.empty ? (
          <EmptyState message="Nothing here." />
        ) : (
          <table className="min-w-full">
            <thead>
              <tr>
                <th className="th">Title</th>
                <th className="th">Type</th>
                <th className="th">Lead</th>
                <th className="th">Due</th>
                <th className="th">Priority</th>
                <th className="th">Status</th>
                <th className="th"></th>
              </tr>
            </thead>
            <tbody>
              {data.content.map((t) => (
                <tr key={t.id} className="hover:bg-slate-50">
                  <td className="td font-medium">{t.title}</td>
                  <td className="td text-xs">{t.type}</td>
                  <td className="td font-mono text-xs">{t.leadReference ?? '—'}</td>
                  <td className="td text-xs">{dateTime(t.dueAt)}</td>
                  <td className="td"><Badge value={t.priority} /></td>
                  <td className="td"><Badge value={t.status} /></td>
                  <td className="td text-right">
                    {t.status !== 'DONE' && (
                      <button className="btn-ghost px-3 py-1 text-xs" onClick={() => update.mutate({ id: t.id, next: 'DONE' })}>
                        Complete
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {showCreate && <CreateTaskModal onClose={() => setShowCreate(false)} onSaved={() => qc.invalidateQueries({ queryKey: ['my-tasks'] })} />}
    </div>
  );
}

function CreateTaskModal({ onClose, onSaved }: { onClose: () => void; onSaved: () => void }) {
  const [f, setF] = useState({ title: '', description: '', type: 'GENERAL', priority: 'MEDIUM', dueAt: '', reminderAt: '' });
  const set = (k: string, v: string) => setF((s) => ({ ...s, [k]: v }));
  const mut = useMutation({
    mutationFn: async () =>
      api.post('/tasks', { ...f, dueAt: f.dueAt || undefined, reminderAt: f.reminderAt || f.dueAt || undefined }),
    onSuccess: () => { onSaved(); onClose(); },
    onError: (e) => alert(apiErrorMessage(e)),
  });
  return (
    <Modal title="New Task" onClose={onClose}>
      <form className="space-y-3" onSubmit={(e) => { e.preventDefault(); mut.mutate(); }}>
        <div><label className="label">Title *</label><input className="input" required value={f.title} onChange={(e) => set('title', e.target.value)} /></div>
        <div><label className="label">Description</label><textarea className="input" rows={2} value={f.description} onChange={(e) => set('description', e.target.value)} /></div>
        <div className="grid grid-cols-2 gap-3">
          <div><label className="label">Type</label>
            <select className="input" value={f.type} onChange={(e) => set('type', e.target.value)}>
              {['FOLLOW_UP', 'CALLBACK', 'SITE_VISIT_PREP', 'DOCUMENT', 'PAYMENT_COLLECTION', 'GENERAL'].map((s) => <option key={s}>{s}</option>)}
            </select>
          </div>
          <div><label className="label">Priority</label>
            <select className="input" value={f.priority} onChange={(e) => set('priority', e.target.value)}>
              {['LOW', 'MEDIUM', 'HIGH', 'URGENT'].map((s) => <option key={s}>{s}</option>)}
            </select>
          </div>
          <div><label className="label">Due</label><input className="input" type="datetime-local" onChange={(e) => set('dueAt', e.target.value ? new Date(e.target.value).toISOString() : '')} /></div>
          <div><label className="label">Reminder</label><input className="input" type="datetime-local" onChange={(e) => set('reminderAt', e.target.value ? new Date(e.target.value).toISOString() : '')} /></div>
        </div>
        <div className="flex justify-end gap-2">
          <button type="button" className="btn-ghost" onClick={onClose}>Cancel</button>
          <button className="btn-primary" disabled={mut.isPending}>Save</button>
        </div>
      </form>
    </Modal>
  );
}
