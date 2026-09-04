import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, apiErrorMessage } from '../lib/api';
import type { Page, Property } from '../lib/types';
import { useCompanyMeta } from '../lib/meta';
import { useAuth } from '../auth/AuthContext';
import { Badge, EmptyState, Pagination, Spinner } from '../components/ui';
import { Modal } from '../components/Modal';
import { money } from '../lib/format';

export default function Properties() {
  const qc = useQueryClient();
  const meta = useCompanyMeta();
  const { hasRole } = useAuth();
  const [page, setPage] = useState(0);
  const [q, setQ] = useState('');
  const [type, setType] = useState('');
  const [status, setStatus] = useState('');
  const [showCreate, setShowCreate] = useState(false);

  const params = { page, size: 20, q: q || undefined, type: type || undefined, status: status || undefined };
  const { data, isLoading } = useQuery({
    queryKey: ['properties', params],
    queryFn: async () => (await api.get<Page<Property>>('/properties', { params })).data,
  });

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold text-slate-800">Property Management</h1>
        {hasRole('ADMIN', 'SALES_MANAGER') && <button className="btn-primary" onClick={() => setShowCreate(true)}>+ Property</button>}
      </div>

      <div className="card flex flex-wrap gap-2 p-3">
        <input className="input max-w-xs" placeholder="Search title / location / city" value={q} onChange={(e) => { setPage(0); setQ(e.target.value); }} />
        <select className="input max-w-[160px]" value={type} onChange={(e) => { setPage(0); setType(e.target.value); }}>
          <option value="">All types</option>
          {meta.data?.enums.propertyType.map((s) => <option key={s}>{s}</option>)}
        </select>
        <select className="input max-w-[160px]" value={status} onChange={(e) => { setPage(0); setStatus(e.target.value); }}>
          <option value="">All statuses</option>
          {meta.data?.enums.propertyStatus.map((s) => <option key={s}>{s}</option>)}
        </select>
      </div>

      <div className="card overflow-x-auto">
        {isLoading ? (
          <Spinner />
        ) : !data || data.empty ? (
          <EmptyState message="No properties found." />
        ) : (
          <table className="min-w-full">
            <thead>
              <tr>
                <th className="th">Title</th>
                <th className="th">Project</th>
                <th className="th">Type</th>
                <th className="th">Location</th>
                <th className="th">Area</th>
                <th className="th">Price</th>
                <th className="th">Status</th>
              </tr>
            </thead>
            <tbody>
              {data.content.map((p) => (
                <tr key={p.id} className="hover:bg-slate-50">
                  <td className="td font-medium">{p.title}</td>
                  <td className="td text-xs">{p.projectName ?? '—'}</td>
                  <td className="td text-xs">{p.propertyType}</td>
                  <td className="td text-xs">{[p.location, p.city].filter(Boolean).join(', ') || '—'}</td>
                  <td className="td text-xs">{p.areaSqft ? `${p.areaSqft} sqft` : '—'}</td>
                  <td className="td text-xs">{money(p.price)}</td>
                  <td className="td"><Badge value={p.status} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {data && <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />}
      </div>

      {showCreate && <CreatePropertyModal onClose={() => setShowCreate(false)} onSaved={() => qc.invalidateQueries({ queryKey: ['properties'] })} />}
    </div>
  );
}

function CreatePropertyModal({ onClose, onSaved }: { onClose: () => void; onSaved: () => void }) {
  const meta = useCompanyMeta();
  const [f, setF] = useState({ title: '', propertyType: 'FLAT', unitNumber: '', location: '', city: '', areaSqft: '', price: '', bedrooms: '', bathrooms: '', status: 'AVAILABLE' });
  const set = (k: string, v: string) => setF((s) => ({ ...s, [k]: v }));
  const mut = useMutation({
    mutationFn: async () =>
      api.post('/properties', {
        ...f,
        areaSqft: f.areaSqft ? Number(f.areaSqft) : undefined,
        price: f.price ? Number(f.price) : undefined,
        bedrooms: f.bedrooms ? Number(f.bedrooms) : undefined,
        bathrooms: f.bathrooms ? Number(f.bathrooms) : undefined,
      }),
    onSuccess: () => { onSaved(); onClose(); },
    onError: (e) => alert(apiErrorMessage(e)),
  });
  return (
    <Modal title="New Property" onClose={onClose}>
      <form className="grid grid-cols-2 gap-3" onSubmit={(e) => { e.preventDefault(); mut.mutate(); }}>
        <div className="col-span-2"><label className="label">Title *</label><input className="input" required value={f.title} onChange={(e) => set('title', e.target.value)} /></div>
        <div><label className="label">Type</label>
          <select className="input" value={f.propertyType} onChange={(e) => set('propertyType', e.target.value)}>
            {meta.data?.enums.propertyType.map((s) => <option key={s}>{s}</option>)}
          </select>
        </div>
        <div><label className="label">Unit #</label><input className="input" value={f.unitNumber} onChange={(e) => set('unitNumber', e.target.value)} /></div>
        <div><label className="label">Location</label><input className="input" value={f.location} onChange={(e) => set('location', e.target.value)} /></div>
        <div><label className="label">City</label><input className="input" value={f.city} onChange={(e) => set('city', e.target.value)} /></div>
        <div><label className="label">Area (sqft)</label><input className="input" type="number" value={f.areaSqft} onChange={(e) => set('areaSqft', e.target.value)} /></div>
        <div><label className="label">Price</label><input className="input" type="number" value={f.price} onChange={(e) => set('price', e.target.value)} /></div>
        <div><label className="label">Bedrooms</label><input className="input" type="number" value={f.bedrooms} onChange={(e) => set('bedrooms', e.target.value)} /></div>
        <div><label className="label">Bathrooms</label><input className="input" type="number" value={f.bathrooms} onChange={(e) => set('bathrooms', e.target.value)} /></div>
        <div className="col-span-2 mt-2 flex justify-end gap-2">
          <button type="button" className="btn-ghost" onClick={onClose}>Cancel</button>
          <button className="btn-primary" disabled={mut.isPending}>Save</button>
        </div>
      </form>
    </Modal>
  );
}
