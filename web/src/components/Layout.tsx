import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import clsx from 'clsx';
import { useAuth } from '../auth/AuthContext';
import { api } from '../lib/api';
import type { Role } from '../lib/types';

interface NavItem {
  to: string;
  label: string;
  icon: string;
  roles?: Role[];
}

const NAV: NavItem[] = [
  { to: '/', label: 'Dashboard', icon: '📊' },
  { to: '/leads', label: 'Leads', icon: '🎯' },
  { to: '/assignments', label: 'Lead Assignment', icon: '🔀', roles: ['ADMIN', 'SALES_MANAGER'] },
  { to: '/follow-ups', label: 'Calling / Follow-up', icon: '📞', roles: ['ADMIN', 'SALES_MANAGER', 'CALLING_TEAM'] },
  { to: '/site-visits', label: 'Site Visits', icon: '🏠' },
  { to: '/sales', label: 'Sales / Bookings', icon: '💰', roles: ['ADMIN', 'SALES_MANAGER', 'SALES_EXECUTIVE'] },
  { to: '/properties', label: 'Properties', icon: '🏗️' },
  { to: '/customers', label: 'Customers', icon: '👥' },
  { to: '/tasks', label: 'Tasks & Reminders', icon: '✅' },
  { to: '/reports', label: 'Reports & Analytics', icon: '📈', roles: ['ADMIN', 'SALES_MANAGER'] },
  { to: '/users', label: 'Users & Teams', icon: '⚙️', roles: ['ADMIN'] },
  { to: '/audit', label: 'Audit Logs', icon: '🧾', roles: ['ADMIN'] },
];

export default function Layout() {
  const { user, logout, hasRole } = useAuth();
  const navigate = useNavigate();

  const { data: unread } = useQuery({
    queryKey: ['notif-unread'],
    queryFn: async () => (await api.get<{ unread: number }>('/notifications/unread-count')).data.unread,
    refetchInterval: 30_000,
  });

  const items = NAV.filter((n) => !n.roles || hasRole(...n.roles));

  return (
    <div className="flex h-full">
      <aside className="hidden w-64 shrink-0 flex-col border-r border-slate-200 bg-white md:flex">
        <div className="border-b border-slate-200 px-5 py-4">
          <p className="text-sm font-bold text-brand-800">Shri Trishakti Infra</p>
          <p className="text-xs text-slate-400">Realtors Pvt Ltd — CRM</p>
        </div>
        <nav className="flex-1 space-y-1 overflow-y-auto p-3">
          {items.map((n) => (
            <NavLink
              key={n.to}
              to={n.to}
              end={n.to === '/'}
              className={({ isActive }) =>
                clsx(
                  'flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium',
                  isActive ? 'bg-brand-50 text-brand-800' : 'text-slate-600 hover:bg-slate-100',
                )
              }
            >
              <span>{n.icon}</span>
              {n.label}
            </NavLink>
          ))}
        </nav>
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex items-center justify-between border-b border-slate-200 bg-white px-4 py-3">
          <div className="md:hidden">
            <p className="text-sm font-bold text-brand-800">STIR CRM</p>
          </div>
          <div className="flex items-center gap-3">
            <button
              className="relative rounded-lg p-2 hover:bg-slate-100"
              onClick={() => navigate('/notifications')}
              title="Notifications"
            >
              🔔
              {!!unread && (
                <span className="absolute -right-0.5 -top-0.5 rounded-full bg-rose-600 px-1.5 text-[10px] font-bold text-white">
                  {unread}
                </span>
              )}
            </button>
            <div className="text-right">
              <p className="text-sm font-semibold text-slate-700">{user?.fullName}</p>
              <p className="text-xs text-slate-400">{user?.roles.join(', ')}</p>
            </div>
            <button className="btn-ghost" onClick={logout}>Sign out</button>
          </div>
        </header>

        <main className="flex-1 overflow-y-auto p-4 md:p-6">
          <Outlet />
        </main>

        <nav className="grid grid-cols-5 border-t border-slate-200 bg-white md:hidden">
          {items.slice(0, 5).map((n) => (
            <NavLink
              key={n.to}
              to={n.to}
              end={n.to === '/'}
              className={({ isActive }) =>
                clsx('flex flex-col items-center py-2 text-[10px]', isActive ? 'text-brand-700' : 'text-slate-500')
              }
            >
              <span className="text-base">{n.icon}</span>
              {n.label.split(' ')[0]}
            </NavLink>
          ))}
        </nav>
      </div>
    </div>
  );
}
