import { useState } from 'react';
import { Link } from 'react-router-dom';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, apiErrorMessage } from '../lib/api';
import type { LeadListItem, Page } from '../lib/types';
import { useCompanyMeta } from '../lib/meta';
import { Badge, EmptyState, Pagination, Spinner } from '../components/ui';
import { Modal } from '../components/Modal';
import { dateTime, fromNow } from '../lib/format';
import Papa from 'papaparse';

type CsvRow = Record<string, string>;
type ImportStatus = { state: 'Uploading' | 'Processing' | 'Completed' | 'Failed'; received: number; imported: number; skipped: number; errors: string[] };
const CSV_HEADERS = ['customerName', 'mobile', 'email', 'source', 'sourceChannel', 'propertyType', 'budgetMin', 'budgetMax', 'preferredLocation', 'priority', 'notes'];

export default function Leads() {
  const qc = useQueryClient();
  const meta = useCompanyMeta();
  const [page, setPage] = useState(0);
  const [q, setQ] = useState('');
  const [status, setStatus] = useState('');
  const [channel, setChannel] = useState('');
  const [showCreate, setShowCreate] = useState(false);
  const [importing, setImporting] = useState(false);
  const [importStatus, setImportStatus] = useState<ImportStatus | null>(null);

  const params = { page, size: 20, sort: 'createdAt,desc', q: q || undefined, status: status || undefined, sourceChannel: channel || undefined };

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['leads', params],
    queryFn: async () => (await api.get<Page<LeadListItem>>('/leads', { params })).data,
    placeholderData: keepPreviousData,
  });

  const exportCsv = async () => {
    // Export what the user is looking at: same filters as the table above.
    const res = await api.get('/leads/export', {
      params: { q: q || undefined, status: status || undefined, sourceChannel: channel || undefined },
      responseType: 'blob',
    });
    const url = URL.createObjectURL(res.data as Blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'leads-export.csv';
    a.click();
    URL.revokeObjectURL(url);
  };

  const importCsv = async (file: File) => {
    if (importing) return;
    setImporting(true);
    setImportStatus({ state: 'Uploading', received: 0, imported: 0, skipped: 0, errors: [] });
    let uploadId = '';
    let buffer: CsvRow[] = [];
    let nextRowNumber = 2;
    let totals = { received: 0, imported: 0, skipped: 0, errors: [] as string[] };
    const send = async (rows: CsvRow[], firstRowNumber: number) => {
      if (!rows.length) return;
      const response = (await api.post(`/leads/imports/${uploadId}/chunks`, { rows, firstRowNumber })).data;
      totals = { received: response.receivedRows, imported: response.imported, skipped: response.skipped, errors: response.errors };
      setImportStatus({ state: 'Processing', ...totals });
    };
    try {
      await new Promise<void>((resolve, reject) => {
        Papa.parse<CsvRow>(file, {
          header: true,
          skipEmptyLines: true,
          chunkSize: 1024 * 1024,
          chunk: (result, parser) => {
            parser.pause();
            void (async () => {
              try {
                if (!uploadId) {
                  const headers = result.meta.fields ?? [];
                  if (!CSV_HEADERS.every((header) => headers.includes(header))) throw new Error(`CSV must contain headers: ${CSV_HEADERS.join(', ')}`);
                  uploadId = (await api.post('/leads/imports', { fileName: file.name, headers })).data.uploadId;
                }
                buffer.push(...result.data);
                while (buffer.length >= 500) {
                  const rows = buffer.splice(0, 500);
                  await send(rows, nextRowNumber);
                  nextRowNumber += rows.length;
                }
                parser.resume();
              } catch (error) { parser.abort(); reject(error); }
            })();
          },
          complete: (result) => {
            void (async () => {
              try {
                if (!uploadId) throw new Error('CSV is empty');
                await send(buffer, nextRowNumber);
                const completed = (await api.post(`/leads/imports/${uploadId}/complete`)).data;
                totals = { received: completed.receivedRows, imported: completed.imported, skipped: completed.skipped, errors: completed.errors };
                setImportStatus({ state: 'Completed', ...totals });
                resolve();
              } catch (error) { reject(error); }
            })();
          },
          error: (error) => reject(error),
        });
      });
      qc.invalidateQueries({ queryKey: ['leads'] });
    } catch (error) {
      setImportStatus({ state: 'Failed', ...totals, errors: [apiErrorMessage(error)] });
    } finally { setImporting(false); }
  };

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h1 className="text-xl font-bold text-slate-800">Lead Management</h1>
        <div className="flex flex-wrap gap-2">
          <label className="btn-ghost cursor-pointer">
            Import CSV
            <input
              type="file"
              accept=".csv"
              hidden
              onChange={(e) => e.target.files?.[0] && void importCsv(e.target.files[0])}
            />
          </label>
          <button className="btn-ghost" onClick={exportCsv}>Export CSV</button>
          <button className="btn-primary" onClick={() => setShowCreate(true)}>+ New Lead</button>
        </div>
      </div>

      {importStatus && <div className="card flex items-center justify-between gap-3 p-3 text-sm">
        <span><strong>{importStatus.state}</strong> · {importStatus.received} rows received · {importStatus.imported} imported · {importStatus.skipped} skipped</span>
        {!importing && <button className="btn-ghost" onClick={() => setImportStatus(null)}>Dismiss</button>}
      </div>}

      <div className="card flex flex-wrap gap-2 p-3">
        <input
          className="input max-w-xs"
          placeholder="Search name / mobile / email…"
          value={q}
          onChange={(e) => {
            setPage(0);
            setQ(e.target.value);
          }}
        />
        <select className="input max-w-[180px]" value={status} onChange={(e) => { setPage(0); setStatus(e.target.value); }}>
          <option value="">All statuses</option>
          {meta.data?.enums.leadStatus.map((s) => <option key={s}>{s}</option>)}
        </select>
        <select className="input max-w-[180px]" value={channel} onChange={(e) => { setPage(0); setChannel(e.target.value); }}>
          <option value="">All sources</option>
          {meta.data?.enums.sourceChannel.map((s) => <option key={s}>{s}</option>)}
        </select>
      </div>

      <div className="card overflow-x-auto">
        {isLoading ? (
          <Spinner />
        ) : isError ? (
          <EmptyState message={apiErrorMessage(error)} />
        ) : !data || data.empty ? (
          <EmptyState message="No leads match your filters." />
        ) : (
          <table className="min-w-full">
            <thead>
              <tr>
                <th className="th">Ref</th>
                <th className="th">Customer</th>
                <th className="th">Mobile</th>
                <th className="th">Source</th>
                <th className="th">Type</th>
                <th className="th">Status</th>
                <th className="th">Priority</th>
                <th className="th">Assigned</th>
                <th className="th">Next follow-up</th>
              </tr>
            </thead>
            <tbody>
              {data.content.map((l) => (
                <tr key={l.id} className="hover:bg-slate-50">
                  <td className="td font-mono text-xs">
                    <Link className="text-brand-700 hover:underline" to={`/leads/${l.id}`}>{l.reference}</Link>
                  </td>
                  <td className="td font-medium">{l.customerName}</td>
                  <td className="td">{l.mobile}</td>
                  <td className="td text-xs">{l.sourceChannel ?? '—'}</td>
                  <td className="td text-xs">{l.propertyType ?? '—'}</td>
                  <td className="td"><Badge value={l.status} /></td>
                  <td className="td"><Badge value={l.priority} /></td>
                  <td className="td text-xs">{l.assignedUserName ?? '—'}</td>
                  <td className="td text-xs" title={dateTime(l.nextFollowUpAt)}>{l.nextFollowUpAt ? fromNow(l.nextFollowUpAt) : '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {data && <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />}
      </div>

      {showCreate && <CreateLeadModal onClose={() => setShowCreate(false)} />}
    </div>
  );
}

function CreateLeadModal({ onClose }: { onClose: () => void }) {
  const qc = useQueryClient();
  const meta = useCompanyMeta();
  const [form, setForm] = useState({
    customerName: '', mobile: '', email: '', source: '', sourceChannel: 'OTHER',
    propertyType: 'FLAT', budgetMin: '', budgetMax: '', preferredLocation: '', priority: 'WARM',
  });
  const set = (k: string, v: string) => setForm((f) => ({ ...f, [k]: v }));

  const mut = useMutation({
    mutationFn: async () =>
      (await api.post('/leads', {
        ...form,
        budgetMin: form.budgetMin ? Number(form.budgetMin) : undefined,
        budgetMax: form.budgetMax ? Number(form.budgetMax) : undefined,
      })).data,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['leads'] });
      onClose();
    },
    onError: (e) => alert(apiErrorMessage(e)),
  });

  return (
    <Modal title="New Lead" onClose={onClose}>
      <form
        className="grid grid-cols-2 gap-3"
        onSubmit={(e) => {
          e.preventDefault();
          mut.mutate();
        }}
      >
        <div className="col-span-2">
          <label className="label">Customer name *</label>
          <input className="input" required value={form.customerName} onChange={(e) => set('customerName', e.target.value)} />
        </div>
        <div>
          <label className="label">Mobile *</label>
          <input className="input" required value={form.mobile} onChange={(e) => set('mobile', e.target.value)} />
        </div>
        <div>
          <label className="label">Email</label>
          <input className="input" type="email" value={form.email} onChange={(e) => set('email', e.target.value)} />
        </div>
        <div>
          <label className="label">Source / Advertisement</label>
          <input className="input" value={form.source} onChange={(e) => set('source', e.target.value)} />
        </div>
        <div>
          <label className="label">Source channel</label>
          <select className="input" value={form.sourceChannel} onChange={(e) => set('sourceChannel', e.target.value)}>
            {meta.data?.enums.sourceChannel.map((s) => <option key={s}>{s}</option>)}
          </select>
        </div>
        <div>
          <label className="label">Property type</label>
          <select className="input" value={form.propertyType} onChange={(e) => set('propertyType', e.target.value)}>
            {meta.data?.enums.propertyType.map((s) => <option key={s}>{s}</option>)}
          </select>
        </div>
        <div>
          <label className="label">Priority</label>
          <select className="input" value={form.priority} onChange={(e) => set('priority', e.target.value)}>
            {meta.data?.enums.leadPriority.map((s) => <option key={s}>{s}</option>)}
          </select>
        </div>
        <div>
          <label className="label">Budget min</label>
          <input className="input" type="number" value={form.budgetMin} onChange={(e) => set('budgetMin', e.target.value)} />
        </div>
        <div>
          <label className="label">Budget max</label>
          <input className="input" type="number" value={form.budgetMax} onChange={(e) => set('budgetMax', e.target.value)} />
        </div>
        <div className="col-span-2">
          <label className="label">Preferred location</label>
          <input className="input" value={form.preferredLocation} onChange={(e) => set('preferredLocation', e.target.value)} />
        </div>
        <div className="col-span-2 mt-2 flex justify-end gap-2">
          <button type="button" className="btn-ghost" onClick={onClose}>Cancel</button>
          <button className="btn-primary" disabled={mut.isPending}>{mut.isPending ? 'Saving…' : 'Create lead'}</button>
        </div>
      </form>
    </Modal>
  );
}
