import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  Wrench,
  CreditCard,
  FileSpreadsheet,
  AlertCircle,
  Headphones,
  Users,
  Layers,
  Settings,
  Building2,
  X,
  MapPin,
  ShieldCheck,
  User,
  KeyRound,
  Sparkles,
  Bell,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';

const navigationLinks = [
  { name: 'Dashboard', path: '/dashboard', icon: LayoutDashboard },
  { name: 'Maintenance', path: '/maintenance', icon: Wrench, badge: '28 Towers' },
  { name: 'Payments', path: '/payments', icon: CreditCard },
  { name: 'Bank Reconciliation', path: '/bank-reconciliation', icon: FileSpreadsheet, badge: 'Staff' },
  { name: 'Complaints', path: '/complaints', icon: AlertCircle, badge: 'AI' },
  { name: 'Visitors & Gate', path: '/visitors', icon: ShieldCheck, badge: 'Gate' },
  { name: 'Amenities', path: '/amenities', icon: Sparkles, badge: 'Book' },
  { name: 'Notices', path: '/notices', icon: Bell, badge: 'Live' },
  { name: 'Help Desk', path: '/helpdesk', icon: Headphones },
  { name: 'Committee', path: '/committee', icon: Users },
  { name: 'Architecture', path: '/architecture', icon: Layers, highlight: true },
  { name: 'Settings', path: '/settings', icon: Settings },
];

export default function Sidebar({ isOpen, onClose }) {
  const { user, isAuthenticated, isHelpDesk } = useAuth();

  const primaryFlat = user?.flats && user.flats.length > 0 ? user.flats[0] : null;

  return (
    <>
      {/* Mobile Backdrop */}
      {isOpen && (
        <div
          className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm z-40 lg:hidden"
          onClick={onClose}
          aria-hidden="true"
        />
      )}

      {/* Sidebar Container */}
      <aside
        className={`fixed top-0 bottom-0 left-0 z-50 w-72 bg-[#0c1322] text-slate-300 flex flex-col border-r border-slate-800 transition-transform duration-300 ease-in-out lg:translate-x-0 ${
          isOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        {/* Brand / Logo Header */}
        <div className="h-16 px-6 flex items-center justify-between border-b border-slate-800/80 bg-[#090f1c]">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-teal-500 to-emerald-400 flex items-center justify-center text-slate-950 font-bold shadow-md shadow-teal-500/20">
              <Building2 className="w-5 h-5 text-slate-950" />
            </div>
            <div>
              <div className="font-bold text-base text-white tracking-tight flex items-center gap-1.5">
                Pune Grand Vista
              </div>
              <p className="text-[11px] text-teal-400/90 font-medium tracking-wide flex items-center gap-1">
                <MapPin className="w-2.5 h-2.5" />
                28 Towers • 2,400 Units
              </p>
            </div>
          </div>

          {/* Mobile close button */}
          <button
            onClick={onClose}
            className="lg:hidden p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition cursor-pointer"
            aria-label="Close sidebar"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Operational Context Tag / Role Context */}
        <div className="px-5 py-3 mx-4 my-3 rounded-lg bg-slate-900/80 border border-slate-800/80 text-xs">
          <div className="flex items-center justify-between text-slate-400">
            <span className="text-[11px] font-semibold uppercase tracking-wider text-teal-400">
              {user?.role === 'HELP_DESK'
                ? 'Help Desk Operations'
                : user?.role === 'COMMITTEE'
                ? 'Managing Committee'
                : user?.role === 'GUARD'
                ? 'Gate Security Ops'
                : isAuthenticated
                ? 'Resident Portal'
                : 'Facility Ops'}
            </span>
            <span className="inline-flex items-center gap-1 text-[11px] text-emerald-400">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse"></span>
              Live Mode
            </span>
          </div>
          <div className="text-slate-200 font-medium text-xs mt-1 truncate">
            {isAuthenticated
              ? user?.fullName
              : 'Apex Facility Management'}
          </div>
          <div className="text-[11px] text-slate-400">
            {isAuthenticated
              ? user?.role === 'HELP_DESK'
                ? 'Apex Facility Lead'
                : user?.role === 'COMMITTEE'
                ? 'Elected MC President'
                : user?.role === 'GUARD'
                ? 'Gate 1 Security Officer'
                : primaryFlat
                ? `${primaryFlat.towerName} • Unit #${primaryFlat.flatNumber}`
                : 'Resident Member'
              : 'Baner-Balewadi, Pune'}
          </div>
        </div>

        {/* Nav Links */}
        <nav className="flex-1 px-3 py-2 space-y-1 overflow-y-auto">
          {navigationLinks.map((item) => {
            const Icon = item.icon;
            return (
              <NavLink
                key={item.path}
                to={item.path}
                onClick={() => {
                  if (window.innerWidth < 1024) onClose();
                }}
                className={({ isActive }) =>
                  `flex items-center justify-between px-3.5 py-2.5 rounded-xl text-xs sm:text-sm font-medium transition-all ${
                    isActive
                      ? 'bg-teal-500/10 text-teal-300 font-semibold border-r-2 border-teal-400'
                      : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
                  }`
                }
              >
                <div className="flex items-center gap-3">
                  <Icon className="w-4 h-4 shrink-0" />
                  <span>{item.name}</span>
                </div>
                {item.badge && (
                  <span
                    className={`text-[10px] px-2 py-0.5 rounded font-mono font-medium ${
                      item.badge === 'AI'
                        ? 'bg-violet-500/20 text-violet-300 border border-violet-500/30'
                        : item.badge === 'Staff'
                        ? 'bg-purple-500/20 text-purple-300 border border-purple-500/30'
                        : 'bg-slate-800 text-slate-400'
                    }`}
                  >
                    {item.badge}
                  </span>
                )}
              </NavLink>
            );
          })}
        </nav>

        {/* Footer info in sidebar */}
        <div className="p-4 border-t border-slate-800/80 bg-[#090f1c] text-xs text-slate-400">
          <div className="flex items-center gap-2 mb-1.5 text-slate-300 font-medium">
            <ShieldCheck className="w-4 h-4 text-teal-400" />
            <span>Township Core v1.0</span>
          </div>
          <p className="text-[11px] leading-relaxed text-slate-400">
            Pune Municipal Corp (PMC) Zone 4. Spring Security JWT Auth active.
          </p>
        </div>
      </aside>
    </>
  );
}
