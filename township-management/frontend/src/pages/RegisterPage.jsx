import { useState, useEffect } from 'react';
import { useNavigate, Link, Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { townshipApi } from '../services/api';
import {
  Building2,
  Lock,
  Mail,
  User,
  Phone,
  Home,
  ArrowRight,
  AlertCircle,
  Loader2,
  CheckCircle2,
  ShieldCheck,
} from 'lucide-react';

export default function RegisterPage() {
  const navigate = useNavigate();
  const { register, isAuthenticated } = useAuth();

  const [towers, setTowers] = useState([]);
  const [loadingTowers, setLoadingTowers] = useState(true);

  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    phone: '',
    password: '',
    role: 'RESIDENT',
    towerId: '',
    flatNumber: '',
  });

  const [submitting, setSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  // Fetch 28 towers on mount
  useEffect(() => {
    let isMounted = true;
    const fetchTowers = async () => {
      try {
        const data = await townshipApi.getTowers();
        if (isMounted) {
          setTowers(data || []);
          if (data && data.length > 0) {
            setFormData((prev) => ({ ...prev, towerId: data[0].code || 'T01' }));
          }
        }
      } catch (err) {
        console.error('Failed to load towers:', err);
      } finally {
        if (isMounted) setLoadingTowers(false);
      }
    };
    fetchTowers();
    return () => {
      isMounted = false;
    };
  }, []);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    setErrorMsg('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setErrorMsg('');

    if (!formData.fullName.trim()) {
      setErrorMsg('Full name is required.');
      return;
    }
    if (!formData.email.trim()) {
      setErrorMsg('Email address is required.');
      return;
    }
    if (!formData.phone.trim()) {
      setErrorMsg('Indian mobile phone number is required.');
      return;
    }
    if (!formData.password || formData.password.length < 6) {
      setErrorMsg('Password must be at least 6 characters.');
      return;
    }

    // Format phone to standard Indian format if bare 10 digits
    let formattedPhone = formData.phone.trim();
    if (/^[6-9]\d{9}$/.test(formattedPhone)) {
      formattedPhone = '+91' + formattedPhone;
    }

    setSubmitting(true);
    try {
      await register({
        ...formData,
        phone: formattedPhone,
      });
      navigate('/dashboard', { replace: true });
    } catch (err) {
      setErrorMsg(err.message || 'Registration failed. Please check your details.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-[calc(100vh-8rem)] flex items-center justify-center py-8 px-4 sm:px-6 lg:px-8">
      <div className="max-w-lg w-full space-y-6">
        {/* Branding */}
        <div className="text-center">
          <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-[#0c1322] border border-slate-700 shadow-xl mb-3">
            <Building2 className="w-7 h-7 text-teal-400" />
          </div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">
            Pune Grand Vista
          </h1>
          <p className="text-xs text-slate-500 mt-1">
            Resident & Facility Onboarding • 28 Towers & 2,400 Units
          </p>
        </div>

        {/* Card */}
        <div className="bg-white rounded-2xl shadow-xl shadow-slate-200/50 border border-slate-200 p-6 sm:p-8">
          <div className="mb-6">
            <h2 className="text-lg font-bold text-slate-900">Resident Flat Registration</h2>
            <p className="text-xs text-slate-500 mt-0.5">
              Register as an apartment owner or tenant to access maintenance bills, ledger, and township services.
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

            {/* Full Name */}
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                Full Name
              </label>
              <div className="relative">
                <User className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  name="fullName"
                  value={formData.fullName}
                  onChange={handleChange}
                  placeholder="e.g. Vikramaditya Deshmukh"
                  className="w-full pl-9 pr-3 py-2 text-xs text-slate-800 placeholder-slate-400 rounded-xl border border-slate-300 focus:border-teal-500 focus:ring-2 focus:ring-teal-500/20 focus:outline-none transition"
                  required
                />
              </div>
            </div>

            {/* Email & Phone */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                  Email Address
                </label>
                <div className="relative">
                  <Mail className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                  <input
                    type="email"
                    name="email"
                    value={formData.email}
                    onChange={handleChange}
                    placeholder="vikram@example.com"
                    className="w-full pl-9 pr-3 py-2 text-xs text-slate-800 placeholder-slate-400 rounded-xl border border-slate-300 focus:border-teal-500 focus:ring-2 focus:ring-teal-500/20 focus:outline-none transition"
                    required
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                  Mobile Number
                </label>
                <div className="relative">
                  <Phone className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                  <input
                    type="text"
                    name="phone"
                    value={formData.phone}
                    onChange={handleChange}
                    placeholder="+919822123456"
                    className="w-full pl-9 pr-3 py-2 text-xs text-slate-800 placeholder-slate-400 rounded-xl border border-slate-300 focus:border-teal-500 focus:ring-2 focus:ring-teal-500/20 focus:outline-none transition"
                    required
                  />
                </div>
              </div>
            </div>

            {/* Password */}
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1.5">
                Password (min 6 characters)
              </label>
              <div className="relative">
                <Lock className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="password"
                  name="password"
                  value={formData.password}
                  onChange={handleChange}
                  placeholder="••••••••••••"
                  className="w-full pl-9 pr-3 py-2 text-xs text-slate-800 placeholder-slate-400 rounded-xl border border-slate-300 focus:border-teal-500 focus:ring-2 focus:ring-teal-500/20 focus:outline-none transition"
                  required
                />
              </div>
            </div>

            {/* Flat Details (Required/Recommended for residents) */}
            {formData.role === 'RESIDENT' && (
              <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200/90 space-y-3">
                <div className="flex items-center gap-1.5 text-xs font-semibold text-slate-800">
                  <Home className="w-4 h-4 text-teal-600" />
                  <span>Township Flat Assignment</span>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div>
                    <label className="block text-[11px] font-semibold text-slate-600 mb-1">
                      Tower (28 Available)
                    </label>
                    <select
                      name="towerId"
                      value={formData.towerId}
                      onChange={handleChange}
                      disabled={loadingTowers}
                      className="w-full py-1.5 px-2.5 text-xs text-slate-800 bg-white rounded-lg border border-slate-300 focus:border-teal-500 focus:outline-none transition"
                    >
                      {towers.map((tower) => (
                        <option key={tower.code} value={tower.code}>
                          {tower.code} — {tower.name}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className="block text-[11px] font-semibold text-slate-600 mb-1">
                      Flat Number
                    </label>
                    <input
                      type="text"
                      name="flatNumber"
                      value={formData.flatNumber}
                      onChange={handleChange}
                      placeholder="e.g. 1403"
                      className="w-full py-1.5 px-2.5 text-xs text-slate-800 bg-white rounded-lg border border-slate-300 focus:border-teal-500 focus:outline-none transition"
                    />
                  </div>
                </div>
              </div>
            )}

            <button
              type="submit"
              disabled={submitting}
              className="w-full mt-3 py-2.5 px-4 rounded-xl bg-teal-600 hover:bg-teal-700 text-white font-medium text-xs shadow-md shadow-teal-600/20 flex items-center justify-center gap-2 transition disabled:opacity-70 cursor-pointer"
            >
              {submitting ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin text-white" />
                  <span>Registering & Minting JWT...</span>
                </>
              ) : (
                <>
                  <span>Create Account & Enter Portal</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </form>

          {/* Link to Login */}
          <div className="mt-5 text-center text-xs text-slate-500">
            Already have an account?{' '}
            <Link
              to="/login"
              className="font-semibold text-teal-600 hover:text-teal-700 inline-flex items-center gap-0.5"
            >
              Sign In <ArrowRight className="w-3 h-3" />
            </Link>
          </div>
        </div>

        {/* Security badge */}
        <div className="flex items-center justify-center gap-2 text-center text-[11px] text-slate-400">
          <ShieldCheck className="w-3.5 h-3.5 text-teal-500" />
          <span>PostgreSQL relational integrity with unique phone and tower-flat constraints</span>
        </div>
      </div>
    </div>
  );
}
