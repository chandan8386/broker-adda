import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

const TOKEN_KEY = 'stir_crm_access';
const REFRESH_KEY = 'stir_crm_refresh';

export const tokenStore = {
  get access() {
    return localStorage.getItem(TOKEN_KEY);
  },
  get refresh() {
    return localStorage.getItem(REFRESH_KEY);
  },
  set(access: string, refresh: string) {
    localStorage.setItem(TOKEN_KEY, access);
    localStorage.setItem(REFRESH_KEY, refresh);
  },
  clear() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_KEY);
  },
};

export const api = axios.create({
  baseURL: BASE_URL,
  timeout: 120_000,
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = tokenStore.access;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

let refreshing: Promise<string> | null = null;

async function refreshAccessToken(): Promise<string> {
  const refresh = tokenStore.refresh;
  if (!refresh) throw new Error('No refresh token');
  const res = await axios.post(`${BASE_URL}/auth/refresh`, { refreshToken: refresh });
  tokenStore.set(res.data.accessToken, res.data.refreshToken);
  return res.data.accessToken;
}

api.interceptors.response.use(
  (r) => r,
  async (error: AxiosError) => {
    const original = error.config as (InternalAxiosRequestConfig & { _retry?: boolean }) | undefined;
    const status = error.response?.status;
    const url = original?.url ?? '';

    if (status === 401 && original && !original._retry && !url.includes('/auth/')) {
      original._retry = true;
      try {
        refreshing = refreshing ?? refreshAccessToken();
        const newToken = await refreshing;
        refreshing = null;
        original.headers.Authorization = `Bearer ${newToken}`;
        return api(original);
      } catch {
        refreshing = null;
        tokenStore.clear();
        if (!location.pathname.startsWith('/login')) location.assign('/login');
      }
    }
    return Promise.reject(error);
  },
);

export function apiErrorMessage(err: unknown): string {
  const e = err as AxiosError<{ message?: string; fieldErrors?: { field: string; message: string }[] }>;
  const data = e.response?.data;
  if (data?.fieldErrors?.length) {
    return data.fieldErrors.map((f) => `${f.field}: ${f.message}`).join(', ');
  }
  return data?.message || e.message || 'Something went wrong';
}
