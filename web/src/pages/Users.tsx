import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, apiErrorMessage } from '../lib/api';
import type { Page } from '../lib/types';
import { Badge, EmptyState, Pagination, Spinner } from '../components/ui';
import { Modal } from '../components/Modal';

interface UserRow {
  id: number; fullName: string; username: string; email: string; phone?: string;
  teamName?: string; roles: string[]; active: boolean; lastLoginAt?: string;
}

export default function Users() {
  const qc = useQueryClient();
  const [page, setPage] = useState(0);
  const [showCreate, setShowCreate] = useState(false);

  const { data, isLoading } = useQuery({
    queryKey: ['users-list', page],
    queryFn: async () => (await api.get<Page<UserRow>>('/users', { params: { page, size: 20 } })).data,
  });

  const toggle = useMutation({
    mutationFn: async ({ id, active }: { id: number; active: boolean }) =>
      api.post(`/users/${id}/${active ? 'deactivate' : 'activate'}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['users-list'] }),
    onError: (e) => alert(apiErrorMessage(e)),
  });

  const remove = useMutation({
    mutationFn: async (id: number) => api.delete(`/users/${id}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['users-list'] }),
    // The API refuses deletion when a user still owns leads, calls or site visits and
    // explains what is blocking it. Surface that verbatim rather than a generic failure.
    onError: (e) => alert(apiErrorMessage(e)),
  });

  const confirmDelete = (u: UserRow) => {
    const ok = window.confirm(
      `Permanently delete ${u.fullName} (${u.username})?\n\n` +
        'This cannot be undone. If they already own leads, calls or site visits the ' +
        'deletion will be refused — deactivate them instead so that history is kept.',
    );
    if (ok) remove.mutate(u.id);
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold text-slate-800">User / Team Management</h1>
        <button className="btn-primary" onClick={() => setShowCreate(true)}>+ User</button>
      </div>
      <div className="card overflow-x-auto">
        {isLoading ? (
          <Spinner />
        ) : !data || data.empty ? (
          <EmptyState message="No users." />
        ) : (
          <table className="min-w-full">
            <thead>
              <tr>
                <th className="th">Name</th><th className="th">Username</th><th className="th">Email</th>
                <th className="th">Team</th><th className="th">Roles</th><th className="th">Active</th>
                <th className="th text-right">Actions</th>
              </tr>
            </thead>
            <tbody>
              {data.content.map((u) => (
                <tr key={u.id} className="hover:bg-slate-50">
                  <td className="td font-medium">{u.fullName}</td>
                  <td className="td text-xs">{u.username}</td>
                  <td className="td text-xs">{u.email}</td>
                  <td className="td text-xs">{u.teamName ?? '—'}</td>
                  <td className="td">
                    <div className="flex flex-wrap gap-1">{u.roles.map((r) => <Badge key={r} value={r} />)}</div>
                  </td>
                  <td className="td">{u.active ? '✅' : '⛔'}</td>
                  <td className="td">
                    <div className="flex justify-end gap-2">
                      <button
                        className="btn-ghost px-3 py-1 text-xs"
                        disabled={toggle.isPending}
                        onClick={() => toggle.mutate({ id: u.id, active: u.active })}
                      >
                        {u.active ? 'Deactivate' : 'Activate'}
                      </button>
                      <button
                        className="btn px-3 py-1 text-xs bg-rose-50 text-rose-700 hover:bg-rose-100"
                        disabled={remove.isPending}
                        onClick={() => confirmDelete(u)}
                      >
                        Delete
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {data && <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />}
      </div>

      {showCreate && <CreateUserModal onClose={() => setShowCreate(false)} onSaved={() => qc.invalidateQueries({ queryKey: ['users-list'] })} />}
    </div>
  );
}

function CreateUserModal({ onClose, onSaved }: { onClose: () => void; onSaved: () => void }) {
  const [f, setF] = useState({ fullName: '', username: '', email: '', password: 'Password@123', phone: '', role: 'CALLING_TEAM' });
  const set = (k: string, v: string) => setF((s) => ({ ...s, [k]: v }));
  const mut = useMutation({
    mutationFn: async () =>
      api.post('/users', { fullName: f.fullName, username: f.username, email: f.email, password: f.password, phone: f.phone, roles: [f.role], active: true }),
    onSuccess: () => { onSaved(); onClose(); },
    onError: (e) => alert(apiErrorMessage(e)),
  });
  return (
    <Modal title="New User" onClose={onClose}>
      <form className="grid grid-cols-2 gap-3" onSubmit={(e) => { e.preventDefault(); mut.mutate(); }}>
        <div className="col-span-2"><label className="label">Full name *</label><input className="input" required value={f.fullName} onChange={(e) => set('fullName', e.target.value)} /></div>
        <div><label className="label">Username *</label><input className="input" required value={f.username} onChange={(e) => set('username', e.target.value)} /></div>
        <div><label className="label">Email *</label><input className="input" type="email" required value={f.email} onChange={(e) => set('email', e.target.value)} /></div>
        <div><label className="label">Phone</label><input className="input" value={f.phone} onChange={(e) => set('phone', e.target.value)} /></div>
        <div><label className="label">Temp password *</label><input className="input" required value={f.password} onChange={(e) => set('password', e.target.value)} /></div>
        <div className="col-span-2"><label className="label">Role</label>
          <select className="input" value={f.role} onChange={(e) => set('role', e.target.value)}>
            {['ADMIN', 'SALES_MANAGER', 'CALLING_TEAM', 'SALES_EXECUTIVE'].map((s) => <option key={s}>{s}</option>)}
          </select>
        </div>
        <div className="col-span-2 flex justify-end gap-2">
          <button type="button" className="btn-ghost" onClick={onClose}>Cancel</button>
          <button className="btn-primary" disabled={mut.isPending}>Create</button>
        </div>
      </form>
    </Modal>
  );
}
