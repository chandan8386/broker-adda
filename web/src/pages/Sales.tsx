import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { api } from '../lib/api';
import type { Page } from '../lib/types';
import { Badge, EmptyState, Spinner } from '../components/ui';
import { dateOnly, money } from '../lib/format';

interface Booking {
  id: number; bookingNumber: string; leadId: number; leadName: string;
  propertyTitle: string; customerName?: string; salesExecutiveName?: string;
  status: string; quotedPrice?: number; negotiatedPrice?: number; tokenAmount?: number;
  bookingDate?: string; expectedClosureDate?: string;
}
interface Purchase {
  id: number; bookingNumber: string; propertyTitle: string; customerName?: string;
  finalPrice?: number; totalPaid?: number; balance?: number; status: string;
  agreementDate?: string; registrationDate?: string;
}

export default function Sales() {
  const bookings = useQuery({
    queryKey: ['bookings'],
    queryFn: async () => (await api.get<Page<Booking>>('/sales/bookings', { params: { size: 50 } })).data,
  });
  const purchases = useQuery({
    queryKey: ['purchases'],
    queryFn: async () => (await api.get<Page<Purchase>>('/sales/purchases', { params: { size: 50 } })).data,
  });

  return (
    <div className="space-y-6">
      <h1 className="text-xl font-bold text-slate-800">Sales / Purchase Management</h1>

      <section>
        <h2 className="mb-2 text-sm font-semibold uppercase tracking-wide text-slate-500">Bookings</h2>
        <div className="card overflow-x-auto">
          {bookings.isLoading ? (
            <Spinner />
          ) : !bookings.data || bookings.data.empty ? (
            <EmptyState message="No bookings yet. Create one from an interested lead." />
          ) : (
            <table className="min-w-full">
              <thead>
                <tr>
                  <th className="th">Booking #</th>
                  <th className="th">Lead</th>
                  <th className="th">Property</th>
                  <th className="th">Executive</th>
                  <th className="th">Status</th>
                  <th className="th">Quoted</th>
                  <th className="th">Negotiated</th>
                  <th className="th">Token</th>
                </tr>
              </thead>
              <tbody>
                {bookings.data.content.map((b) => (
                  <tr key={b.id} className="hover:bg-slate-50">
                    <td className="td font-mono text-xs">{b.bookingNumber}</td>
                    <td className="td"><Link className="text-brand-700 hover:underline" to={`/leads/${b.leadId}`}>{b.leadName}</Link></td>
                    <td className="td text-xs">{b.propertyTitle}</td>
                    <td className="td text-xs">{b.salesExecutiveName ?? '—'}</td>
                    <td className="td"><Badge value={b.status} /></td>
                    <td className="td text-xs">{money(b.quotedPrice)}</td>
                    <td className="td text-xs">{money(b.negotiatedPrice)}</td>
                    <td className="td text-xs">{money(b.tokenAmount)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </section>

      <section>
        <h2 className="mb-2 text-sm font-semibold uppercase tracking-wide text-slate-500">Purchases</h2>
        <div className="card overflow-x-auto">
          {purchases.isLoading ? (
            <Spinner />
          ) : !purchases.data || purchases.data.empty ? (
            <EmptyState message="No purchases yet." />
          ) : (
            <table className="min-w-full">
              <thead>
                <tr>
                  <th className="th">Booking #</th>
                  <th className="th">Property</th>
                  <th className="th">Customer</th>
                  <th className="th">Status</th>
                  <th className="th">Final price</th>
                  <th className="th">Paid</th>
                  <th className="th">Balance</th>
                  <th className="th">Agreement</th>
                </tr>
              </thead>
              <tbody>
                {purchases.data.content.map((p) => (
                  <tr key={p.id} className="hover:bg-slate-50">
                    <td className="td font-mono text-xs">{p.bookingNumber}</td>
                    <td className="td text-xs">{p.propertyTitle}</td>
                    <td className="td text-xs">{p.customerName ?? '—'}</td>
                    <td className="td"><Badge value={p.status} /></td>
                    <td className="td text-xs">{money(p.finalPrice)}</td>
                    <td className="td text-xs text-emerald-600">{money(p.totalPaid)}</td>
                    <td className="td text-xs text-rose-600">{money(p.balance)}</td>
                    <td className="td text-xs">{dateOnly(p.agreementDate)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </section>
    </div>
  );
}
