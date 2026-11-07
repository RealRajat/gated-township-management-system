import { useState, useEffect, useCallback } from 'react';
import {
  Users,
  ShieldCheck,
  Landmark,
  FileText,
  Mail,
  Building,
  CheckCircle2,
  TrendingUp,
  Download,
  Calendar,
  AlertTriangle,
  Clock,
  Activity,
  Layers,
  ChevronDown,
  RefreshCw,
  Search,
  Filter,
  DollarSign,
  AlertCircle,
  Phone,
  Lock,
  ArrowUpRight,
  UserCheck
} from 'lucide-react';
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  Legend,
  CartesianGrid,
  PieChart,
  Pie,
  Cell
} from 'recharts';
import DemoNoticeBanner from '../components/DemoNoticeBanner';
import StatCard from '../components/StatCard';
import { useAuth } from '../context/AuthContext';
import { committeeApi, townshipApi, downloadBlob } from '../services/api';
import { committeeMembers } from '../data/mockData';

const COLORS = ['#0d9488', '#0284c7', '#6366f1', '#8b5cf6', '#ec4899', '#f59e0b', '#10b981', '#64748b'];

const expenseCategories = [
  { name: 'Common Area Electricity (MSEDCL)', amount: '₹18,40,000', percentage: '26%', color: 'bg-teal-500' },
  { name: 'Security Services (Apex Guards - 28 Towers)', amount: '₹16,20,000', percentage: '23%', color: 'bg-sky-500' },
  { name: 'Housekeeping & Sanitization', amount: '₹14,50,000', percentage: '20%', color: 'bg-indigo-500' },
  { name: 'Elevator AMC (Schindler & Otis - 56 Lifts)', amount: '₹8,90,000', percentage: '12%', color: 'bg-violet-500' },
  { name: 'PMC Water Tanker Supplementary Supply', amount: '₹7,20,000', percentage: '10%', color: 'bg-amber-500' },
  { name: 'Clubhouse, Pool & Landscaping Care', amount: '₹6,40,000', percentage: '9%', color: 'bg-emerald-500' },
];

export default function CommitteePage() {
  const { user, isCommittee, isHelpDesk, login } = useAuth();
  const canAccess = isCommittee || isHelpDesk;

  // Active Tab
  const [activeTab, setActiveTab] = useState('financial'); // 'financial' | 'complaints' | 'operations' | 'governance'

  // Filters
  const [selectedTower, setSelectedTower] = useState('ALL');
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const [towers, setTowers] = useState([]);

  // Data states
  const [dashboardData, setDashboardData] = useState(null);
  const [defaulters, setDefaulters] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [downloadingCsv, setDownloadingCsv] = useState(false);

  // Search in tables
  const [towerSearch, setTowerSearch] = useState('');
  const [defaulterSearch, setDefaulterSearch] = useState('');

  // Switch account loading
  const [switchingUser, setSwitchingUser] = useState(false);

  // Format currency helper
  const formatINR = (val) => {
    if (val === null || val === undefined || isNaN(Number(val))) return '₹0.00';
    const num = typeof val === 'number' ? val : parseFloat(val);
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 2,
    }).format(num);
  };

  // Load towers list on mount
  useEffect(() => {
    const fetchTowers = async () => {
      try {
        const res = await townshipApi.getTowers();
        setTowers(res || []);
      } catch (err) {
        console.warn('Could not load towers list:', err.message);
      }
    };
    fetchTowers();
  }, []);

  // Fetch dashboard and defaulters
  const loadData = useCallback(async () => {
    if (!canAccess) {
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const params = {
        fromDate: fromDate || undefined,
        toDate: toDate || undefined,
        towerId: selectedTower !== 'ALL' ? selectedTower : undefined,
      };

      const [dashRes, defRes] = await Promise.all([
        committeeApi.getDashboard(params),
        committeeApi.getDefaulters(selectedTower !== 'ALL' ? selectedTower : undefined),
      ]);

      setDashboardData(dashRes);
      setDefaulters(defRes || []);
    } catch (err) {
      console.error('Error loading committee analytics:', err);
      setError(err.message || 'Failed to load committee analytics');
    } finally {
      setLoading(false);
    }
  }, [canAccess, selectedTower, fromDate, toDate]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Handle CSV exports
  const handleExport = async (type) => {
    try {
      setDownloadingCsv(true);
      const params = {
        fromDate: fromDate || undefined,
        toDate: toDate || undefined,
        towerId: selectedTower !== 'ALL' ? selectedTower : undefined,
      };

      let blob;
      let filename;
      const stamp = new Date().toISOString().split('T')[0];

      if (type === 'financial') {
        blob = await committeeApi.downloadFinancialCsv(params);
        filename = `financial-report-${stamp}.csv`;
      } else if (type === 'defaulters') {
        blob = await committeeApi.downloadDefaultersCsv(selectedTower !== 'ALL' ? selectedTower : undefined);
        filename = `defaulters-report-${stamp}.csv`;
      } else if (type === 'complaints') {
        blob = await committeeApi.downloadComplaintsCsv(params);
        filename = `complaints-report-${stamp}.csv`;
      } else if (type === 'operational') {
        blob = await committeeApi.downloadOperationalCsv(params);
        filename = `operational-summary-${stamp}.csv`;
      }

      if (blob && filename) {
        downloadBlob(blob, filename);
      }
    } catch (err) {
      alert('Error downloading report: ' + err.message);
    } finally {
      setDownloadingCsv(false);
    }
  };

  // Quick Date Filter presets
  const applyPreset = (preset) => {
    const today = new Date();
    if (preset === 'ALL') {
      setFromDate('');
      setToDate('');
    } else if (preset === 'YTD') {
      const ytd = `${today.getFullYear()}-01-01`;
      setFromDate(ytd);
      setToDate(today.toISOString().split('T')[0]);
    } else if (preset === '30D') {
      const d30 = new Date(today.getTime() - 30 * 24 * 60 * 60 * 1000);
      setFromDate(d30.toISOString().split('T')[0]);
      setToDate(today.toISOString().split('T')[0]);
    }
  };

  // Fast switch to Committee demo user
  const handleSwitchToCommittee = async () => {
    try {
      setSwitchingUser(true);
      await login('committee@grandvistapune.in', 'Committee@123');
    } catch (err) {
      alert('Failed to login as Managing Committee: ' + err.message);
    } finally {
      setSwitchingUser(false);
    }
  };

  // If user cannot access committee
  if (!canAccess) {
    return (
      <div className="space-y-6">
        <DemoNoticeBanner moduleName="Committee & RWA Governance" />

        <div className="bg-white rounded-xl border border-amber-200 p-8 shadow-sm text-center max-w-2xl mx-auto my-12">
          <div className="w-16 h-16 bg-amber-50 text-amber-600 rounded-full flex items-center justify-center mx-auto mb-4">
            <Lock className="w-8 h-8" />
          </div>
          <h2 className="text-xl font-bold text-slate-900 mb-2">
            Restricted Executive Committee Access
          </h2>
          <p className="text-sm text-slate-600 mb-6 leading-relaxed">
            The Managing Committee & RWA Analytics portal contains executive financial reports,
            dues ageing metrics, defaulter ledgers, and township-wide operational audit data.
            This module is restricted to <strong>ROLE_COMMITTEE</strong> and <strong>ROLE_HELP_DESK</strong>.
          </p>
          <div className="bg-slate-50 border border-slate-200 rounded-lg p-4 mb-6 text-left text-xs text-slate-600 space-y-1">
            <p><strong>Current Active Account:</strong> {user?.fullName || 'Resident'} ({user?.email})</p>
            <p><strong>Active Role:</strong> <span className="font-mono bg-slate-200 px-1 py-0.5 rounded">{user?.role}</span></p>
          </div>
          <button
            onClick={handleSwitchToCommittee}
            disabled={switchingUser}
            className="inline-flex items-center gap-2 px-5 py-2.5 bg-teal-600 hover:bg-teal-700 text-white rounded-lg text-sm font-semibold shadow-sm transition disabled:opacity-50"
          >
            <UserCheck className="w-4 h-4" />
            {switchingUser ? 'Signing in...' : 'Sign in as Managing Committee President'}
          </button>
        </div>
      </div>
    );
  }

  const fin = dashboardData?.financial;
  const trends = dashboardData?.collectionTrends || [];
  const ageing = dashboardData?.duesAgeing || [];
  const towerDues = dashboardData?.towerDues || [];
  const comp = dashboardData?.complaints;
  const op = dashboardData?.operational;

  // Prepare chart data for complaints category
  const complaintCategoryData = comp?.categoryBreakdown
    ? Object.entries(comp.categoryBreakdown).map(([name, value]) => ({ name, value }))
    : [];

  // Filtered tower dues table
  const filteredTowerDues = towerDues.filter(
    (t) =>
      t.towerId?.toLowerCase().includes(towerSearch.toLowerCase()) ||
      t.towerName?.toLowerCase().includes(towerSearch.toLowerCase())
  );

  // Filtered defaulters
  const filteredDefaulters = defaulters.filter(
    (d) =>
      d.towerId?.toLowerCase().includes(defaulterSearch.toLowerCase()) ||
      d.flatNumber?.toLowerCase().includes(defaulterSearch.toLowerCase()) ||
      d.towerName?.toLowerCase().includes(defaulterSearch.toLowerCase())
  );

  return (
    <div className="space-y-6">
      <DemoNoticeBanner moduleName="Managing Committee & RWA Governance" />

      {/* Page Header */}
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold text-slate-900 tracking-tight">
              Managing Committee & Executive Intelligence
            </h1>
            <span className="px-2 py-0.5 text-xs font-semibold bg-teal-100 text-teal-800 rounded-full border border-teal-200">
              Live PostgreSQL Analytics
            </span>
          </div>
          <p className="text-xs sm:text-sm text-slate-500 mt-0.5">
            Real-time financial audits, dues ageing buckets, maintenance SLAs, and township operations across 28 towers.
          </p>
        </div>

        {/* Global Export CSV Buttons */}
        <div className="flex flex-wrap items-center gap-2">
          <button
            onClick={() => handleExport('financial')}
            disabled={downloadingCsv}
            className="inline-flex items-center gap-1.5 px-3 py-2 bg-white hover:bg-slate-50 text-slate-700 border border-slate-200 rounded-lg text-xs font-semibold shadow-sm transition disabled:opacity-50"
            title="Export full financial billing report as CSV"
          >
            <Download className="w-3.5 h-3.5 text-slate-500" />
            Financial CSV
          </button>
          <button
            onClick={() => handleExport('defaulters')}
            disabled={downloadingCsv}
            className="inline-flex items-center gap-1.5 px-3 py-2 bg-white hover:bg-slate-50 text-slate-700 border border-slate-200 rounded-lg text-xs font-semibold shadow-sm transition disabled:opacity-50"
            title="Export privacy-safe defaulters summary as CSV"
          >
            <Download className="w-3.5 h-3.5 text-slate-500" />
            Defaulters CSV
          </button>
          <button
            onClick={() => handleExport('complaints')}
            disabled={downloadingCsv}
            className="inline-flex items-center gap-1.5 px-3 py-2 bg-white hover:bg-slate-50 text-slate-700 border border-slate-200 rounded-lg text-xs font-semibold shadow-sm transition disabled:opacity-50"
            title="Export complaints resolution log as CSV"
          >
            <Download className="w-3.5 h-3.5 text-slate-500" />
            Complaints CSV
          </button>
          <button
            onClick={() => handleExport('operational')}
            disabled={downloadingCsv}
            className="inline-flex items-center gap-1.5 px-3 py-2 bg-white hover:bg-slate-50 text-slate-700 border border-slate-200 rounded-lg text-xs font-semibold shadow-sm transition disabled:opacity-50"
            title="Export operational summary as CSV"
          >
            <Download className="w-3.5 h-3.5 text-slate-500" />
            Operations CSV
          </button>
        </div>
      </div>

      {/* Filter Toolbar */}
      <div className="bg-white border border-slate-200 rounded-xl p-4 shadow-sm space-y-3">
        <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3">
          <div className="flex flex-wrap items-center gap-3">
            {/* Tower Filter */}
            <div className="flex items-center gap-2">
              <span className="text-xs font-medium text-slate-600 flex items-center gap-1">
                <Building className="w-3.5 h-3.5 text-slate-400" />
                Tower:
              </span>
              <select
                value={selectedTower}
                onChange={(e) => setSelectedTower(e.target.value)}
                className="text-xs bg-slate-50 border border-slate-200 rounded-lg px-2.5 py-1.5 font-medium text-slate-700 focus:outline-none focus:ring-1 focus:ring-teal-500"
              >
                <option value="ALL">All 28 Towers (Township-wide)</option>
                {towers.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.id} - {t.name}
                  </option>
                ))}
              </select>
            </div>

            {/* Date Range Filters */}
            <div className="flex items-center gap-2">
              <span className="text-xs font-medium text-slate-600 flex items-center gap-1">
                <Calendar className="w-3.5 h-3.5 text-slate-400" />
                From:
              </span>
              <input
                type="date"
                value={fromDate}
                onChange={(e) => setFromDate(e.target.value)}
                className="text-xs bg-slate-50 border border-slate-200 rounded-lg px-2 py-1 text-slate-700 focus:outline-none focus:ring-1 focus:ring-teal-500"
              />
              <span className="text-xs text-slate-400">to</span>
              <input
                type="date"
                value={toDate}
                onChange={(e) => setToDate(e.target.value)}
                className="text-xs bg-slate-50 border border-slate-200 rounded-lg px-2 py-1 text-slate-700 focus:outline-none focus:ring-1 focus:ring-teal-500"
              />
            </div>

            {/* Quick Presets */}
            <div className="flex items-center gap-1">
              <button
                onClick={() => applyPreset('ALL')}
                className={`text-[11px] px-2 py-1 rounded font-medium transition ${
                  !fromDate && !toDate
                    ? 'bg-teal-600 text-white'
                    : 'bg-slate-100 hover:bg-slate-200 text-slate-600'
                }`}
              >
                All Time
              </button>
              <button
                onClick={() => applyPreset('YTD')}
                className="text-[11px] px-2 py-1 rounded font-medium bg-slate-100 hover:bg-slate-200 text-slate-600 transition"
              >
                Year to Date
              </button>
              <button
                onClick={() => applyPreset('30D')}
                className="text-[11px] px-2 py-1 rounded font-medium bg-slate-100 hover:bg-slate-200 text-slate-600 transition"
              >
                Last 30 Days
              </button>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={loadData}
              disabled={loading}
              className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg text-xs font-medium transition disabled:opacity-50"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin text-teal-600' : 'text-slate-500'}`} />
              Refresh
            </button>
          </div>
        </div>
      </div>

      {/* Tabs Navigation */}
      <div className="flex border-b border-slate-200 space-x-6">
        <button
          onClick={() => setActiveTab('financial')}
          className={`pb-3 text-sm font-semibold transition relative ${
            activeTab === 'financial'
              ? 'text-teal-600 border-b-2 border-teal-600'
              : 'text-slate-500 hover:text-slate-700'
          }`}
        >
          Financial Health & Dues Ageing
        </button>
        <button
          onClick={() => setActiveTab('complaints')}
          className={`pb-3 text-sm font-semibold transition relative ${
            activeTab === 'complaints'
              ? 'text-teal-600 border-b-2 border-teal-600'
              : 'text-slate-500 hover:text-slate-700'
          }`}
        >
          Maintenance & Complaint SLAs
        </button>
        <button
          onClick={() => setActiveTab('operations')}
          className={`pb-3 text-sm font-semibold transition relative ${
            activeTab === 'operations'
              ? 'text-teal-600 border-b-2 border-teal-600'
              : 'text-slate-500 hover:text-slate-700'
          }`}
        >
          Township Operations & Amenities
        </button>
        <button
          onClick={() => setActiveTab('governance')}
          className={`pb-3 text-sm font-semibold transition relative ${
            activeTab === 'governance'
              ? 'text-teal-600 border-b-2 border-teal-600'
              : 'text-slate-500 hover:text-slate-700'
          }`}
        >
          Executive Governance & Reserves
        </button>
      </div>

      {loading && (
        <div className="py-12 text-center">
          <RefreshCw className="w-8 h-8 animate-spin text-teal-600 mx-auto mb-2" />
          <p className="text-sm text-slate-500">Aggregating live PostgreSQL committee intelligence...</p>
        </div>
      )}

      {error && !loading && (
        <div className="bg-red-50 border border-red-200 rounded-xl p-4 text-sm text-red-700 flex items-center gap-2">
          <AlertCircle className="w-5 h-5 flex-shrink-0" />
          <span>Error loading committee data: {error}</span>
        </div>
      )}

      {!loading && !error && (
        <>
          {/* ========================================================================= */}
          {/* TAB 1: FINANCIAL HEALTH & DUES AGEING */}
          {/* ========================================================================= */}
          {activeTab === 'financial' && (
            <div className="space-y-6">
              {/* Financial KPI Cards */}
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                <StatCard
                  title="Total Billed Amount"
                  value={formatINR(fin?.totalBilled)}
                  subtext={`${fin?.totalBillsCount || 0} total bills generated`}
                  icon={FileText}
                  colorScheme="navy"
                />
                <StatCard
                  title="Total Amount Collected"
                  value={formatINR(fin?.totalCollected)}
                  subtext={`${fin?.collectionPercentage || 0}% collection rate`}
                  icon={TrendingUp}
                  colorScheme="teal"
                  trend="up"
                  trendLabel={`${fin?.collectionPercentage || 0}%`}
                />
                <StatCard
                  title="Total Outstanding Dues"
                  value={formatINR(fin?.totalOutstanding)}
                  subtext={`${fin?.unpaidCount || 0} unpaid / ${fin?.partiallyPaidCount || 0} partial`}
                  icon={AlertTriangle}
                  colorScheme="amber"
                />
                <StatCard
                  title="Overdue Receivables"
                  value={formatINR(fin?.overdueAmount)}
                  subtext={`${fin?.overdueCount || 0} overdue flat bills`}
                  icon={AlertCircle}
                  colorScheme="rose"
                  trend="down"
                  trendLabel={`${fin?.overdueCount || 0} accounts`}
                />
              </div>

              {/* Monthly Trend & Dues Ageing Charts */}
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Month-wise Trend Chart */}
                <div className="bg-white border border-slate-200 rounded-xl p-5 shadow-sm space-y-4">
                  <div className="flex items-center justify-between">
                    <div>
                      <h3 className="text-base font-bold text-slate-900">
                        Month-wise Collection Trends
                      </h3>
                      <p className="text-xs text-slate-500">
                        Monthly billed amount versus verified collections
                      </p>
                    </div>
                    <span className="text-xs bg-slate-100 text-slate-600 px-2 py-1 rounded font-medium">
                      INR Currency
                    </span>
                  </div>

                  <div className="h-64">
                    {trends.length === 0 ? (
                      <div className="h-full flex items-center justify-center text-xs text-slate-400">
                        No monthly billing trends available for current selection.
                      </div>
                    ) : (
                      <ResponsiveContainer width="100%" height="100%">
                        <BarChart data={trends} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
                          <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                          <XAxis dataKey="month" stroke="#94a3b8" fontSize={11} tickLine={false} />
                          <YAxis
                            stroke="#94a3b8"
                            fontSize={11}
                            tickLine={false}
                            tickFormatter={(v) => `₹${(v / 1000).toFixed(0)}k`}
                          />
                          <Tooltip
                            formatter={(value) => [formatINR(value), '']}
                            labelFormatter={(label) => `Month: ${label}`}
                            contentStyle={{ borderRadius: '8px', border: '1px solid #e2e8f0', fontSize: '12px' }}
                          />
                          <Legend wrapperStyle={{ fontSize: '12px', paddingTop: '8px' }} />
                          <Bar dataKey="billedAmount" name="Billed Amount" fill="#0f172a" radius={[4, 4, 0, 0]} />
                          <Bar dataKey="collectedAmount" name="Collected Amount" fill="#0d9488" radius={[4, 4, 0, 0]} />
                        </BarChart>
                      </ResponsiveContainer>
                    )}
                  </div>
                </div>

                {/* Dues Ageing Buckets */}
                <div className="bg-white border border-slate-200 rounded-xl p-5 shadow-sm space-y-4">
                  <div className="flex items-center justify-between">
                    <div>
                      <h3 className="text-base font-bold text-slate-900">
                        Outstanding Dues Ageing Analysis
                      </h3>
                      <p className="text-xs text-slate-500">
                        Breakdown of unpaid arrears by duration past due date
                      </p>
                    </div>
                    <span className="text-xs bg-amber-50 text-amber-700 border border-amber-200 px-2 py-0.5 rounded-full font-medium">
                      Ageing Risk
                    </span>
                  </div>

                  <div className="h-64">
                    {ageing.length === 0 ? (
                      <div className="h-full flex items-center justify-center text-xs text-slate-400">
                        No outstanding dues recorded.
                      </div>
                    ) : (
                      <ResponsiveContainer width="100%" height="100%">
                        <BarChart data={ageing} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
                          <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                          <XAxis dataKey="bucket" stroke="#94a3b8" fontSize={11} tickLine={false} />
                          <YAxis
                            stroke="#94a3b8"
                            fontSize={11}
                            tickLine={false}
                            tickFormatter={(v) => `₹${(v / 1000).toFixed(0)}k`}
                          />
                          <Tooltip
                            formatter={(value, name) => [
                              formatINR(value),
                              name === 'totalOutstanding' ? 'Outstanding Dues' : name,
                            ]}
                            contentStyle={{ borderRadius: '8px', border: '1px solid #e2e8f0', fontSize: '12px' }}
                          />
                          <Bar dataKey="totalOutstanding" name="Outstanding Balance" fill="#f59e0b" radius={[4, 4, 0, 0]} />
                        </BarChart>
                      </ResponsiveContainer>
                    )}
                  </div>

                  {/* Ageing Table Mini Breakdown */}
                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 pt-2 border-t border-slate-100 text-center">
                    {ageing.map((b) => (
                      <div key={b.bucket} className="bg-slate-50 rounded-lg p-2 border border-slate-100">
                        <p className="text-[10px] font-medium text-slate-500">{b.bucket}</p>
                        <p className="text-xs font-bold text-slate-800">{formatINR(b.totalOutstanding)}</p>
                        <p className="text-[10px] text-slate-400">
                          {b.billCount} bills ({b.percentage}%)
                        </p>
                      </div>
                    ))}
                  </div>
                </div>
              </div>

              {/* Defaulter Summary Directory (Privacy-Safe) */}
              <div className="bg-white border border-slate-200 rounded-xl shadow-sm overflow-hidden">
                <div className="p-5 border-b border-slate-200 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                  <div>
                    <div className="flex items-center gap-2">
                      <h3 className="text-base font-bold text-slate-900">
                        High-Dues Defaulter Directory
                      </h3>
                      <span className="text-[11px] font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200 px-2 py-0.5 rounded-full flex items-center gap-1">
                        <ShieldCheck className="w-3 h-3 text-emerald-600" />
                        Privacy Protected
                      </span>
                    </div>
                    <p className="text-xs text-slate-500 mt-0.5">
                      Flats with outstanding maintenance balances. Personal phone numbers and passwords are strictly hidden.
                    </p>
                  </div>

                  <div className="flex items-center gap-2">
                    <div className="relative">
                      <Search className="w-3.5 h-3.5 absolute left-2.5 top-2.5 text-slate-400" />
                      <input
                        type="text"
                        placeholder="Search flat or tower..."
                        value={defaulterSearch}
                        onChange={(e) => setDefaulterSearch(e.target.value)}
                        className="pl-8 pr-3 py-1.5 text-xs bg-slate-50 border border-slate-200 rounded-lg focus:outline-none focus:ring-1 focus:ring-teal-500"
                      />
                    </div>
                    <button
                      onClick={() => handleExport('defaulters')}
                      disabled={downloadingCsv}
                      className="inline-flex items-center gap-1 px-3 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg text-xs font-semibold transition"
                    >
                      <Download className="w-3.5 h-3.5 text-slate-500" />
                      Export
                    </button>
                  </div>
                </div>

                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs">
                    <thead className="bg-slate-50 text-slate-600 font-semibold uppercase tracking-wider border-b border-slate-200">
                      <tr>
                        <th className="py-3 px-4">Tower</th>
                        <th className="py-3 px-4">Flat Number</th>
                        <th className="py-3 px-4">Carpet Area</th>
                        <th className="py-3 px-4">Outstanding Balance</th>
                        <th className="py-3 px-4">Days Overdue</th>
                        <th className="py-3 px-4">Oldest Due Date</th>
                        <th className="py-3 px-4">Last Payment</th>
                        <th className="py-3 px-4">Recovery Status</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 font-medium">
                      {filteredDefaulters.length === 0 ? (
                        <tr>
                          <td colSpan="8" className="py-8 text-center text-slate-400">
                            No defaulters found for current tower selection.
                          </td>
                        </tr>
                      ) : (
                        filteredDefaulters.map((d) => (
                          <tr key={`${d.towerId}-${d.flatNumber}`} className="hover:bg-slate-50/80 transition">
                            <td className="py-3 px-4 font-bold text-slate-900">
                              {d.towerId}
                              <span className="block text-[11px] font-normal text-slate-500">{d.towerName}</span>
                            </td>
                            <td className="py-3 px-4 font-mono font-semibold text-slate-800">
                              {d.flatNumber}
                            </td>
                            <td className="py-3 px-4 text-slate-600">{d.carpetArea} sq ft</td>
                            <td className="py-3 px-4 font-bold text-rose-600">
                              {formatINR(d.outstandingDues)}
                            </td>
                            <td className="py-3 px-4">
                              <span
                                className={`px-2 py-0.5 rounded-full text-[10px] font-semibold ${
                                  d.daysOverdue > 90
                                    ? 'bg-rose-100 text-rose-800'
                                    : d.daysOverdue > 60
                                    ? 'bg-amber-100 text-amber-800'
                                    : 'bg-slate-100 text-slate-700'
                                }`}
                              >
                                {d.daysOverdue} days
                              </span>
                            </td>
                            <td className="py-3 px-4 text-slate-600">{d.oldestDueDate || '—'}</td>
                            <td className="py-3 px-4 text-slate-600">{d.lastPaymentDate || 'No payments recorded'}</td>
                            <td className="py-3 px-4">
                              {d.daysOverdue > 90 ? (
                                <span className="inline-flex items-center gap-1 text-[11px] text-rose-600 font-semibold">
                                  <AlertTriangle className="w-3 h-3" />
                                  Legal Notice Recommended
                                </span>
                              ) : (
                                <span className="text-[11px] text-amber-600 font-semibold">
                                  Reminder Dispatched
                                </span>
                              )}
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
              </div>

              {/* Tower-wise Financial Health Table */}
              <div className="bg-white border border-slate-200 rounded-xl shadow-sm overflow-hidden">
                <div className="p-5 border-b border-slate-200 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                  <div>
                    <h3 className="text-base font-bold text-slate-900">
                      Tower-wise Financial Health & Collection Rates
                    </h3>
                    <p className="text-xs text-slate-500 mt-0.5">
                      Comparison across all 28 towers (Orchid T01 through Foxglove T28)
                    </p>
                  </div>

                  <div className="relative">
                    <Search className="w-3.5 h-3.5 absolute left-2.5 top-2.5 text-slate-400" />
                    <input
                      type="text"
                      placeholder="Filter towers..."
                      value={towerSearch}
                      onChange={(e) => setTowerSearch(e.target.value)}
                      className="pl-8 pr-3 py-1.5 text-xs bg-slate-50 border border-slate-200 rounded-lg focus:outline-none focus:ring-1 focus:ring-teal-500"
                    />
                  </div>
                </div>

                <div className="overflow-x-auto max-h-96">
                  <table className="w-full text-left text-xs">
                    <thead className="bg-slate-50 text-slate-600 font-semibold uppercase tracking-wider sticky top-0 border-b border-slate-200 z-10">
                      <tr>
                        <th className="py-3 px-4">Tower</th>
                        <th className="py-3 px-4">Flats</th>
                        <th className="py-3 px-4">Total Billed</th>
                        <th className="py-3 px-4">Total Collected</th>
                        <th className="py-3 px-4">Outstanding Dues</th>
                        <th className="py-3 px-4">Collection Rate</th>
                        <th className="py-3 px-4">Defaulters</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 font-medium">
                      {filteredTowerDues.map((t) => (
                        <tr key={t.towerId} className="hover:bg-slate-50/80 transition">
                          <td className="py-3 px-4 font-bold text-slate-900">
                            {t.towerId}
                            <span className="ml-2 font-normal text-slate-500">{t.towerName}</span>
                          </td>
                          <td className="py-3 px-4 text-slate-600">{t.flatCount}</td>
                          <td className="py-3 px-4 text-slate-800">{formatINR(t.totalBilled)}</td>
                          <td className="py-3 px-4 text-teal-600 font-semibold">{formatINR(t.totalCollected)}</td>
                          <td className="py-3 px-4 font-semibold text-rose-600">{formatINR(t.outstandingDues)}</td>
                          <td className="py-3 px-4">
                            <div className="flex items-center gap-2">
                              <div className="w-16 bg-slate-100 rounded-full h-1.5 overflow-hidden">
                                <div
                                  className="bg-teal-600 h-1.5 rounded-full"
                                  style={{ width: `${Math.min(100, t.collectionRate)}%` }}
                                />
                              </div>
                              <span className="font-semibold text-slate-700">{t.collectionRate}%</span>
                            </div>
                          </td>
                          <td className="py-3 px-4">
                            {t.defaulterCount > 0 ? (
                              <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-rose-50 text-rose-700 border border-rose-200">
                                {t.defaulterCount} flats
                              </span>
                            ) : (
                              <span className="text-[10px] text-teal-600 font-semibold">Clean</span>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}

          {/* ========================================================================= */}
          {/* TAB 2: MAINTENANCE & COMPLAINT SLAS */}
          {/* ========================================================================= */}
          {activeTab === 'complaints' && (
            <div className="space-y-6">
              {/* Complaint KPIs */}
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                <StatCard
                  title="Total Complaints"
                  value={comp?.totalComplaints || 0}
                  subtext="Logged in filtered period"
                  icon={Activity}
                  colorScheme="navy"
                />
                <StatCard
                  title="In Progress / Open"
                  value={(comp?.openCount || 0) + (comp?.assignedCount || 0) + (comp?.inProgressCount || 0)}
                  subtext={`${comp?.resolvedCount || 0} successfully resolved`}
                  icon={Clock}
                  colorScheme="teal"
                />
                <StatCard
                  title="Avg First Response"
                  value={`${comp?.avgFirstResponseHours || 0} hrs`}
                  subtext="From submission to staff response"
                  icon={TrendingUp}
                  colorScheme="emerald"
                />
                <StatCard
                  title="Avg Resolution Time"
                  value={`${comp?.avgResolutionHours || 0} hrs`}
                  subtext="End-to-end turnaround"
                  icon={CheckCircle2}
                  colorScheme="sky"
                />
              </div>

              {/* SLA Breaches & Critical Safety Banner */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="bg-rose-50 border border-rose-200 rounded-xl p-4 flex items-center justify-between">
                  <div>
                    <p className="text-xs font-semibold text-rose-700 uppercase tracking-wider">
                      SLA Breached Complaints
                    </p>
                    <p className="text-2xl font-bold text-rose-950 mt-1">
                      {comp?.slaBreachCount || 0}
                    </p>
                    <p className="text-xs text-rose-600 mt-0.5">
                      Exceeded designated resolution deadline
                    </p>
                  </div>
                  <div className="w-12 h-12 bg-rose-100 rounded-xl flex items-center justify-center text-rose-600">
                    <AlertTriangle className="w-6 h-6" />
                  </div>
                </div>

                <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 flex items-center justify-between">
                  <div>
                    <p className="text-xs font-semibold text-amber-700 uppercase tracking-wider">
                      Critical Safety Escalations
                    </p>
                    <p className="text-2xl font-bold text-amber-950 mt-1">
                      {comp?.criticalEscalationsCount || 0}
                    </p>
                    <p className="text-xs text-amber-600 mt-0.5">
                      Triggered by safety keywords (Gas, Fire, Lift Trap)
                    </p>
                  </div>
                  <div className="w-12 h-12 bg-amber-100 rounded-xl flex items-center justify-center text-amber-600">
                    <ShieldCheck className="w-6 h-6" />
                  </div>
                </div>
              </div>

              {/* Category Breakdown & Status Breakdown */}
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Category Pie Chart */}
                <div className="bg-white border border-slate-200 rounded-xl p-5 shadow-sm space-y-4">
                  <h3 className="text-base font-bold text-slate-900">
                    Complaints by Category
                  </h3>
                  <p className="text-xs text-slate-500">
                    Distribution across civil, electrical, lift, plumbing, and housekeeping
                  </p>

                  <div className="h-64">
                    {complaintCategoryData.length === 0 ? (
                      <div className="h-full flex items-center justify-center text-xs text-slate-400">
                        No category data available.
                      </div>
                    ) : (
                      <ResponsiveContainer width="100%" height="100%">
                        <PieChart>
                          <Pie
                            data={complaintCategoryData}
                            dataKey="value"
                            nameKey="name"
                            cx="50%"
                            cy="50%"
                            outerRadius={80}
                            label={({ name, percent }) => `${name} (${(percent * 100).toFixed(0)}%)`}
                            labelLine={false}
                          >
                            {complaintCategoryData.map((entry, index) => (
                              <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                            ))}
                          </Pie>
                          <Tooltip
                            formatter={(v, name) => [`${v} complaints`, name]}
                            contentStyle={{ borderRadius: '8px', border: '1px solid #e2e8f0', fontSize: '12px' }}
                          />
                        </PieChart>
                      </ResponsiveContainer>
                    )}
                  </div>
                </div>

                {/* Priority Breakdown Bar Chart */}
                <div className="bg-white border border-slate-200 rounded-xl p-5 shadow-sm space-y-4">
                  <h3 className="text-base font-bold text-slate-900">
                    Complaints by Priority Level
                  </h3>
                  <p className="text-xs text-slate-500">
                    Urgency classification (LOW, MEDIUM, HIGH, EMERGENCY)
                  </p>

                  <div className="h-64 flex flex-col justify-center space-y-3">
                    {comp?.priorityBreakdown &&
                      Object.entries(comp.priorityBreakdown).map(([priority, count]) => {
                        const total = comp.totalComplaints || 1;
                        const pct = Math.round((count / total) * 100);
                        return (
                          <div key={priority} className="space-y-1">
                            <div className="flex justify-between text-xs font-medium">
                              <span className="text-slate-700 font-semibold">{priority}</span>
                              <span className="text-slate-500">
                                {count} ({pct}%)
                              </span>
                            </div>
                            <div className="w-full bg-slate-100 rounded-full h-2 overflow-hidden">
                              <div
                                className={`h-2 rounded-full ${
                                  priority === 'EMERGENCY'
                                    ? 'bg-rose-500'
                                    : priority === 'HIGH'
                                    ? 'bg-amber-500'
                                    : priority === 'MEDIUM'
                                    ? 'bg-sky-500'
                                    : 'bg-slate-400'
                                }`}
                                style={{ width: `${pct}%` }}
                              />
                            </div>
                          </div>
                        );
                      })}
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* ========================================================================= */}
          {/* TAB 3: TOWNSHIP OPERATIONS & AMENITIES */}
          {/* ========================================================================= */}
          {activeTab === 'operations' && (
            <div className="space-y-6">
              {/* Operations KPI Cards */}
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                <StatCard
                  title="Total Visitors Logged"
                  value={op?.totalVisitors || 0}
                  subtext={`${op?.todayCheckIns || 0} checked in today`}
                  icon={Users}
                  colorScheme="navy"
                />
                <StatCard
                  title="Active Visitors Inside"
                  value={op?.checkedInCurrently || 0}
                  subtext={`${op?.checkedOut || 0} checked out`}
                  icon={ShieldCheck}
                  colorScheme="teal"
                />
                <StatCard
                  title="Amenity Bookings"
                  value={op?.totalAmenityBookings || 0}
                  subtext={`${op?.cancellationRate || 0}% cancellation rate`}
                  icon={Calendar}
                  colorScheme="emerald"
                />
                <StatCard
                  title="Notice Read Engagement"
                  value={`${op?.noticeEngagementRate || 0}%`}
                  subtext={`${op?.totalNoticeReads || 0} confirmed reads`}
                  icon={FileText}
                  colorScheme="sky"
                />
              </div>

              {/* Top Booked Amenities & Pre-Approved vs Walk-in Visitors */}
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Top Amenities List */}
                <div className="bg-white border border-slate-200 rounded-xl p-5 shadow-sm space-y-4">
                  <div className="flex items-center justify-between">
                    <div>
                      <h3 className="text-base font-bold text-slate-900">
                        Most Booked Amenities
                      </h3>
                      <p className="text-xs text-slate-500">
                        Clubhouse and recreational facility utilization
                      </p>
                    </div>
                    <span className="text-xs bg-slate-100 text-slate-600 px-2 py-0.5 rounded font-medium">
                      Top 5 Facilities
                    </span>
                  </div>

                  <div className="divide-y divide-slate-100">
                    {op?.topAmenities && op.topAmenities.length > 0 ? (
                      op.topAmenities.map((amenity) => (
                        <div key={amenity.amenityId} className="py-3 flex items-center justify-between">
                          <div>
                            <p className="text-xs font-bold text-slate-800">{amenity.amenityName}</p>
                            <p className="text-[11px] text-slate-500">
                              Category: {amenity.category} • {amenity.totalAttendees} total attendees
                            </p>
                          </div>
                          <span className="px-2.5 py-1 rounded-full text-xs font-semibold bg-teal-50 text-teal-700 border border-teal-200">
                            {amenity.bookingCount} bookings
                          </span>
                        </div>
                      ))
                    ) : (
                      <p className="py-6 text-center text-xs text-slate-400">
                        No amenity bookings recorded yet.
                      </p>
                    )}
                  </div>
                </div>

                {/* Pre-Approved vs Walk-In Visitors */}
                <div className="bg-white border border-slate-200 rounded-xl p-5 shadow-sm space-y-4">
                  <h3 className="text-base font-bold text-slate-900">
                    Visitor Authorization Breakdown
                  </h3>
                  <p className="text-xs text-slate-500">
                    Gate security protocol compliance
                  </p>

                  <div className="space-y-4 pt-2">
                    <div className="bg-slate-50 rounded-lg p-4 border border-slate-100 space-y-2">
                      <div className="flex justify-between items-center text-xs font-semibold">
                        <span className="text-teal-700 flex items-center gap-1.5">
                          <CheckCircle2 className="w-4 h-4 text-teal-600" />
                          Resident Pre-Approved Passes
                        </span>
                        <span className="text-slate-900 font-bold">{op?.preApprovedCount || 0}</span>
                      </div>
                      <p className="text-[11px] text-slate-500">
                        QR passes and OTP codes generated by residents in advance.
                      </p>
                    </div>

                    <div className="bg-slate-50 rounded-lg p-4 border border-slate-100 space-y-2">
                      <div className="flex justify-between items-center text-xs font-semibold">
                        <span className="text-amber-700 flex items-center gap-1.5">
                          <Clock className="w-4 h-4 text-amber-600" />
                          Walk-In Gate Registrations
                        </span>
                        <span className="text-slate-900 font-bold">{op?.walkInCount || 0}</span>
                      </div>
                      <p className="text-[11px] text-slate-500">
                        Logged by Gate Guards with resident real-time approval at entry.
                      </p>
                    </div>
                  </div>
                </div>
              </div>

              {/* Tower-wise Operational Comparison Table */}
              <div className="bg-white border border-slate-200 rounded-xl shadow-sm overflow-hidden">
                <div className="p-5 border-b border-slate-200">
                  <h3 className="text-base font-bold text-slate-900">
                    Tower Operational Performance Comparison
                  </h3>
                  <p className="text-xs text-slate-500 mt-0.5">
                    Visitors, complaints, and outstanding dues correlated by tower
                  </p>
                </div>

                <div className="overflow-x-auto max-h-80">
                  <table className="w-full text-left text-xs">
                    <thead className="bg-slate-50 text-slate-600 font-semibold uppercase tracking-wider sticky top-0 border-b border-slate-200 z-10">
                      <tr>
                        <th className="py-3 px-4">Tower ID</th>
                        <th className="py-3 px-4">Tower Name</th>
                        <th className="py-3 px-4">Visitor Footfall</th>
                        <th className="py-3 px-4">Complaints Logged</th>
                        <th className="py-3 px-4">Outstanding Dues</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 font-medium">
                      {op?.towerOperationalComparison &&
                        op.towerOperationalComparison.map((t) => (
                          <tr key={t.towerId} className="hover:bg-slate-50/80 transition">
                            <td className="py-3 px-4 font-bold text-slate-900">{t.towerId}</td>
                            <td className="py-3 px-4 text-slate-700">{t.towerName}</td>
                            <td className="py-3 px-4 text-slate-700">{t.visitorCount} visitors</td>
                            <td className="py-3 px-4">
                              <span
                                className={`px-2 py-0.5 rounded-full text-[10px] font-semibold ${
                                  t.complaintCount > 3
                                    ? 'bg-amber-100 text-amber-800'
                                    : 'bg-slate-100 text-slate-700'
                                }`}
                              >
                                {t.complaintCount} complaints
                              </span>
                            </td>
                            <td className="py-3 px-4 font-semibold text-rose-600">
                              {formatINR(t.outstandingDues)}
                            </td>
                          </tr>
                        ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}

          {/* ========================================================================= */}
          {/* TAB 4: EXECUTIVE GOVERNANCE & RESERVES AUDIT */}
          {/* ========================================================================= */}
          {activeTab === 'governance' && (
            <div className="space-y-6">
              {/* Sinking Fund & Reserve Metrics */}
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                <StatCard
                  title="Sinking Fund Balance"
                  value="₹1.84 Cr"
                  subtext="Invested in Scheduled Bank FDs"
                  icon={Landmark}
                  colorScheme="teal"
                  trend="up"
                  trendLabel="+₹12 L / mo"
                />
                <StatCard
                  title="Operational Reserve"
                  value="₹46.20 L"
                  subtext="Contingency cash reserves"
                  icon={TrendingUp}
                  colorScheme="emerald"
                  trend="up"
                  trendLabel="Healthy"
                />
                <StatCard
                  title="Elected Committee"
                  value="7 Members"
                  subtext="Term: 2024–2027 (3 Years)"
                  icon={Users}
                  colorScheme="navy"
                />
                <StatCard
                  title="Statutory Compliance"
                  value="100% Audit"
                  subtext="FY 2025-26 Passed (Kirtane & Pandit LLP)"
                  icon={ShieldCheck}
                  colorScheme="sky"
                />
              </div>

              {/* Major Expenditures Breakdown */}
              <div className="bg-white border border-slate-200 rounded-xl p-5 shadow-sm space-y-4">
                <div className="flex items-center justify-between">
                  <div>
                    <h3 className="text-base font-bold text-slate-900">
                      Major Monthly Society Expenditures
                    </h3>
                    <p className="text-xs text-slate-500">
                      Key operational disbursements authorized by the Managing Committee
                    </p>
                  </div>
                  <span className="text-xs font-semibold text-slate-700 bg-slate-100 px-2.5 py-1 rounded-md">
                    Total: ₹71,60,000 / mo
                  </span>
                </div>

                <div className="space-y-3">
                  {expenseCategories.map((item) => (
                    <div key={item.name} className="space-y-1">
                      <div className="flex items-center justify-between text-xs font-medium">
                        <span className="text-slate-700">{item.name}</span>
                        <span className="text-slate-900 font-bold">
                          {item.amount} ({item.percentage})
                        </span>
                      </div>
                      <div className="w-full bg-slate-100 rounded-full h-2 overflow-hidden">
                        <div
                          className={`h-2 rounded-full ${item.color}`}
                          style={{ width: item.percentage }}
                        />
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* Elected Managing Committee Directory */}
              <div className="bg-white border border-slate-200 rounded-xl p-5 shadow-sm space-y-4">
                <div>
                  <h3 className="text-base font-bold text-slate-900">
                    Elected Managing Committee (2024–2027)
                  </h3>
                  <p className="text-xs text-slate-500">
                    Governing body registered under Maharashtra Co-operative Societies Act, 1960
                  </p>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                  {committeeMembers.map((member) => (
                    <div
                      key={member.name}
                      className="p-4 rounded-xl border border-slate-200 bg-slate-50/50 hover:bg-white hover:border-teal-200 transition space-y-2"
                    >
                      <div className="flex items-start justify-between">
                        <div>
                          <h4 className="text-sm font-bold text-slate-900">{member.name}</h4>
                          <p className="text-xs font-semibold text-teal-700">{member.role}</p>
                        </div>
                        <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-slate-200 text-slate-700">
                          {member.tower}
                        </span>
                      </div>
                      <div className="space-y-1 text-xs text-slate-600 pt-1 border-t border-slate-100">
                        <p className="flex items-center gap-1.5">
                          <Mail className="w-3.5 h-3.5 text-slate-400" />
                          {member.email}
                        </p>
                        <p className="flex items-center gap-1.5">
                          <Phone className="w-3.5 h-3.5 text-slate-400" />
                          {member.phone}
                        </p>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}
