import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../lib/api';
import type { NotificationItem, Page } from '../lib/types';
import { EmptyState, Spinner } from '../components/ui';
import { fromNow } from '../lib/format';

export default function Notifications() {
  const qc = useQueryClient();
  const { data, isLoading } = useQuery({
    queryKey: ['notifications'],
    queryFn: async () => (await api.get<Page<NotificationItem>>('/notifications', { params: { size: 50 } })).data,
  });

  const markAll = useMutation({
    mutationFn: async () => api.post('/notifications/read-all'),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['notifications'] });
      qc.invalidateQueries({ queryKey: ['notif-unread'] });
    },
  });
  const markOne = useMutation({
    mutationFn: async (id: number) => api.post(`/notifications/${id}/read`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['notifications'] });
      qc.invalidateQueries({ queryKey: ['notif-unread'] });
    },
  });

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-bold text-slate-800">Notifications</h1>
        <button className="btn-ghost" onClick={() => markAll.mutate()}>Mark all read</button>
      </div>
      <div className="card divide-y divide-slate-100">
        {isLoading ? (
          <Spinner />
        ) : !data || data.empty ? (
          <EmptyState message="You're all caught up." />
        ) : (
          data.content.map((n) => (
            <button
              key={n.id}
              className={`flex w-full items-start gap-3 p-4 text-left hover:bg-slate-50 ${n.read ? 'opacity-60' : ''}`}
              onClick={() => !n.read && markOne.mutate(n.id)}
            >
              <span className={`mt-1 h-2 w-2 shrink-0 rounded-full ${n.read ? 'bg-slate-300' : 'bg-brand-500'}`} />
              <div className="min-w-0">
                <p className="text-sm font-medium text-slate-800">{n.title}</p>
                {n.body && <p className="text-sm text-slate-500">{n.body}</p>}
                <p className="text-xs text-slate-400">{fromNow(n.createdAt)}</p>
              </div>
            </button>
          ))
        )}
      </div>
    </div>
  );
}
