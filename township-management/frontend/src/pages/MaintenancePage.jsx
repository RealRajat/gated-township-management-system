import { useState, useEffect, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Wrench,
  Receipt,
  Building2,
  Search,
  Filter,
  Download,
  Calculator,
  CheckCircle2,
  AlertCircle,
  Clock,
  ArrowRight,
  RotateCw,
  FileText,
  DollarSign,
  Calendar,
  Layers,
  ChevronRight,
  ShieldCheck,
  CreditCard,
  Info,
  Sliders,
  X,
  Sparkles,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { billingApi } from '../services/api';
import StatCard from '../components/StatCard';

export default function MaintenancePage() {
  const navigate = useNavigate();
  const { user, isHelpDesk } = useAuth();

  // Active view tab: For Help Desk: 'overview' | 'all-bills' | 'ledger' | 'config'
  // For Resident: 'my-bills' | 'my-ledger'
  const [activeTab, setActiveTab] = useState(isHelpDesk ? 'overview' : 'my-bills');

  // Resident state
  const [residentSummary, setResidentSummary] = useState(null);
  const [selectedFlatId, setSelectedFlatId] = useState(null);
  const [flatLedger, setFlatLedger] = useState([]);
  const [loadingLedger, setLoadingLedger] = useState(false);

  // Admin / Help Desk state
  const [adminOverview, setAdminOverview] = useState(null);
  const [adminBills, setAdminBills] = useState([]);
  const [adminConfig, setAdminConfig] = useState(null);
  const [billingMonthFilter, setBillingMonthFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [searchTerm, setSearchTerm] = useState('');

  // Modals & UI interactive states
  const [showGenerateModal, setShowGenerateModal] = useState(false);
  const [generating, setGenerating] = useState(false);
  const [generateForm, setGenerateForm] = useState({
    billingMonth: new Date().toISOString().substring(0, 7), // e.g. '2026-11'
    dueDays: 15,
    notes: '',
  });
  const [generateResult, setGenerateResult] = useState(null);
  const [showPaymentModal, setShowPaymentModal] = useState(false);
  const [selectedBillForPayment, setSelectedBillForPayment] = useState(null);
  const [activeLedgerModalFlat, setActiveLedgerModalFlat] = useState(null);

  // Global loading & error states
  const [loading, setLoading] = useState(true);
  const [errorMsg, setErrorMsg] = useState('');

  // Format currency in Indian Rupees
  const formatINR = (val) => {
    if (val === null || val === undefined) return '₹0.00';
    const num = Number(val);
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 2,
    }).format(num);
  };

  // Load Resident Data
  const loadResidentData = async () => {
    setLoading(true);
    setErrorMsg('');
    try {
      const summary = await billingApi.getMyBills();
      setResidentSummary(summary);
      if (summary?.flats?.length > 0 && !selectedFlatId) {
        setSelectedFlatId(summary.flats[0].flatId);
      }
    } catch (err) {
      setErrorMsg(err.message || 'Failed to load maintenance bills from database.');
    } finally {
      setLoading(false);
    }
  };

  // Load Admin Data
  const loadAdminData = async () => {
    setLoading(true);
    setErrorMsg('');
    try {
      const [overviewData, billsData, configData] = await Promise.all([
        billingApi.getAdminOverview(),
        billingApi.getAdminBills({ status: statusFilter, billingMonth: billingMonthFilter }),
        billingApi.getBillingConfig(),
      ]);
      setAdminOverview(overviewData);
      setAdminBills(billsData || []);
      setAdminConfig(configData);
    } catch (err) {
      setErrorMsg(err.message || 'Failed to load township billing records from database.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (isHelpDesk) {
      loadAdminData();
    } else {
      loadResidentData();
    }
  }, [isHelpDesk]);

  // Load ledger when selected flat changes or tab switches to ledger
  useEffect(() => {
    if (!isHelpDesk && selectedFlatId && activeTab === 'my-ledger') {
      loadFlatLedger(selectedFlatId);
    }
  }, [selectedFlatId, activeTab, isHelpDesk]);

  const loadFlatLedger = async (flatId) => {
    setLoadingLedger(true);
    try {
      const entries = await billingApi.getFlatLedger(flatId);
      setFlatLedger(entries || []);
    } catch (err) {
      console.error('Ledger load failed:', err);
    } finally {
      setLoadingLedger(false);
    }
  };

  // Handle Monthly Bill Generation
  const handleGenerateBills = async (e) => {
    e.preventDefault();
    setGenerating(true);
    setGenerateResult(null);
    try {
      const res = await billingApi.generateMonthlyBills(generateForm);
      setGenerateResult(res);
      await loadAdminData();
    } catch (err) {
      setGenerateResult({
        error: err.message || 'Bill generation failed.',
      });
    } finally {
      setGenerating(false);
    }
  };

  // Filtered Admin Bills
  const filteredAdminBills = useMemo(() => {
    return adminBills.filter((bill) => {
      const matchesSearch =
        bill.flatNumber.toLowerCase().includes(searchTerm.toLowerCase()) ||
        bill.towerName.toLowerCase().includes(searchTerm.toLowerCase()) ||
        bill.towerId.toLowerCase().includes(searchTerm.toLowerCase()) ||
        (bill.billingMonth && bill.billingMonth.includes(searchTerm));
      const matchesStatus =
        statusFilter === 'ALL' || bill.status === statusFilter;
      const matchesMonth =
        !billingMonthFilter || bill.billingMonth === billingMonthFilter;
      return matchesSearch && matchesStatus && matchesMonth;
    });
  }, [adminBills, searchTerm, statusFilter, billingMonthFilter]);

  // Status Badge Component
  const StatusBadge = ({ status }) => {
    switch (status) {
      case 'PAID':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
            <CheckCircle2 className="w-3 h-3 text-emerald-600" />
            PAID
          </span>
        );
      case 'PARTIALLY_PAID':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-sky-50 text-sky-700 border border-sky-200">
            <Clock className="w-3 h-3 text-sky-600" />
            PARTIAL
          </span>
        );
      case 'OVERDUE':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-rose-50 text-rose-700 border border-rose-200">
            <AlertCircle className="w-3 h-3 text-rose-600" />
            OVERDUE
          </span>
        );
      case 'UNPAID':
      default:
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-amber-50 text-amber-700 border border-amber-200">
            <Clock className="w-3 h-3 text-amber-600" />
            UNPAID
          </span>
        );
    }
  };

  return (
    <div className="space-y-6">
      {/* Page Title & Context Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-2">
        <div>
          <div className="flex items-center gap-2">
            <span className="text-xs font-semibold uppercase tracking-wider text-teal-600 font-mono">
              Phase 5 Active • PostgreSQL Connected
            </span>
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
          </div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight mt-0.5">
            Maintenance Billing & Flat Ledger
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 mt-0.5">
            {isHelpDesk
              ? 'Township-wide billing administration across all 28 towers, monthly bill generation, and financial audit ledgers.'
              : 'View your monthly apartment maintenance dues, payment audit history, and society flat ledger.'}
          </p>
        </div>

        {/* Action Controls */}
        <div className="flex items-center gap-2">
          {isHelpDesk ? (
            <>
              <button
                onClick={() => {
                  setShowGenerateModal(true);
                  setGenerateResult(null);
                }}
                className="inline-flex items-center gap-1.5 px-3.5 py-2 bg-teal-600 hover:bg-teal-700 text-white rounded-xl text-xs font-semibold shadow-sm transition cursor-pointer"
              >
                <Calculator className="w-4 h-4" />
                Generate Monthly Bills
              </button>
              <button
                onClick={loadAdminData}
                disabled={loading}
                className="p-2 rounded-xl bg-white border border-slate-200 hover:bg-slate-50 text-slate-600 transition cursor-pointer"
                title="Refresh from PostgreSQL"
              >
                <RotateCw className={`w-4 h-4 ${loading ? 'animate-spin text-teal-600' : ''}`} />
              </button>
            </>
          ) : (
            <button
              onClick={loadResidentData}
              disabled={loading}
              className="inline-flex items-center gap-1.5 px-3.5 py-2 bg-white border border-slate-200 hover:bg-slate-50 text-slate-700 rounded-xl text-xs font-semibold shadow-sm transition cursor-pointer"
            >
              <RotateCw className={`w-4 h-4 ${loading ? 'animate-spin text-teal-600' : ''}`} />
              Refresh My Bills
            </button>
          )}
        </div>
      </div>

      {/* Error Alert Banner */}
      {errorMsg && (
        <div className="p-4 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 text-xs flex items-center justify-between animate-in fade-in">
          <div className="flex items-center gap-2.5">
            <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
            <span className="font-medium">{errorMsg}</span>
          </div>
          <button
            onClick={isHelpDesk ? loadAdminData : loadResidentData}
            className="text-xs font-semibold underline hover:text-rose-900 cursor-pointer"
          >
            Try Again
          </button>
        </div>
      )}

      {/* =========================================================================
          RESIDENT PORTAL VIEW
         ========================================================================= */}
      {!isHelpDesk && (
        <div className="space-y-6">
          {/* Summary Metric Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <StatCard
              title="Total Monthly Dues"
              value={formatINR(residentSummary?.currentMonthlyDues)}
              subtext="Rate: ₹6.50 / sq.ft / mo"
              icon={Receipt}
              colorScheme="teal"
              trend="neutral"
              trendLabel="Based on unit area"
            />
            <StatCard
              title="Total Paid to Date"
              value={formatINR(residentSummary?.totalPaidAmount)}
              subtext="Verified in PostgreSQL"
              icon={CheckCircle2}
              colorScheme="emerald"
              trend="up"
              trendLabel="Receipts Audited"
            />
            <StatCard
              title="Outstanding Balance"
              value={formatINR(residentSummary?.totalOutstandingBalance)}
              subtext={residentSummary?.totalOutstandingBalance > 0 ? 'Dues awaiting settlement' : 'Account in good standing'}
              icon={DollarSign}
              colorScheme={residentSummary?.totalOutstandingBalance > 0 ? 'amber' : 'emerald'}
              trend={residentSummary?.totalOutstandingBalance > 0 ? 'down' : 'up'}
              trendLabel={residentSummary?.totalOutstandingBalance > 0 ? 'Pending' : 'Zero Dues'}
            />
            <StatCard
              title="Overdue Notices"
              value={residentSummary?.overdueCount || 0}
              subtext="Overdue bills"
              icon={AlertCircle}
              colorScheme={residentSummary?.overdueCount > 0 ? 'rose' : 'indigo'}
              trend={residentSummary?.overdueCount > 0 ? 'down' : 'neutral'}
              trendLabel={residentSummary?.overdueCount > 0 ? 'Action required' : 'Current'}
            />
          </div>

          {/* Tab Selector */}
          <div className="flex items-center gap-2 border-b border-slate-200">
            <button
              onClick={() => setActiveTab('my-bills')}
              className={`pb-3 px-3 text-xs sm:text-sm font-semibold transition border-b-2 cursor-pointer ${
                activeTab === 'my-bills'
                  ? 'border-teal-500 text-teal-700'
                  : 'border-transparent text-slate-500 hover:text-slate-800'
              }`}
            >
              My Invoices & Bills ({residentSummary?.bills?.length || 0})
            </button>
            <button
              onClick={() => setActiveTab('my-ledger')}
              className={`pb-3 px-3 text-xs sm:text-sm font-semibold transition border-b-2 cursor-pointer ${
                activeTab === 'my-ledger'
                  ? 'border-teal-500 text-teal-700'
                  : 'border-transparent text-slate-500 hover:text-slate-800'
              }`}
            >
              Flat Ledger & Audit History
            </button>
          </div>

          {/* Tab 1: Resident Bills Table */}
          {activeTab === 'my-bills' && (
            <div className="bg-white rounded-xl border border-slate-200/90 shadow-sm overflow-hidden">
              <div className="p-4 sm:p-5 border-b border-slate-100 flex items-center justify-between">
                <div>
                  <h3 className="font-bold text-slate-900 text-sm">Monthly Maintenance Invoices</h3>
                  <p className="text-xs text-slate-500 mt-0.5">
                    Calculated at society base rate ₹6.50/sq.ft pursuant to apartment super built-up area.
                  </p>
                </div>
              </div>

              {loading ? (
                <div className="p-12 text-center text-xs text-slate-400">
                  <RotateCw className="w-6 h-6 mx-auto mb-2 text-teal-600 animate-spin" />
                  Loading invoices from PostgreSQL...
                </div>
              ) : residentSummary?.bills?.length === 0 ? (
                <div className="p-12 text-center text-xs text-slate-400">
                  <Receipt className="w-8 h-8 mx-auto mb-2 text-slate-300" />
                  No bills generated for your flat yet.
                </div>
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs">
                    <thead className="bg-slate-50 text-slate-500 font-semibold uppercase tracking-wider text-[10px] border-b border-slate-100">
                      <tr>
                        <th className="px-4 py-3">Bill Period</th>
                        <th className="px-4 py-3">Flat & Tower</th>
                        <th className="px-3 py-3 text-right">Area (sq.ft)</th>
                        <th className="px-3 py-3 text-right">Base Rate</th>
                        <th className="px-3 py-3 text-right">Bill Amount</th>
                        <th className="px-3 py-3 text-right">Paid</th>
                        <th className="px-3 py-3 text-right">Balance Due</th>
                        <th className="px-3 py-3">Due Date</th>
                        <th className="px-3 py-3 text-center">Status</th>
                        <th className="px-4 py-3 text-right">Action</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {residentSummary?.bills?.map((bill) => (
                        <tr key={bill.id} className="hover:bg-slate-50/80 transition">
                          <td className="px-4 py-3 font-mono font-bold text-slate-800">
                            {bill.billingMonth}
                          </td>
                          <td className="px-4 py-3">
                            <div className="font-semibold text-slate-900">
                              {bill.towerName} • #{bill.flatNumber}
                            </div>
                            <div className="text-[10px] text-slate-400 font-mono">
                              ID: #{bill.flatId}
                            </div>
                          </td>
                          <td className="px-3 py-3 text-right font-mono text-slate-600">
                            {bill.flatAreaSqFt} sq ft
                          </td>
                          <td className="px-3 py-3 text-right font-mono text-slate-600">
                            ₹{bill.ratePerSqFt}
                          </td>
                          <td className="px-3 py-3 text-right font-mono font-bold text-slate-900">
                            {formatINR(bill.baseAmount)}
                          </td>
                          <td className="px-3 py-3 text-right font-mono font-semibold text-emerald-700">
                            {formatINR(bill.paidAmount)}
                          </td>
                          <td className="px-3 py-3 text-right font-mono font-bold text-amber-700">
                            {formatINR(bill.outstandingBalance)}
                          </td>
                          <td className="px-3 py-3 text-slate-600 font-mono text-[11px]">
                            {bill.dueDate}
                          </td>
                          <td className="px-3 py-3 text-center">
                            <StatusBadge status={bill.status} />
                          </td>
                          <td className="px-4 py-3 text-right">
                            {bill.outstandingBalance > 0 ? (
                              <button
                                onClick={() => navigate(`/payments?billId=${bill.id}`)}
                                className="inline-flex items-center gap-1 px-3 py-1 bg-teal-600 hover:bg-teal-700 text-white rounded-lg text-[11px] font-semibold transition cursor-pointer shadow-sm"
                              >
                                <CreditCard className="w-3 h-3" />
                                Pay Now (Test Mode)
                              </button>
                            ) : (
                              <span className="text-[11px] text-slate-400 font-medium">
                                Settled
                              </span>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}

          {/* Tab 2: Resident Flat Ledger */}
          {activeTab === 'my-ledger' && (
            <div className="bg-white rounded-xl border border-slate-200/90 shadow-sm overflow-hidden">
              <div className="p-4 sm:p-5 border-b border-slate-100 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                <div>
                  <h3 className="font-bold text-slate-900 text-sm">Apartment Financial Ledger</h3>
                  <p className="text-xs text-slate-500 mt-0.5">
                    Immutable transactional audit history of bill assessments, fee debits, and verified credit payments.
                  </p>
                </div>

                {/* Flat Switcher if multiple units */}
                {residentSummary?.flats?.length > 1 && (
                  <div className="flex items-center gap-2 text-xs">
                    <span className="text-slate-500 font-medium">Select Flat:</span>
                    <select
                      value={selectedFlatId}
                      onChange={(e) => setSelectedFlatId(Number(e.target.value))}
                      className="px-2.5 py-1 bg-slate-50 border border-slate-200 rounded-lg text-xs font-semibold focus:outline-none focus:border-teal-500"
                    >
                      {residentSummary.flats.map((fl) => (
                        <option key={fl.flatId} value={fl.flatId}>
                          {fl.towerName} #{fl.flatNumber}
                        </option>
                      ))}
                    </select>
                  </div>
                )}
              </div>

              {loadingLedger ? (
                <div className="p-12 text-center text-xs text-slate-400">
                  <RotateCw className="w-6 h-6 mx-auto mb-2 text-teal-600 animate-spin" />
                  Loading ledger transactions...
                </div>
              ) : flatLedger.length === 0 ? (
                <div className="p-12 text-center text-xs text-slate-400">
                  <FileText className="w-8 h-8 mx-auto mb-2 text-slate-300" />
                  No ledger entries recorded yet for this apartment.
                </div>
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full text-left text-xs">
                    <thead className="bg-slate-50 text-slate-500 font-semibold uppercase tracking-wider text-[10px] border-b border-slate-100">
                      <tr>
                        <th className="px-4 py-3">Date & Time</th>
                        <th className="px-3 py-3">Transaction Type</th>
                        <th className="px-4 py-3">Description</th>
                        <th className="px-4 py-3">Reference</th>
                        <th className="px-3 py-3 text-right">Debit (+)</th>
                        <th className="px-3 py-3 text-right">Credit (-)</th>
                        <th className="px-4 py-3 text-right">Running Balance</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100 font-mono text-[11px]">
                      {flatLedger.map((entry) => (
                        <tr key={entry.id} className="hover:bg-slate-50/80 transition">
                          <td className="px-4 py-3 text-slate-600">
                            {new Date(entry.entryDate).toLocaleString('en-IN', {
                              dateStyle: 'medium',
                              timeStyle: 'short',
                            })}
                          </td>
                          <td className="px-3 py-3">
                            <span className={`px-2 py-0.5 rounded text-[10px] font-semibold ${
                              entry.entryType === 'BILL_GENERATION'
                                ? 'bg-amber-100 text-amber-800'
                                : entry.entryType === 'PAYMENT'
                                ? 'bg-emerald-100 text-emerald-800'
                                : 'bg-slate-100 text-slate-700'
                            }`}>
                              {entry.entryType}
                            </span>
                          </td>
                          <td className="px-4 py-3 font-sans text-slate-800 font-medium">
                            {entry.description}
                          </td>
                          <td className="px-4 py-3 text-slate-500 text-[10px]">
                            {entry.transactionReference || '—'}
                          </td>
                          <td className="px-3 py-3 text-right font-semibold text-rose-700">
                            {entry.debit > 0 ? formatINR(entry.debit) : '—'}
                          </td>
                          <td className="px-3 py-3 text-right font-semibold text-emerald-700">
                            {entry.credit > 0 ? formatINR(entry.credit) : '—'}
                          </td>
                          <td className="px-4 py-3 text-right font-bold text-slate-900">
                            {formatINR(entry.runningBalance)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {/* =========================================================================
          HELP DESK / FACILITY MANAGEMENT VIEW
         ========================================================================= */}
      {isHelpDesk && (
        <div className="space-y-6">
          {/* Township-Wide Metrics */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <StatCard
              title="Total Township Billed"
              value={formatINR(adminOverview?.totalBilledAmount)}
              subtext="28 Towers Aggregate"
              icon={Receipt}
              colorScheme="teal"
              trend="up"
              trendLabel={`₹${adminOverview?.currentBaseRate || '6.50'} / sq.ft`}
            />
            <StatCard
              title="Total Collections"
              value={formatINR(adminOverview?.totalCollectedAmount)}
              subtext={`Collection Rate: ${adminOverview?.collectionRatePercentage || 0}%`}
              icon={CheckCircle2}
              colorScheme="emerald"
              trend="up"
              trendLabel="Verified"
            />
            <StatCard
              title="Outstanding Deficit"
              value={formatINR(adminOverview?.totalOutstandingAmount)}
              subtext="Unpaid & Partial dues"
              icon={DollarSign}
              colorScheme="amber"
              trend="down"
              trendLabel="Pending settlement"
            />
            <StatCard
              title="Overdue Invoices"
              value={adminOverview?.overdueCount || 0}
              subtext={`Across ${adminOverview?.totalBillsCount || 0} total invoices`}
              icon={AlertCircle}
              colorScheme="rose"
              trend="down"
              trendLabel="Grace period expired"
            />
          </div>

          {/* Admin Tab Selector */}
          <div className="flex items-center gap-2 border-b border-slate-200">
            <button
              onClick={() => setActiveTab('overview')}
              className={`pb-3 px-3 text-xs sm:text-sm font-semibold transition border-b-2 cursor-pointer ${
                activeTab === 'overview'
                  ? 'border-teal-500 text-teal-700'
                  : 'border-transparent text-slate-500 hover:text-slate-800'
              }`}
            >
              All Apartment Invoices ({filteredAdminBills.length})
            </button>
            <button
              onClick={() => setActiveTab('config')}
              className={`pb-3 px-3 text-xs sm:text-sm font-semibold transition border-b-2 cursor-pointer ${
                activeTab === 'config'
                  ? 'border-teal-500 text-teal-700'
                  : 'border-transparent text-slate-500 hover:text-slate-800'
              }`}
            >
              Society Billing Rules & Rates
            </button>
          </div>

          {/* Admin Table View */}
          {activeTab === 'overview' && (
            <div className="bg-white rounded-xl border border-slate-200/90 shadow-sm overflow-hidden">
              {/* Controls Bar */}
              <div className="p-4 sm:p-5 border-b border-slate-100 flex flex-col md:flex-row md:items-center justify-between gap-3">
                <div className="flex items-center gap-2">
                  <span className="text-sm font-bold text-slate-900 tracking-tight">
                    Township Billing Ledger
                  </span>
                  <span className="text-xs px-2 py-0.5 rounded-full bg-slate-100 text-slate-600 font-medium">
                    {filteredAdminBills.length} Invoices
                  </span>
                </div>

                <div className="flex flex-wrap items-center gap-2.5">
                  {/* Search */}
                  <div className="relative">
                    <Search className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                    <input
                      type="text"
                      value={searchTerm}
                      onChange={(e) => setSearchTerm(e.target.value)}
                      placeholder="Search flat (e.g. 1203), tower..."
                      className="w-48 pl-8 pr-3 py-1.5 bg-slate-50 border border-slate-200 text-xs rounded-lg focus:outline-none focus:border-teal-500"
                    />
                  </div>

                  {/* Status Filter */}
                  <select
                    value={statusFilter}
                    onChange={(e) => setStatusFilter(e.target.value)}
                    className="py-1.5 px-2.5 bg-slate-50 border border-slate-200 text-xs rounded-lg focus:outline-none focus:border-teal-500 font-medium"
                  >
                    <option value="ALL">All Statuses</option>
                    <option value="PAID">PAID</option>
                    <option value="PARTIALLY_PAID">PARTIALLY PAID</option>
                    <option value="UNPAID">UNPAID</option>
                    <option value="OVERDUE">OVERDUE</option>
                  </select>

                  {/* Month Filter */}
                  <input
                    type="month"
                    value={billingMonthFilter}
                    onChange={(e) => setBillingMonthFilter(e.target.value)}
                    className="py-1 px-2.5 bg-slate-50 border border-slate-200 text-xs rounded-lg focus:outline-none focus:border-teal-500"
                  />
                  {billingMonthFilter && (
                    <button
                      onClick={() => setBillingMonthFilter('')}
                      className="text-[11px] text-teal-600 hover:text-teal-800 underline cursor-pointer"
                    >
                      Clear
                    </button>
                  )}
                </div>
              </div>

              {/* Table */}
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-50 text-slate-500 font-semibold uppercase tracking-wider text-[10px] border-b border-slate-100">
                    <tr>
                      <th className="px-4 py-3">Month</th>
                      <th className="px-4 py-3">Tower & Unit</th>
                      <th className="px-3 py-3 text-right">Area</th>
                      <th className="px-3 py-3 text-right">Base Amount</th>
                      <th className="px-3 py-3 text-right">Paid</th>
                      <th className="px-3 py-3 text-right">Outstanding</th>
                      <th className="px-3 py-3">Due Date</th>
                      <th className="px-3 py-3 text-center">Status</th>
                      <th className="px-4 py-3 text-right">Audit</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 font-mono text-[11px]">
                    {filteredAdminBills.map((bill) => (
                      <tr key={bill.id} className="hover:bg-slate-50/80 transition">
                        <td className="px-4 py-3 font-bold text-slate-800">
                          {bill.billingMonth}
                        </td>
                        <td className="px-4 py-3 font-sans">
                          <div className="font-semibold text-slate-900">
                            {bill.towerName} • Unit #{bill.flatNumber}
                          </div>
                          <div className="text-[10px] text-slate-400 font-mono">
                            Flat ID: #{bill.flatId}
                          </div>
                        </td>
                        <td className="px-3 py-3 text-right text-slate-600">
                          {bill.flatAreaSqFt} sq ft
                        </td>
                        <td className="px-3 py-3 text-right font-bold text-slate-900">
                          {formatINR(bill.baseAmount)}
                        </td>
                        <td className="px-3 py-3 text-right font-semibold text-emerald-700">
                          {formatINR(bill.paidAmount)}
                        </td>
                        <td className="px-3 py-3 text-right font-bold text-amber-700">
                          {formatINR(bill.outstandingBalance)}
                        </td>
                        <td className="px-3 py-3 text-slate-600 text-[10px]">
                          {bill.dueDate}
                        </td>
                        <td className="px-3 py-3 text-center">
                          <StatusBadge status={bill.status} />
                        </td>
                        <td className="px-4 py-3 text-right">
                          <button
                            onClick={async () => {
                              setActiveLedgerModalFlat({
                                flatId: bill.flatId,
                                flatNumber: bill.flatNumber,
                                towerName: bill.towerName,
                              });
                              await loadFlatLedger(bill.flatId);
                            }}
                            className="inline-flex items-center gap-1 px-2.5 py-1 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg text-[10px] font-semibold transition cursor-pointer"
                          >
                            <FileText className="w-3 h-3 text-teal-600" />
                            Ledger
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* Admin Config View */}
          {activeTab === 'config' && (
            <div className="bg-white rounded-xl border border-slate-200/90 shadow-sm p-6 space-y-4">
              <div className="flex items-center gap-2 pb-3 border-b border-slate-100">
                <Sliders className="w-4 h-4 text-teal-600" />
                <h3 className="font-bold text-slate-900 text-sm">
                  Pune Grand Vista Society Billing Bylaws & Constants
                </h3>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-xs">
                <div className="p-4 bg-slate-50 rounded-xl border border-slate-100">
                  <span className="text-slate-400 block text-[11px] mb-1 font-medium">Base Maintenance Rate</span>
                  <div className="text-xl font-bold text-teal-700 font-mono">
                    ₹{adminConfig?.baseRatePerSqFt || '6.50'}
                  </div>
                  <span className="text-[10px] text-slate-500 block mt-1">Per square foot per month (Project Brief)</span>
                </div>

                <div className="p-4 bg-slate-50 rounded-xl border border-slate-100">
                  <span className="text-slate-400 block text-[11px] mb-1 font-medium">Grace Period</span>
                  <div className="text-xl font-bold text-slate-800 font-mono">
                    {adminConfig?.gracePeriodDays || 15} Days
                  </div>
                  <span className="text-[10px] text-slate-500 block mt-1">Days after bill issue date before overdue</span>
                </div>

                <div className="p-4 bg-slate-50 rounded-xl border border-slate-100">
                  <span className="text-slate-400 block text-[11px] mb-1 font-medium">Late Fee Provision</span>
                  <div className="text-xl font-bold text-slate-800 font-mono">
                    {adminConfig?.lateFeePercentage || '10.00'}%
                  </div>
                  <span className="text-[10px] text-slate-500 block mt-1">Configurable under Maharashtra Society Rules</span>
                </div>
              </div>

              <div className="p-3 bg-teal-50 border border-teal-200 rounded-xl text-xs text-teal-900 flex items-start gap-2">
                <ShieldCheck className="w-4 h-4 text-teal-600 shrink-0 mt-0.5" />
                <div>
                  <span className="font-semibold">Financial Precision Guarantee: </span>
                  All calculations strictly enforce <code className="font-mono font-bold">BigDecimal</code> and PostgreSQL <code className="font-mono font-bold">NUMERIC(12, 2)</code> precision. Floating-point arithmetic is strictly prohibited across all billing code.
                </div>
              </div>
            </div>
          )}
        </div>
      )}

      {/* =========================================================================
          MODAL: Generate Monthly Bills (Help Desk Only)
         ========================================================================= */}
      {showGenerateModal && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-2xl border border-slate-200 max-w-md w-full p-6 animate-in fade-in zoom-in-95">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <div className="flex items-center gap-2">
                <div className="p-2 bg-teal-50 rounded-lg text-teal-600">
                  <Calculator className="w-4 h-4" />
                </div>
                <h3 className="font-bold text-slate-900 text-sm">Generate Monthly Bills</h3>
              </div>
              <button
                onClick={() => setShowGenerateModal(false)}
                className="p-1 rounded-lg hover:bg-slate-100 text-slate-400 hover:text-slate-700 transition cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handleGenerateBills} className="space-y-4 mt-4 text-xs">
              <div>
                <label className="block text-slate-700 font-semibold mb-1">
                  Billing Cycle Month (YYYY-MM)
                </label>
                <input
                  type="month"
                  value={generateForm.billingMonth}
                  onChange={(e) => setGenerateForm({ ...generateForm, billingMonth: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl font-mono text-xs focus:outline-none focus:border-teal-500"
                  required
                />
              </div>

              <div>
                <label className="block text-slate-700 font-semibold mb-1">
                  Payment Due Grace Period (Days)
                </label>
                <input
                  type="number"
                  min="5"
                  max="60"
                  value={generateForm.dueDays}
                  onChange={(e) => setGenerateForm({ ...generateForm, dueDays: Number(e.target.value) })}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl font-mono text-xs focus:outline-none focus:border-teal-500"
                  required
                />
              </div>

              <div>
                <label className="block text-slate-700 font-semibold mb-1">
                  Invoice Notes & Circular Citation
                </label>
                <input
                  type="text"
                  placeholder="e.g. Q4 Maintenance run (Rate: ₹6.50/sq.ft)"
                  value={generateForm.notes}
                  onChange={(e) => setGenerateForm({ ...generateForm, notes: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs focus:outline-none focus:border-teal-500"
                />
              </div>

              {generateResult && (
                <div className={`p-3 rounded-xl text-xs ${
                  generateResult.error
                    ? 'bg-rose-50 text-rose-800 border border-rose-200'
                    : 'bg-emerald-50 text-emerald-800 border border-emerald-200'
                }`}>
                  {generateResult.error ? (
                    <div>{generateResult.error}</div>
                  ) : (
                    <div>
                      <div className="font-bold flex items-center gap-1">
                        <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                        Billing Generation Complete!
                      </div>
                      <div className="mt-1 font-mono text-[11px]">
                        • Bills Generated: {generateResult.billsGeneratedCount}<br />
                        • Duplicates Skipped (Idempotent): {generateResult.duplicatesSkippedCount}<br />
                        • Total Amount: {formatINR(generateResult.totalAmountBilled)}
                      </div>
                    </div>
                  )}
                </div>
              )}

              <div className="flex items-center justify-end gap-2 pt-2 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setShowGenerateModal(false)}
                  className="px-4 py-2 rounded-xl border border-slate-200 text-slate-600 hover:bg-slate-50 font-medium cursor-pointer"
                >
                  Close
                </button>
                <button
                  type="submit"
                  disabled={generating}
                  className="px-4 py-2 rounded-xl bg-teal-600 hover:bg-teal-700 text-white font-semibold transition cursor-pointer flex items-center gap-1.5 disabled:opacity-50"
                >
                  {generating ? (
                    <>
                      <RotateCw className="w-3.5 h-3.5 animate-spin" />
                      Running Cycle...
                    </>
                  ) : (
                    <>
                      <Calculator className="w-3.5 h-3.5" />
                      Run Idempotent Generation
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* =========================================================================
          MODAL: Payment Notice (Phase 6 Integration Notice)
         ========================================================================= */}
      {showPaymentModal && selectedBillForPayment && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-2xl border border-slate-200 max-w-md w-full p-6 animate-in fade-in zoom-in-95 text-center">
            <div className="w-12 h-12 mx-auto rounded-2xl bg-teal-50 border border-teal-200 flex items-center justify-center text-teal-600 mb-3">
              <CreditCard className="w-6 h-6" />
            </div>

            <h3 className="text-base font-bold text-slate-900">Payment Gateway Integration</h3>
            <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
              Simulated payment gateway and test payment integration will be unlocked in the upcoming module.
            </p>

            <div className="my-4 p-4 rounded-xl bg-slate-50 border border-slate-100 text-left text-xs font-mono">
              <div className="flex justify-between py-1 border-b border-slate-200/60">
                <span className="text-slate-400">Invoice:</span>
                <span className="font-semibold text-slate-800">{selectedBillForPayment.billingMonth}</span>
              </div>
              <div className="flex justify-between py-1 border-b border-slate-200/60">
                <span className="text-slate-400">Flat:</span>
                <span className="font-semibold text-slate-800">{selectedBillForPayment.towerName} #{selectedBillForPayment.flatNumber}</span>
              </div>
              <div className="flex justify-between py-1 border-b border-slate-200/60">
                <span className="text-slate-400">Base Calculation:</span>
                <span className="text-slate-800">{selectedBillForPayment.flatAreaSqFt} sq.ft @ ₹{selectedBillForPayment.ratePerSqFt}</span>
              </div>
              <div className="flex justify-between pt-1">
                <span className="text-slate-500 font-bold">Outstanding Due:</span>
                <span className="text-teal-700 font-bold text-sm">{formatINR(selectedBillForPayment.outstandingBalance)}</span>
              </div>
            </div>

            <div className="p-3 bg-amber-50 border border-amber-200 rounded-xl text-[11px] text-amber-800 flex items-center gap-2 mb-4 text-left">
              <Info className="w-4 h-4 text-amber-600 shrink-0" />
              <span>Real PostgreSQL financial records are intact. Mock payment actions remain disabled until Phase 6 per project specifications.</span>
            </div>

            <button
              onClick={() => setShowPaymentModal(false)}
              className="w-full py-2.5 px-4 rounded-xl bg-slate-900 hover:bg-slate-800 text-white font-semibold text-xs transition cursor-pointer"
            >
              Acknowledge & Close
            </button>
          </div>
        </div>
      )}

      {/* =========================================================================
          MODAL / DRAWER: Admin Flat Ledger Inspector
         ========================================================================= */}
      {activeLedgerModalFlat && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-2xl border border-slate-200 max-w-3xl w-full p-6 animate-in fade-in zoom-in-95 max-h-[85vh] flex flex-col">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <div>
                <h3 className="font-bold text-slate-900 text-sm">
                  Ledger History: {activeLedgerModalFlat.towerName} #{activeLedgerModalFlat.flatNumber}
                </h3>
                <span className="text-[10px] text-slate-400 font-mono">
                  Flat ID: #{activeLedgerModalFlat.flatId} • PostgreSQL Audit Entries
                </span>
              </div>
              <button
                onClick={() => setActiveLedgerModalFlat(null)}
                className="p-1 rounded-lg hover:bg-slate-100 text-slate-400 hover:text-slate-700 transition cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="flex-1 overflow-y-auto mt-4">
              {loadingLedger ? (
                <div className="p-12 text-center text-xs text-slate-400">
                  <RotateCw className="w-6 h-6 mx-auto mb-2 text-teal-600 animate-spin" />
                  Loading transactions...
                </div>
              ) : flatLedger.length === 0 ? (
                <div className="p-12 text-center text-xs text-slate-400">
                  No ledger entries recorded yet.
                </div>
              ) : (
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-50 text-slate-500 font-semibold uppercase tracking-wider text-[10px] border-b border-slate-100">
                    <tr>
                      <th className="px-3 py-2.5">Date</th>
                      <th className="px-3 py-2.5">Type</th>
                      <th className="px-3 py-2.5">Description</th>
                      <th className="px-3 py-2.5 text-right">Debit (+)</th>
                      <th className="px-3 py-2.5 text-right">Credit (-)</th>
                      <th className="px-3 py-2.5 text-right">Balance</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 font-mono text-[11px]">
                    {flatLedger.map((entry) => (
                      <tr key={entry.id} className="hover:bg-slate-50/80">
                        <td className="px-3 py-2.5 text-slate-600">
                          {new Date(entry.entryDate).toLocaleDateString('en-IN')}
                        </td>
                        <td className="px-3 py-2.5">
                          <span className="text-[10px] px-1.5 py-0.5 rounded bg-slate-100 font-semibold text-slate-700">
                            {entry.entryType}
                          </span>
                        </td>
                        <td className="px-3 py-2.5 font-sans text-slate-800">
                          {entry.description}
                        </td>
                        <td className="px-3 py-2.5 text-right text-rose-700 font-semibold">
                          {entry.debit > 0 ? formatINR(entry.debit) : '—'}
                        </td>
                        <td className="px-3 py-2.5 text-right text-emerald-700 font-semibold">
                          {entry.credit > 0 ? formatINR(entry.credit) : '—'}
                        </td>
                        <td className="px-3 py-2.5 text-right font-bold text-slate-900">
                          {formatINR(entry.runningBalance)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>

            <div className="pt-3 border-t border-slate-100 flex justify-end">
              <button
                onClick={() => setActiveLedgerModalFlat(null)}
                className="px-4 py-1.5 rounded-xl bg-slate-900 hover:bg-slate-800 text-white font-medium text-xs cursor-pointer"
              >
                Done
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
