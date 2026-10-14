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
      <div className="relative z-10 w-full max-w-sm">
        <div className="card border-gold-200 p-4 shadow-2xl">
          <img className="mx-auto mb-2 block w-full max-w-[280px]" src="/img/trishakti-logo-transparent-crop.png" alt="Shri Trishakti Infra Realtors Pvt Ltd" />
          <form onSubmit={submit} className="space-y-4">
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
      </div>
     
    </div>
  );
}
