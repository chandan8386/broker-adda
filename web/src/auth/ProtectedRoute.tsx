import { Navigate, useLocation } from 'react-router-dom';
import type { ReactNode } from 'react';
import { useAuth } from './AuthContext';
import type { Role } from '../lib/types';

export function ProtectedRoute({ children, roles }: { children: ReactNode; roles?: Role[] }) {
  const { user, loading, hasRole } = useAuth();
  const location = useLocation();

  if (loading) {
    return <div className="grid h-full place-items-center text-slate-400">Loading…</div>;
  }
  if (!user) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }
  if (roles && roles.length > 0 && !hasRole(...roles)) {
    return (
      <div className="grid h-full place-items-center text-center text-slate-500">
        <div>
          <p className="text-lg font-semibold">Access denied</p>
          <p className="text-sm">Your role doesn’t have permission to view this page.</p>
        </div>
      </div>
    );
  }
  return <>{children}</>;
}
