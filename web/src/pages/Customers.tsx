import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { api } from '../lib/api';
import type { Page } from '../lib/types';
import { EmptyState, Pagination, Spinner } from '../components/ui';
import { dateOnly } from '../lib/format';

interface Customer {
  id: number; fullName: string; mobile: string; email?: string; city?: string;
  idProofType?: string; idProofNumber?: string; createdAt: string;
}

export default function Customers() {
  const [page, setPage] = useState(0);
  const [q, setQ] = useState('');
  const { data, isLoading } = useQuery({
    queryKey: ['customers', page, q],
    queryFn: async () => (await api.get<Page<Customer>>('/customers', { params: { page, size: 20, q: q || undefined } })).data,
  });

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-bold text-slate-800">Customer Management</h1>
      <div className="card p-3">
        <input className="input max-w-xs" placeholder="Search name / mobile / email" value={q} onChange={(e) => { setPage(0); setQ(e.target.value); }} />
      </div>
      <div className="card overflow-x-auto">
        {isLoading ? (
          <Spinner />
        ) : !data || data.empty ? (
          <EmptyState message="No customers yet — they are created automatically on booking." />
        ) : (
          <table className="min-w-full">
            <thead>
              <tr>
                <th className="th">Name</th>
                <th className="th">Mobile</th>
                <th className="th">Email</th>
                <th className="th">City</th>
                <th className="th">ID proof</th>
                <th className="th">Since</th>
              </tr>
            </thead>
            <tbody>
              {data.content.map((c) => (
                <tr key={c.id} className="hover:bg-slate-50">
                  <td className="td font-medium">{c.fullName}</td>
                  <td className="td">{c.mobile}</td>
                  <td className="td text-xs">{c.email ?? '—'}</td>
                  <td className="td text-xs">{c.city ?? '—'}</td>
                  <td className="td text-xs">{c.idProofType ? `${c.idProofType} ${c.idProofNumber ?? ''}` : '—'}</td>
                  <td className="td text-xs">{dateOnly(c.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {data && <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />}
      </div>
    </div>
  );
}
