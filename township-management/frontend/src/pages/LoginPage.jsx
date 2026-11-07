import { useState } from 'react';
import { useNavigate, useLocation, Link, Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import {
  Building2,
  Lock,
  Mail,
  ArrowRight,
  AlertCircle,
  Loader2,
  ShieldCheck,
  UserCheck,
  Sparkles,
} from 'lucide-react';

const DEMO_ACCOUNTS = [
  {
    label: 'Resident: Neeraj Kulkarni',
    flat: 'T07-1203 (Iris • 1,150 sq ft)',
    identifier: 'neeraj.kulkarni@grandvistapune.in',
    password: 'Resident@123',
    role: 'RESIDENT',
  },
  {
    label: 'Resident: Swati Joshi',
    flat: 'T12-0408 (Magnolia • 980 sq ft)',
    identifier: 'swati.joshi@grandvistapune.in',
    password: 'Resident@123',
    role: 'RESIDENT',
  },
  {
    label: 'Resident: Aamir Shaikh',
    flat: 'T21-1601 (Foxglove • 1,640 sq ft)',
    identifier: 'aamir.shaikh@grandvistapune.in',
    password: 'Resident@123',
    role: 'RESIDENT',
  },
  {
    label: 'Facility: Col. Rajiv Sharma',
    flat: 'Help Desk / Facility Lead',
    identifier: 'helpdesk@grandvistapune.in',
    password: 'HelpDesk@123',
    role: 'HELP_DESK',
  },
];

export default function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login, isAuthenticated } = useAuth();

  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  const from = location.state?.from?.pathname || '/dashboard';

  if (isAuthenticated) {
    return <Navigate to={from} replace />;
  }

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMsg('');

    if (!identifier.trim()) {
      setErrorMsg('Please enter your registered email or mobile number.');
      return;
    }
    if (!password) {
      setErrorMsg('Please enter your password.');
      return;
    }

    setSubmitting(true);
    try {
      await login(identifier.trim(), password);
      navigate(from, { replace: true });
    } catch (err) {
      setErrorMsg(err.message || 'Login failed. Please verify your credentials.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleSelectDemo = (account) => {
    setIdentifier(account.identifier);
    setPassword(account.password);
    setErrorMsg('');
  };

  return (
    <div className="min-h-[calc(100vh-8rem)] flex items-center justify-center py-6 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-6">
        {/* Branding & Header */}
        <div className="text-center">
          <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-[#0c1322] border border-slate-700 shadow-xl mb-3">
            <Building2 className="w-7 h-7 text-teal-400" />
          </div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">
            Pune Grand Vista
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            Gated Township Management System • 28 Towers & 2,400 Units
          </p>
        </div>

        {/* Main Card */}
        <div className="bg-white rounded-2xl shadow-xl shadow-slate-200/50 border border-slate-200 p-6 sm:p-8">
          <div className="mb-6">
            <h2 className="text-lg font-bold text-slate-900">Sign in to your account</h2>
            <p className="text-xs text-slate-500 mt-0.5">
              Access your maintenance ledger, receipts, complaints, and facility updates.
            </p>
          </div>

          {/* Error Banner */}
          {errorMsg && (
            <div className="mb-5 p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs flex items-start gap-2.5 animate-in fade-in">
              <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-rose-500" />
              <div className="flex-1 font-medium">{errorMsg}</div>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                Email Address or Phone Number
              </label>
              <div className="relative">
                <Mail className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  value={identifier}
                  onChange={(e) => setIdentifier(e.target.value)}
                  placeholder="name@grandvistapune.in or +919822012031"
                  autoComplete="username"
                  className="w-full pl-9 pr-3 py-2 text-xs text-slate-800 placeholder-slate-400 rounded-xl border border-slate-300 focus:border-teal-500 focus:ring-2 focus:ring-teal-500/20 focus:outline-none transition"
                  disabled={submitting}
                />
              </div>
            </div>

            <div>
              <div className="flex items-center justify-between mb-1.5">
                <label className="block text-xs font-semibold text-slate-700">
                  Password
                </label>
                <span className="text-[11px] text-teal-600 hover:text-teal-700 cursor-pointer">
                  Forgot?
                </span>
              </div>
              <div className="relative">
                <Lock className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••••••"
                  autoComplete="current-password"
                  className="w-full pl-9 pr-3 py-2 text-xs text-slate-800 placeholder-slate-400 rounded-xl border border-slate-300 focus:border-teal-500 focus:ring-2 focus:ring-teal-500/20 focus:outline-none transition"
                  disabled={submitting}
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={submitting}
              className="w-full mt-2 py-2.5 px-4 rounded-xl bg-slate-900 hover:bg-slate-800 text-white font-medium text-xs shadow-md shadow-slate-900/10 flex items-center justify-center gap-2 transition disabled:opacity-70 cursor-pointer"
            >
              {submitting ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin text-teal-400" />
                  <span>Authenticating with JWT...</span>
                </>
              ) : (
                <>
                  <span>Sign In</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>

          {/* Development-Only Demo Credentials (Never exposed in production) */}
          {import.meta.env.DEV && (
            <div className="mt-6 pt-5 border-t border-slate-100">
              <div className="flex items-center justify-between gap-1.5 mb-2.5">
                <div className="flex items-center gap-1.5 text-xs font-semibold text-slate-700">
                  <Sparkles className="w-3.5 h-3.5 text-teal-600" />
                  <span>Quick Test Accounts:</span>
                </div>
                <span className="text-[9px] font-mono uppercase px-1.5 py-0.5 rounded bg-amber-100 text-amber-800 font-semibold border border-amber-200">
                  Dev Mode Only
                </span>
              </div>
              <div className="space-y-1.5">
                {DEMO_ACCOUNTS.map((acc, idx) => (
                  <button
                    key={idx}
                    type="button"
                    onClick={() => handleSelectDemo(acc)}
                    className="w-full text-left p-2 rounded-lg bg-slate-50 hover:bg-teal-50/60 border border-slate-200/80 hover:border-teal-300 transition text-xs flex items-center justify-between group cursor-pointer"
                  >
                    <div>
                      <div className="font-semibold text-slate-800 group-hover:text-teal-900">
                        {acc.label}
                      </div>
                      <div className="text-[10px] text-slate-500 group-hover:text-teal-700">
                        {acc.flat}
                      </div>
                    </div>
                    <span className={`text-[10px] font-mono px-1.5 py-0.5 rounded uppercase font-semibold ${
                      acc.role === 'HELP_DESK'
                        ? 'bg-purple-100 text-purple-700'
                        : 'bg-emerald-100 text-emerald-700'
                    }`}>
                      {acc.role}
                    </span>
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* Link to Register */}
          <div className="mt-5 text-center text-xs text-slate-500">
            Are you a new flat owner or tenant?{' '}
            <Link
              to="/register"
              className="font-semibold text-teal-600 hover:text-teal-700 inline-flex items-center gap-0.5"
            >
              Register your flat <ArrowRight className="w-3 h-3" />
            </Link>
          </div>
        </div>

        {/* Security Notice */}
        <div className="flex items-center justify-center gap-2 text-center text-[11px] text-slate-400">
          <ShieldCheck className="w-3.5 h-3.5 text-teal-500" />
          <span>Secured by Spring Security 6 & JWT • BCrypt Hashing • PostgreSQL 16</span>
        </div>
      </div>
    </div>
  );
}
