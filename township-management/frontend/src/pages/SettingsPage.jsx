import { useState } from 'react';
import {
  Settings,
  Building2,
  Receipt,
  Bell,
  Shield,
  Server,
  Save,
  CheckCircle2,
  Database,
  MapPin,
  Lock,
  User,
} from 'lucide-react';
import DemoNoticeBanner from '../components/DemoNoticeBanner';
import { useAuth } from '../context/AuthContext';

export default function SettingsPage() {
  const { user, isHelpDesk } = useAuth();
  const primaryFlat = user?.flats && user.flats.length > 0 ? user.flats[0] : null;

  const [townshipName, setTownshipName] = useState('Pune Grand Vista');
  const [address, setAddress] = useState('Survey No. 48/2, Baner - Balewadi High Street Link Road, Pune 411045');
  const [sqFtRate, setSqFtRate] = useState('3.75');
  const [sinkingFund, setSinkingFund] = useState('500');
  const [gstThreshold, setGstThreshold] = useState('7500');
  const [lateFeePercent, setLateFeePercent] = useState('1.5');
  const [savedNotice, setSavedNotice] = useState(false);

  const handleSave = (e) => {
    e.preventDefault();
    setSavedNotice(true);
    setTimeout(() => setSavedNotice(false), 3000);
  };

  return (
    <div className="space-y-6 max-w-5xl">
      <DemoNoticeBanner moduleName="Township Settings & Billing Rules" />

      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">
            Township & Operational Settings
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 mt-0.5">
            Configure township metadata, billing formulas, GST exemptions, and notification preferences.
          </p>
        </div>

        <button
          onClick={handleSave}
          className="inline-flex items-center gap-1.5 px-4 py-2 bg-teal-600 hover:bg-teal-700 text-white rounded-lg text-xs font-semibold shadow-sm transition self-start sm:self-auto"
        >
          <Save className="w-4 h-4" />
          Save Configurations
        </button>
      </div>

      {savedNotice && (
        <div className="p-3 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-xl text-xs flex items-center gap-2 animate-in fade-in">
          <CheckCircle2 className="w-4 h-4 text-emerald-600" />
          <span>Township configuration updated in local cache (Simulation mode).</span>
        </div>
      )}

      {/* Settings Sections */}
      <div className="space-y-6">
        {/* Section 0: User Profile & Security Credentials */}
        {user && (
          <div className="bg-white rounded-xl border border-slate-200/90 p-5 shadow-sm space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <div className="flex items-center gap-2.5">
                <User className="w-4 h-4 text-teal-600" />
                <h2 className="font-bold text-sm text-slate-900">Your Account & Security Details</h2>
              </div>
              <span className="text-[10px] font-mono px-2 py-0.5 rounded-full font-semibold bg-emerald-100 text-emerald-800">
                JWT Authenticated • {user.role}
              </span>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-xs">
              <div>
                <label className="block text-slate-500 font-medium mb-1">Full Name</label>
                <div className="p-2.5 bg-slate-50 border border-slate-200 rounded-lg font-semibold text-slate-800">
                  {user.fullName}
                </div>
              </div>

              <div>
                <label className="block text-slate-500 font-medium mb-1">Registered Email</label>
                <div className="p-2.5 bg-slate-50 border border-slate-200 rounded-lg text-slate-800">
                  {user.email}
                </div>
              </div>

              <div>
                <label className="block text-slate-500 font-medium mb-1">Registered Mobile Phone</label>
                <div className="p-2.5 bg-slate-50 border border-slate-200 rounded-lg font-mono text-slate-800">
                  {user.phone}
                </div>
              </div>
            </div>

            {primaryFlat && (
              <div className="p-3 bg-teal-50/60 border border-teal-100 rounded-xl text-xs flex items-center justify-between">
                <div>
                  <span className="font-semibold text-teal-900">Assigned Flat: </span>
                  <span className="text-teal-800">{primaryFlat.towerName} • Flat #{primaryFlat.flatNumber}</span>
                  <span className="text-teal-600 text-[11px] ml-2">({primaryFlat.areaSqFt} sq ft • Floor {primaryFlat.floorNumber})</span>
                </div>
                <span className="text-[10px] font-mono font-semibold px-2 py-0.5 rounded bg-teal-100 text-teal-800">
                  {primaryFlat.memberType}
                </span>
              </div>
            )}
          </div>
        )}

        {/* Section 1: Township Profile */}
        <div className="bg-white rounded-xl border border-slate-200/90 p-5 shadow-sm space-y-4">
          <div className="flex items-center gap-2.5 pb-3 border-b border-slate-100">
            <Building2 className="w-4 h-4 text-teal-600" />
            <h2 className="font-bold text-sm text-slate-900">Township Identity & Address</h2>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">
            <div>
              <label className="block text-slate-700 font-semibold mb-1">Township Community Name</label>
              <input
                type="text"
                value={townshipName}
                onChange={(e) => setTownshipName(e.target.value)}
                className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-xs focus:outline-none focus:border-teal-500 focus:bg-white transition"
              />
            </div>

            <div>
              <label className="block text-slate-700 font-semibold mb-1">Municipal Authority Zone</label>
              <input
                type="text"
                disabled
                value="Pune Municipal Corporation (PMC) - Zone 4 Baner"
                className="w-full px-3 py-2 bg-slate-100 border border-slate-200 rounded-lg text-xs text-slate-500 cursor-not-allowed"
              />
            </div>

            <div className="md:col-span-2">
              <label className="block text-slate-700 font-semibold mb-1">Physical Address & Survey Details</label>
              <input
                type="text"
                value={address}
                onChange={(e) => setAddress(e.target.value)}
                className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-xs focus:outline-none focus:border-teal-500 focus:bg-white transition"
              />
            </div>
          </div>
        </div>

        {/* Section 2: Maintenance Billing Formulas */}
        <div className="bg-white rounded-xl border border-slate-200/90 p-5 shadow-sm space-y-4">
          <div className="flex items-center gap-2.5 pb-3 border-b border-slate-100">
            <Receipt className="w-4 h-4 text-teal-600" />
            <h2 className="font-bold text-sm text-slate-900">Maintenance Billing Rules (Indian Society Guidelines)</h2>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 text-xs">
            <div>
              <label className="block text-slate-700 font-semibold mb-1">Base Rate (₹ / sq.ft)</label>
              <div className="relative">
                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 font-mono">₹</span>
                <input
                  type="number"
                  step="0.05"
                  value={sqFtRate}
                  onChange={(e) => setSqFtRate(e.target.value)}
                  className="w-full pl-7 pr-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-xs focus:outline-none focus:border-teal-500 focus:bg-white transition"
                />
              </div>
              <span className="text-[10px] text-slate-400 mt-1 block">Calculated on Super Built-up area</span>
            </div>

            <div>
              <label className="block text-slate-700 font-semibold mb-1">Sinking Fund (₹ / month)</label>
              <div className="relative">
                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 font-mono">₹</span>
                <input
                  type="number"
                  value={sinkingFund}
                  onChange={(e) => setSinkingFund(e.target.value)}
                  className="w-full pl-7 pr-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-xs focus:outline-none focus:border-teal-500 focus:bg-white transition"
                />
              </div>
              <span className="text-[10px] text-slate-400 mt-1 block">Fixed reserve allocation per unit</span>
            </div>

            <div>
              <label className="block text-slate-700 font-semibold mb-1">GST Exemption Limit (₹)</label>
              <div className="relative">
                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 font-mono">₹</span>
                <input
                  type="number"
                  value={gstThreshold}
                  onChange={(e) => setGstThreshold(e.target.value)}
                  className="w-full pl-7 pr-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-xs focus:outline-none focus:border-teal-500 focus:bg-white transition"
                />
              </div>
              <span className="text-[10px] text-slate-400 mt-1 block">18% GST applies strictly above ₹7,500</span>
            </div>

            <div>
              <label className="block text-slate-700 font-semibold mb-1">Late Fee Interest (%)</label>
              <div className="relative">
                <input
                  type="number"
                  step="0.1"
                  value={lateFeePercent}
                  onChange={(e) => setLateFeePercent(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-xs focus:outline-none focus:border-teal-500 focus:bg-white transition"
                />
                <span className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 text-xs">% / mo</span>
              </div>
              <span className="text-[10px] text-slate-400 mt-1 block">Simple interest on overdue balance</span>
            </div>
          </div>
        </div>

        {/* Section 3: System & API Connectivity */}
        <div className="bg-white rounded-xl border border-slate-200/90 p-5 shadow-sm space-y-4">
          <div className="flex items-center gap-2.5 pb-3 border-b border-slate-100">
            <Server className="w-4 h-4 text-teal-600" />
            <h2 className="font-bold text-sm text-slate-900">Backend API & Database Preferences</h2>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">
            <div>
              <label className="block text-slate-700 font-semibold mb-1">Spring Boot Base URL</label>
              <input
                type="text"
                disabled
                value="http://localhost:8080"
                className="w-full px-3 py-2 bg-slate-100 border border-slate-200 rounded-lg text-xs font-mono text-slate-600 cursor-not-allowed"
              />
              <span className="text-[11px] text-slate-400 mt-1 block">Configured via VITE_API_BASE_URL</span>
            </div>

            <div>
              <label className="block text-slate-700 font-semibold mb-1">PostgreSQL Target</label>
              <input
                type="text"
                disabled
                value="localhost:5432/township_db (PostgreSQL 16.15)"
                className="w-full px-3 py-2 bg-slate-100 border border-slate-200 rounded-lg text-xs font-mono text-emerald-700 cursor-not-allowed"
              />
              <span className="text-[11px] text-emerald-600 mt-1 block">Connected & Healthy</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

