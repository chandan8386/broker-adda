import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { isAxiosError } from 'axios';
import { api, tokenStore } from '../lib/api';
import type { AuthResponse, Role, UserSummary } from '../lib/types';

interface AuthState {
  user: UserSummary | null;
  loading: boolean;
  login: (usernameOrEmail: string, password: string) => Promise<void>;
  logout: () => void;
  hasRole: (...roles: Role[]) => boolean;
}

const AuthCtx = createContext<AuthState | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserSummary | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!tokenStore.access) {
      setLoading(false);
      return;
    }
    api
      .get<UserSummary>('/auth/me')
      .then((res) => setUser(res.data))
      .catch(() => tokenStore.clear())
      .finally(() => setLoading(false));
  }, []);

  async function login(usernameOrEmail: string, password: string) {
    for (let attempt = 0; attempt < 2; attempt++) {
      try {
        const res = await api.post<AuthResponse>('/auth/login', { usernameOrEmail, password });
        tokenStore.set(res.data.accessToken, res.data.refreshToken);
        setUser(res.data.user);
        return;
      } catch (err) {
        const transient = isAxiosError(err) && (!err.response || err.response.status >= 500);
        if (!transient || attempt === 1) throw err;
        await new Promise((resolve) => setTimeout(resolve, 2000));
      }
    }
  }

  function logout() {
    api.post('/auth/logout').catch(() => undefined);
    tokenStore.clear();
    setUser(null);
    location.assign('/login');
  }

  const value = useMemo<AuthState>(
    () => ({
      user,
      loading,
      login,
      logout,
      hasRole: (...roles: Role[]) => !!user && user.roles.some((r) => roles.includes(r)),
    }),
    [user, loading],
  );

  return <AuthCtx.Provider value={value}>{children}</AuthCtx.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthCtx);
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>');
  return ctx;
}
