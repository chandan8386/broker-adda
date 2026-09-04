import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { apiErrorMessage } from '../lib/api';

const DEMO = [
  ['Admin', 'admin@trishakti.com'],
  ['Sales Manager', 'manager@trishakti.com'],
  ['Calling Team', 'caller1@trishakti.com'],
  ['Sales Executive', 'sales1@trishakti.com'],
];

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [usernameOrEmail, setId] = useState('admin@trishakti.com');
  const [password, setPassword] = useState('Password@123');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      await login(usernameOrEmail, password);
      navigate('/');
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="grid min-h-full place-items-center bg-gradient-to-br from-brand-800 to-brand-600 p-4">
      <div className="w-full max-w-md">
        <div className="mb-6 text-center text-white">
          <h1 className="text-2xl font-bold">Shri Trishakti Infra Realtors Pvt Ltd</h1>
          <p className="text-sm text-brand-100">Property Lead Management / CRM</p>
        </div>
        <form onSubmit={submit} className="card space-y-4 p-6">
          <div>
            <label className="label">Username or Email</label>
            <input className="input" value={usernameOrEmail} onChange={(e) => setId(e.target.value)} autoFocus />
          </div>
          <div>
            <label className="label">Password</label>
            <input
              className="input"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>
          {error && <p className="rounded-lg bg-rose-50 px-3 py-2 text-sm text-rose-700">{error}</p>}
          <button className="btn-primary w-full" disabled={busy}>
            {busy ? 'Signing in…' : 'Sign in'}
          </button>
          <div className="border-t border-slate-100 pt-3 text-xs text-slate-400">
            <p className="mb-1 font-semibold">Demo accounts (password: Password@123)</p>
            <div className="grid grid-cols-2 gap-1">
              {DEMO.map(([role, email]) => (
                <button
                  key={email}
                  type="button"
                  className="rounded bg-slate-100 px-2 py-1 text-left hover:bg-slate-200"
                  onClick={() => {
                    setId(email);
                    setPassword('Password@123');
                  }}
                >
                  {role}
                </button>
              ))}
            </div>
          </div>
        </form>
      </div>
    </div>
  );
}
