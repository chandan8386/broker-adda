import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';

/**
 * VITE_API_BASE_URL is inlined at BUILD time, so it must be set in the hosting
 * provider's env before the build runs - setting it afterwards does nothing until
 * you redeploy.
 *
 * The localhost fallback only applies when the page is itself served from
 * localhost. Falling back to it in production made a deployed build quietly call
 * the visitor's own machine over http:// from an https:// page, which the browser
 * blocks as mixed content and reports as an opaque "CORS error" - see configError().
 */
const CONFIGURED = (import.meta.env.VITE_API_BASE_URL ?? '').trim().replace(/\/$/, '');

const ON_LOCALHOST =
  typeof window !== 'undefined' &&
  /^(localhost|127\.0\.0\.1|\[::1\])$/.test(window.location.hostname);

const BASE_URL = CONFIGURED || (ON_LOCALHOST ? 'http://localhost:8080/api' : '');

/** A setup problem we can describe precisely, or null if the config looks sane. */
export function configError(): string | null {
  if (!BASE_URL) {
    return 'API URL is not configured. Set VITE_API_BASE_URL (e.g. https://your-api.onrender.com/api) ' +
      'in your hosting provider\'s environment variables, then redeploy - Vite inlines it at build time.';
  }
  if (typeof window !== 'undefined' &&
      window.location.protocol === 'https:' && BASE_URL.startsWith('http://')) {
    return `This page is served over HTTPS but VITE_API_BASE_URL is "${BASE_URL}" (plain HTTP). ` +
      'Browsers block that as mixed content. Use an https:// API URL and redeploy.';
  }
  return null;
}

if (typeof window !== 'undefined') {
  const problem = configError();
  if (problem) {
    console.error('[Trishakti CRM] ' + problem);
  } else {
    // Printed on every boot so which backend a deployment is talking to can be
    // confirmed from the console, without digging through the bundle.
    console.info('[Trishakti CRM] API base URL: ' + BASE_URL);
  }
}

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
  // Fail loudly on a broken API URL rather than letting the call go out. With an empty
  // baseURL the request would be sent to the app's own origin, where an SPA host answers
  // every path with index.html and HTTP 200 - so a login would "succeed" with junk
  // instead of reporting the real problem.
  const problem = configError();
  if (problem) return Promise.reject(new Error(problem));

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
  if (data?.message) return data.message;

  // No response at all: the browser blocked it or nothing was listening. Axios reports
  // both as a bare "Network Error", which tells the user nothing - if the cause is a
  // config mistake we can name, say so instead.
  if (!e.response) {
    const problem = configError();
    if (problem) return problem;
    return `Could not reach the API at ${BASE_URL}. It may be starting up (free hosting sleeps ` +
      'after inactivity and can take ~50s to wake), unreachable, or blocking this origin via CORS.';
  }
  return e.message || 'Something went wrong';
}
