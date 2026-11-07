import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  Menu,
  Search,
  Bell,
  CheckCircle2,
  AlertTriangle,
  XCircle,
  RefreshCw,
  ChevronDown,
  User,
  Shield,
  HelpCircle,
  LogOut,
  LogIn,
  KeyRound,
} from 'lucide-react';
import { checkBackendHealth } from '../services/api';
import { useAuth } from '../context/AuthContext';

export default function Header({ onToggleSidebar }) {
  const { user, isAuthenticated, isHelpDesk, logout } = useAuth();
  const navigate = useNavigate();

  const [backendState, setBackendState] = useState({
    loading: true,
    status: 'probing',
    data: null,
  });
  const [showNotifications, setShowNotifications] = useState(false);
  const [showProfileMenu, setShowProfileMenu] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');

  const probeBackend = async () => {
    setBackendState((prev) => ({ ...prev, loading: true }));
    const result = await checkBackendHealth();
    setBackendState({
      loading: false,
      status: result.status,
      data: result.data,
      latencyMs: result.latencyMs,
    });
  };

  useEffect(() => {
    probeBackend();
    const interval = setInterval(probeBackend, 30000);
    return () => clearInterval(interval);
  }, []);

  const handleLogout = () => {
    logout();
    setShowProfileMenu(false);
    navigate('/login');
  };

  const getInitials = (name) => {
    if (!name) return 'PG';
    const parts = name.trim().split(/\s+/);
    if (parts.length >= 2) {
      return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    }
    return name.substring(0, 2).toUpperCase();
  };

  const primaryFlat = user?.flats && user.flats.length > 0 ? user.flats[0] : null;

  return (
    <header className="sticky top-0 z-30 h-16 bg-white/95 backdrop-blur border-b border-slate-200/90 px-4 sm:px-6 lg:px-8 flex items-center justify-between">
      {/* Left: Mobile Toggle & Township Identity */}
      <div className="flex items-center gap-3">
        <button
          onClick={onToggleSidebar}
          className="lg:hidden p-2 rounded-lg text-slate-600 hover:text-slate-900 hover:bg-slate-100 transition"
          aria-label="Toggle Navigation"
        >
          <Menu className="w-5 h-5" />
        </button>

        <div className="hidden sm:flex items-center gap-2">
          <div className="w-2 h-2 rounded-full bg-teal-500"></div>
          <span className="text-sm font-bold text-slate-900 tracking-tight">
            Pune Grand Vista
          </span>
          <span className="text-xs px-2 py-0.5 rounded-full bg-slate-100 text-slate-600 border border-slate-200 font-medium">
            Pune Facility Hub
          </span>
        </div>
      </div>

      {/* Middle: Global Search Bar */}
      <div className="flex-1 max-w-md mx-4 hidden md:block">
        <div className="relative">
          <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search flats (e.g. T07-1203), towers, UTR, resident name..."
            className="w-full pl-9 pr-10 py-1.5 bg-slate-100/80 hover:bg-slate-100 focus:bg-white text-xs text-slate-800 placeholder-slate-400 rounded-lg border border-transparent focus:border-teal-500 focus:ring-2 focus:ring-teal-500/20 focus:outline-none transition"
          />
          <kbd className="absolute right-2.5 top-1/2 -translate-y-1/2 text-[10px] text-slate-400 bg-white px-1.5 py-0.5 rounded border border-slate-200">
            /
          </kbd>
        </div>
      </div>

      {/* Right: Live Status Pill, Notifications, User Profile */}
      <div className="flex items-center gap-3">
        {/* Real-time Backend / Database Connection Pill */}
        <div className="hidden sm:flex items-center">
          {backendState.status === 'connected' ? (
            <span
              className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-emerald-50 text-emerald-700 border border-emerald-200"
              title="Spring Boot & PostgreSQL Connected"
            >
              <span className="w-2 h-2 rounded-full bg-emerald-500"></span>
              PostgreSQL 16 Connected
            </span>
          ) : backendState.status === 'degraded' ? (
            <span
              className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-amber-50 text-amber-700 border border-amber-200"
              title="Backend UP, DB offline"
            >
              <AlertTriangle className="w-3.5 h-3.5 text-amber-600" />
              API UP • DB Offline
            </span>
          ) : (
            <span
              className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-rose-50 text-rose-700 border border-rose-200"
              title="Backend Unreachable"
            >
              <span className="w-2 h-2 rounded-full bg-rose-500"></span>
              Backend Offline
            </span>
          )}
        </div>

        {/* Refresh probe button */}
        <button
          onClick={probeBackend}
          disabled={backendState.loading}
          className="p-1.5 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition cursor-pointer"
          title="Refresh connection status"
        >
          <RefreshCw
            className={`w-4 h-4 ${backendState.loading ? 'animate-spin text-teal-600' : ''}`}
          />
        </button>

        {/* Notifications Icon with Dropdown */}
        <div className="relative">
          <button
            onClick={() => {
              setShowNotifications(!showNotifications);
              setShowProfileMenu(false);
            }}
            className="p-2 rounded-lg text-slate-600 hover:text-slate-900 hover:bg-slate-100 relative transition cursor-pointer"
            aria-label="View notifications"
          >
            <Bell className="w-4 h-4" />
            <span className="absolute top-1.5 right-1.5 w-2 h-2 rounded-full bg-teal-500 ring-2 ring-white"></span>
          </button>

          {showNotifications && (
            <div className="absolute right-0 mt-2 w-80 bg-white rounded-xl shadow-xl border border-slate-200 p-3 z-50 animate-in fade-in slide-in-from-top-2">
              <div className="flex items-center justify-between pb-2 border-b border-slate-100">
                <span className="font-semibold text-xs text-slate-900">Notifications</span>
                <span className="text-[10px] text-teal-600 font-medium">3 New Alerts</span>
              </div>
              <div className="divide-y divide-slate-100 text-xs">
                <div className="py-2.5">
                  <div className="font-medium text-slate-800">Lift Alert: Tower D #2</div>
                  <div className="text-[11px] text-slate-500 mt-0.5">Schindler engineer on-site (CMP-1082)</div>
                  <div className="text-[10px] text-slate-400 mt-1">25 mins ago</div>
                </div>
                <div className="py-2.5">
                  <div className="font-medium text-slate-800">Bank Statement Reconciliation</div>
                  <div className="text-[11px] text-slate-500 mt-0.5">18 NEFT transactions need unit tagging</div>
                  <div className="text-[10px] text-slate-400 mt-1">2 hours ago</div>
                </div>
                <div className="py-2.5">
                  <div className="font-medium text-slate-800">Quarterly Invoice Run (Q1)</div>
                  <div className="text-[11px] text-slate-500 mt-0.5">₹86.40 Lakhs billed across 28 towers</div>
                  <div className="text-[10px] text-slate-400 mt-1">Today, 09:00 AM</div>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* User Profile Pill or Sign In Button */}
        {isAuthenticated && user ? (
          <div className="relative">
            <button
              onClick={() => {
                setShowProfileMenu(!showProfileMenu);
                setShowNotifications(false);
              }}
              className="flex items-center gap-2 pl-2 pr-3 py-1 rounded-full hover:bg-slate-100 border border-slate-200 transition cursor-pointer"
            >
              <div className="w-7 h-7 rounded-full bg-slate-900 text-teal-300 flex items-center justify-center font-bold text-xs shadow-sm">
                {getInitials(user.fullName)}
              </div>
              <div className="hidden lg:block text-left">
                <div className="text-xs font-semibold text-slate-900 leading-tight">
                  {user.fullName}
                </div>
                <div className="text-[10px] text-slate-500 leading-none">
                  {user.role === 'HELP_DESK'
                    ? 'Help Desk Operations'
                    : user.role === 'COMMITTEE'
                    ? 'Managing Committee'
                    : user.role === 'GUARD'
                    ? 'Security Gate Guard'
                    : primaryFlat
                    ? `${primaryFlat.towerId}-${primaryFlat.flatNumber}`
                    : 'Resident'}
                </div>
              </div>
              <ChevronDown className="w-3.5 h-3.5 text-slate-400" />
            </button>

            {showProfileMenu && (
              <div className="absolute right-0 mt-2 w-64 bg-white rounded-xl shadow-xl border border-slate-200 py-1.5 z-50 text-xs animate-in fade-in">
                <div className="px-3.5 py-2.5 border-b border-slate-100">
                  <div className="font-semibold text-slate-900">{user.fullName}</div>
                  <div className="text-[11px] text-slate-500 truncate">{user.email}</div>
                  <div className="text-[11px] text-slate-500 font-mono mt-0.5">{user.phone}</div>
                  <div className="mt-2 flex items-center gap-1.5">
                    <span
                      className={`text-[10px] font-semibold px-2 py-0.5 rounded-full font-mono ${
                        user.role === 'HELP_DESK'
                          ? 'bg-purple-100 text-purple-700'
                          : user.role === 'COMMITTEE'
                          ? 'bg-teal-100 text-teal-700'
                          : user.role === 'GUARD'
                          ? 'bg-amber-100 text-amber-700'
                          : 'bg-emerald-100 text-emerald-700'
                      }`}
                    >
                      {user.role}
                    </span>
                    {primaryFlat && (
                      <span className="text-[10px] bg-slate-100 text-slate-600 px-2 py-0.5 rounded-full font-medium">
                        {primaryFlat.towerName} • #{primaryFlat.flatNumber}
                      </span>
                    )}
                  </div>
                </div>

                <div className="py-1">
                  <Link
                    to="/settings"
                    onClick={() => setShowProfileMenu(false)}
                    className="flex items-center gap-2 px-3.5 py-2 text-slate-700 hover:bg-slate-50 transition"
                  >
                    <User className="w-3.5 h-3.5 text-slate-400" />
                    Account Settings
                  </Link>
                  <Link
                    to="/architecture"
                    onClick={() => setShowProfileMenu(false)}
                    className="flex items-center gap-2 px-3.5 py-2 text-slate-700 hover:bg-slate-50 transition"
                  >
                    <Shield className="w-3.5 h-3.5 text-slate-400" />
                    System Architecture & DB
                  </Link>
                </div>

                <div className="pt-1 border-t border-slate-100">
                  <button
                    onClick={handleLogout}
                    className="w-full flex items-center gap-2 px-3.5 py-2 text-rose-600 hover:bg-rose-50 transition text-left cursor-pointer"
                  >
                    <LogOut className="w-3.5 h-3.5 text-rose-500" />
                    Sign Out
                  </button>
                </div>
              </div>
            )}
          </div>
        ) : (
          <div className="flex items-center gap-2">
            <Link
              to="/login"
              className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-900 hover:bg-slate-800 text-white font-medium text-xs shadow-sm transition"
            >
              <LogIn className="w-3.5 h-3.5" />
              <span>Sign In</span>
            </Link>
          </div>
        )}
      </div>
    </header>
  );
}
