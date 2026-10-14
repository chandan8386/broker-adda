import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { apiErrorMessage } from '../lib/api';

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
    <div className="relative grid min-h-screen place-items-center overflow-hidden p-4">
      <div
        aria-hidden="true"
        className="absolute inset-0 bg-cover bg-center"
        style={{ backgroundImage: "linear-gradient(110deg, rgba(9, 31, 48, 0.78), rgba(9, 31, 48, 0.34)), url('/img/login-building-front.jpg')" }}
      />
      <div className="relative z-10 w-full max-w-md">
        <div className="mx-auto mb-6 w-fit max-w-full rounded-2xl bg-white/95 p-3 text-center shadow-xl ring-1 ring-white/70 backdrop-blur-sm">
          <img className="mx-auto block w-full max-w-[280px]" src="/img/trishakti-logo.jpeg" alt="Shri Trishakti Infra Realtors Pvt Ltd" />
          <p className="mt-1 text-xs font-semibold tracking-wide text-brand-700">PROPERTY LEAD MANAGEMENT / CRM</p>
        </div>
        <form onSubmit={submit} className="card space-y-4 border-gold-200 p-6 shadow-2xl">
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
        </form>
      </div>
      <a
        className="absolute bottom-3 right-4 z-10 text-xs text-white/80 underline decoration-white/50 underline-offset-2"
        href="https://unsplash.com/photos/an-apartment-building-with-a-walkway-leading-to-the-front-door-MNDn5jPOUNg"
        target="_blank"
        rel="noreferrer"
      >
        Photo by Chelaxy Designs on Unsplash
      </a>
    </div>
  );
}
