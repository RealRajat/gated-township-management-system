import { useState, useEffect, useMemo, useRef } from 'react';
import {
  FileSpreadsheet,
  Upload,
  CheckCircle2,
  AlertTriangle,
  XCircle,
  Download,
  Search,
  Filter,
  RefreshCw,
  HelpCircle,
  FileText,
  ShieldAlert,
  ArrowRight,
  Clock,
  Layers,
  ChevronRight,
  Check,
  X,
  Building2,
  DollarSign,
  AlertCircle,
  Sparkles,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { reconciliationApi, billingApi, townshipApi } from '../services/api';
import StatCard from '../components/StatCard';

export default function BankReconciliationPage() {
  const { user, isHelpDesk } = useAuth();
  const fileInputRef = useRef(null);

  // Data states
  const [transactions, setTransactions] = useState([]);
  const [batches, setBatches] = useState([]);
  const [selectedBatchId, setSelectedBatchId] = useState('');
  const [activeTab, setActiveTab] = useState('ALL'); // 'ALL' | 'MANUAL_REVIEW' | 'EXACT_MATCH' | 'UNMATCHED' | 'RECONCILED'
  const [searchTerm, setSearchTerm] = useState('');

  // UI / Loading states
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  // Review & Matching Drawer / Modal state
  const [selectedTxn, setSelectedTxn] = useState(null);
  const [reviewNotes, setReviewNotes] = useState('');
  const [targetFlatId, setTargetFlatId] = useState('');
  const [targetBillId, setTargetBillId] = useState('');
  const [availableFlats, setAvailableFlats] = useState([]);
  const [approving, setApproving] = useState(false);
  const [reviewError, setReviewError] = useState('');

  // Currency Formatter
  const formatINR = (val) => {
    if (val === null || val === undefined) return '₹0.00';
    const num = Number(val);
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 2,
    }).format(num);
  };

  // Load transactions and batches from PostgreSQL
  const loadData = async () => {
    setLoading(true);
    setErrorMsg('');
    try {
      const [txRes, batchRes] = await Promise.all([
        reconciliationApi.getTransactions({ batchId: selectedBatchId || null }),
        reconciliationApi.getBatches(),
      ]);
      setTransactions(txRes || []);
      setBatches(batchRes || []);
    } catch (err) {
      setErrorMsg(err.message || 'Failed to fetch reconciliation records from server.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (isHelpDesk) {
      loadData();
    }
  }, [isHelpDesk, selectedBatchId]);

  // Load flats for manual selection in review drawer
  useEffect(() => {
    const fetchTowersAndFlats = async () => {
      try {
        const towers = await townshipApi.getTowers();
        if (towers && towers.length > 0) {
          // Flatten first few towers flats for selection convenience
          const flatsList = [];
          for (const t of towers.slice(0, 5)) {
            const flats = await townshipApi.getFlatsByTower(t.id);
            if (flats) {
              flats.forEach((f) => {
                flatsList.push({
                  flatId: f.id,
                  label: `${t.name} • #${f.flatNumber} (${f.superBuiltUpAreaSqFt} sq.ft)`,
                  towerName: t.name,
                  flatNumber: f.flatNumber,
                });
              });
            }
          }
          setAvailableFlats(flatsList);
        }
      } catch (err) {
        console.warn('Could not fetch flats for dropdown:', err);
      }
    };
    if (isHelpDesk) {
      fetchTowersAndFlats();
    }
  }, [isHelpDesk]);

  // Handle CSV file selection and upload
  const handleFileUpload = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (!file.name.toLowerCase().endsWith('.csv')) {
      setErrorMsg('Invalid file format. Please upload a standard comma-separated CSV file (.csv).');
      return;
    }

    if (file.size > 5 * 1024 * 1024) {
      setErrorMsg('File size exceeds the 5MB maximum limit.');
      return;
    }

    setUploading(true);
    setErrorMsg('');
    setSuccessMsg('');

    const formData = new FormData();
    formData.append('file', file);

    try {
      const result = await reconciliationApi.uploadStatement(formData);
      const rowsImported = result.totalRowsImported ?? result.totalRows ?? 0;
      setSuccessMsg(
        `Statement imported successfully! Batch #${result.batchId}: ${rowsImported} rows imported, ` +
        `${result.exactMatchesCount} exact matches, ${result.manualReviewCount} routed to manual review, ` +
        `${result.duplicatesSkippedCount || 0} duplicates skipped.`
      );
      // Reset file input
      if (fileInputRef.current) fileInputRef.current.value = '';
      await loadData();
    } catch (err) {
      setErrorMsg(err.message || 'Failed to upload and reconcile bank CSV.');
    } finally {
      setUploading(false);
    }
  };

  // Download Sample CSV template for user convenience
  const handleDownloadSampleCsv = () => {
    const csvContent =
      'transaction_date,amount,utr_reference,narration,upi_id\n' +
      `2026-11-01,7475.00,PUNBHDFC${Math.floor(100000 + Math.random() * 900000)},NEFT CR-HDFC0000045-NEERAJ KULKARNI T07-1203 PUNE-PUNBHDFC45892,neeraj@okaxis\n` +
      `2026-11-02,6370.00,UPIICICI${Math.floor(100000 + Math.random() * 900000)},UPI-SWATI JOSHI-T12-0408 MAINTENANCE-9822300000@icici,swati@icici\n` +
      `2026-11-03,10660.00,AXISRTGS${Math.floor(100000 + Math.random() * 900000)},RTGS CR-UTIB0000123-AAMIR SHAIKH T21 1601 MAINTENANCE,aamir@hdfcbank\n` +
      `2026-11-04,5000.00,SBIN${Math.floor(100000 + Math.random() * 900000)},IMPS 6124009235 DEPOSIT FOR FLAT MAINTENANCE,resident@sbi\n`;

    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.setAttribute('href', url);
    link.setAttribute('download', `township_bank_statement_sample_${Date.now()}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  // Open transaction review drawer
  const handleOpenReview = (txn) => {
    setSelectedTxn(txn);
    setTargetFlatId(txn.candidateFlatId ? String(txn.candidateFlatId) : '');
    setTargetBillId(txn.candidateBillId ? String(txn.candidateBillId) : '');
    setReviewNotes(txn.reviewNotes || '');
    setReviewError('');
  };

  // Submit Approval / Ledger Posting or Rejection
  const handleApprovalAction = async (action) => {
    if (!selectedTxn) return;

    setApproving(true);
    setReviewError('');

    try {
      const res = await reconciliationApi.approveOrReject({
        bankTransactionId: selectedTxn.id,
        transactionId: selectedTxn.id,
        action,
        targetFlatId: targetFlatId ? Number(targetFlatId) : null,
        targetBillId: targetBillId ? Number(targetBillId) : null,
        notes: reviewNotes,
      });

      setSuccessMsg(
        action === 'APPROVE'
          ? `Transaction ${selectedTxn.utrReference} successfully approved! Flat ledger credited with ${formatINR(res.reconciledAmount || res.amountCredited)}.`
          : `Transaction ${selectedTxn.utrReference} marked as REJECTED.`
      );

      setSelectedTxn(null);
      await loadData();
    } catch (err) {
      setReviewError(err.message || 'Action failed.');
    } finally {
      setApproving(false);
    }
  };

  // Filtered transactions
  const filteredTransactions = useMemo(() => {
    return transactions.filter((tx) => {
      const matchesSearch =
        tx.utrReference.toLowerCase().includes(searchTerm.toLowerCase()) ||
        tx.narration.toLowerCase().includes(searchTerm.toLowerCase()) ||
        (tx.candidateFlatNumber && tx.candidateFlatNumber.toLowerCase().includes(searchTerm.toLowerCase())) ||
        (tx.upiId && tx.upiId.toLowerCase().includes(searchTerm.toLowerCase()));

      let matchesStatus = true;
      if (activeTab === 'MANUAL_REVIEW') {
        matchesStatus = tx.matchStatus === 'MANUAL_REVIEW';
      } else if (activeTab === 'EXACT_MATCH') {
        matchesStatus = tx.matchStatus === 'EXACT_MATCH';
      } else if (activeTab === 'UNMATCHED') {
        matchesStatus = tx.matchStatus === 'UNMATCHED';
      } else if (activeTab === 'RECONCILED') {
        matchesStatus = tx.matchStatus === 'RECONCILED';
      }

      return matchesSearch && matchesStatus;
    });
  }, [transactions, searchTerm, activeTab]);

  // Aggregate KPI Calculations
  const stats = useMemo(() => {
    const totalCredits = transactions.reduce((acc, t) => acc + (Number(t.amount) || 0), 0);
    const exactMatches = transactions.filter((t) => t.matchStatus === 'EXACT_MATCH' || t.matchStatus === 'RECONCILED');
    const matchedAmount = exactMatches.reduce((acc, t) => acc + (Number(t.amount) || 0), 0);
    const manualReviewCount = transactions.filter((t) => t.matchStatus === 'MANUAL_REVIEW').length;
    const unmatchedCount = transactions.filter((t) => t.matchStatus === 'UNMATCHED').length;

    return {
      totalCredits,
      matchedAmount,
      exactMatchesCount: exactMatches.length,
      manualReviewCount,
      unmatchedCount,
    };
  }, [transactions]);

  // Status Badge Component
  const MatchStatusBadge = ({ status }) => {
    switch (status) {
      case 'EXACT_MATCH':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
            <CheckCircle2 className="w-3 h-3 text-emerald-600" />
            EXACT MATCH
          </span>
        );
      case 'RECONCILED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-semibold bg-teal-50 text-teal-700 border border-teal-200">
            <Check className="w-3 h-3 text-teal-600" />
            RECONCILED & POSTED
          </span>
        );
      case 'MANUAL_REVIEW':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-semibold bg-amber-50 text-amber-800 border border-amber-200">
            <AlertTriangle className="w-3 h-3 text-amber-600" />
            MANUAL REVIEW
          </span>
        );
      case 'REJECTED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-semibold bg-slate-100 text-slate-600 border border-slate-200">
            <XCircle className="w-3 h-3 text-slate-500" />
            REJECTED
          </span>
        );
      case 'UNMATCHED':
      default:
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-semibold bg-rose-50 text-rose-700 border border-rose-200">
            <XCircle className="w-3 h-3 text-rose-600" />
            UNMATCHED
          </span>
        );
    }
  };

  // If user is not HELP_DESK, display role boundary banner
  if (!isHelpDesk) {
    return (
      <div className="max-w-2xl mx-auto my-12 bg-white rounded-2xl border border-slate-200 p-8 shadow-sm text-center space-y-4">
        <div className="w-14 h-14 mx-auto rounded-2xl bg-amber-50 border border-amber-200 flex items-center justify-center text-amber-600">
          <ShieldAlert className="w-7 h-7" />
        </div>
        <h2 className="text-xl font-bold text-slate-900">
          Help Desk & Accounts Clearance Required
        </h2>
        <p className="text-xs sm:text-sm text-slate-500 leading-relaxed max-w-lg mx-auto">
          Bank CSV Statement Reconciliation is restricted to the Facility Management Accounts Office (<code className="font-mono font-semibold">ROLE_HELP_DESK</code>). Residents can review individual flat statements and receipts under the <span className="font-semibold text-teal-700">Maintenance & Payments</span> pages.
        </p>
        <div className="p-3 bg-slate-50 rounded-xl border border-slate-100 text-xs font-mono text-slate-600 max-w-md mx-auto">
          Logged in as: <span className="font-semibold text-slate-800">{user?.fullName || user?.identifier}</span> ({user?.role})
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Top Test Mode / Reconciliation Watermark */}
      <div className="p-3 bg-teal-500/10 border border-teal-500/30 rounded-2xl flex items-center justify-between text-xs text-teal-950">
        <div className="flex items-center gap-2.5">
          <span className="px-2 py-0.5 rounded-md bg-teal-700 text-white font-mono font-bold text-[10px] tracking-wider uppercase">
            Bank Reconciliation v1.0
          </span>
          <span className="font-medium">
            Deterministic statement matching engine with zero double-counting enforcement and manual audit approval.
          </span>
        </div>
        <span className="text-[11px] font-mono text-teal-800 hidden sm:inline">
          PostgreSQL 16 Engine
        </span>
      </div>

      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="text-xs font-semibold uppercase tracking-wider text-teal-600 font-mono">
              Phase 6 • Bank Reconciliation Engine
            </span>
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
          </div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight mt-0.5">
            Bank CSV Payment Reconciliation
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 mt-0.5">
            Automated statement matching for NEFT, IMPS, UPI, and RTGS direct deposits into society bank accounts.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={handleDownloadSampleCsv}
            className="inline-flex items-center gap-1.5 px-3.5 py-2 bg-white hover:bg-slate-50 text-slate-700 border border-slate-200 rounded-xl text-xs font-semibold shadow-xs transition cursor-pointer"
          >
            <Download className="w-4 h-4 text-slate-500" />
            Download Sample CSV
          </button>

          <button
            onClick={loadData}
            disabled={loading}
            className="p-2 rounded-xl bg-white border border-slate-200 hover:bg-slate-50 text-slate-600 transition cursor-pointer"
            title="Refresh transactions"
          >
            <RotateCw className={`w-4 h-4 ${loading ? 'animate-spin text-teal-600' : ''}`} />
          </button>
        </div>
      </div>

      {/* Success Notification */}
      {successMsg && (
        <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs flex items-center justify-between animate-in fade-in">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span className="font-semibold">{successMsg}</span>
          </div>
          <button
            onClick={() => setSuccessMsg('')}
            className="text-emerald-700 hover:text-emerald-900 text-xs"
          >
            <X className="w-3.5 h-3.5" />
          </button>
        </div>
      )}

      {/* Error Alert */}
      {errorMsg && (
        <div className="p-4 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 text-xs flex items-center justify-between">
          <div className="flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 text-rose-600 shrink-0" />
            <span>{errorMsg}</span>
          </div>
          <button
            onClick={loadData}
            className="text-xs font-semibold underline hover:text-rose-900"
          >
            Retry
          </button>
        </div>
      )}

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Statement Credits"
          value={formatINR(stats.totalCredits)}
          subtext={`${transactions.length} Imported line items`}
          icon={FileSpreadsheet}
          colorScheme="teal"
          trend="neutral"
          trendLabel="All Batches"
        />
        <StatCard
          title="Auto-Matched & Posted"
          value={formatINR(stats.matchedAmount)}
          subtext={`${stats.exactMatchesCount} Invoices reconciled`}
          icon={CheckCircle2}
          colorScheme="emerald"
          trend="up"
          trendLabel="Exact UTR / Token"
        />
        <StatCard
          title="Manual Review Queue"
          value={`${stats.manualReviewCount} Items`}
          subtext="Ambiguous flat deposits"
          icon={AlertTriangle}
          colorScheme="amber"
          trend="down"
          trendLabel="Needs Action"
        />
        <StatCard
          title="Unidentified Credits"
          value={`${stats.unmatchedCount} Items`}
          subtext="No flat or bill reference"
          icon={XCircle}
          colorScheme="rose"
          trend="neutral"
          trendLabel="Unmatched"
        />
      </div>

      {/* CSV Upload Dropzone */}
      <div className="bg-white rounded-2xl border-2 border-dashed border-slate-300 p-6 text-center hover:border-teal-500 transition shadow-xs">
        <div className="max-w-md mx-auto">
          <div className="w-12 h-12 rounded-2xl bg-teal-50 text-teal-600 mx-auto flex items-center justify-center mb-3">
            <Upload className="w-6 h-6" />
          </div>
          <h3 className="text-sm font-bold text-slate-800">
            Upload Bank Statement CSV (HDFC / ICICI / SBI / Axis)
          </h3>
          <p className="text-xs text-slate-500 mt-1 leading-relaxed">
            The deterministic engine parses UTR, transaction date, credit amount, and narration tokens to match flat maintenance bills.
          </p>

          <input
            type="file"
            ref={fileInputRef}
            onChange={handleFileUpload}
            accept=".csv"
            className="hidden"
            id="csv-file-input"
          />

          <div className="mt-4 flex items-center justify-center gap-3">
            <label
              htmlFor="csv-file-input"
              className={`px-4 py-2 bg-slate-900 hover:bg-slate-800 text-white text-xs font-semibold rounded-xl shadow-xs transition cursor-pointer flex items-center gap-2 ${
                uploading ? 'opacity-50 pointer-events-none' : ''
              }`}
            >
              {uploading ? (
                <>
                  <RotateCw className="w-3.5 h-3.5 animate-spin" />
                  Parsing & Matching Statement...
                </>
              ) : (
                <>
                  <Upload className="w-3.5 h-3.5" />
                  Select CSV File to Reconcile
                </>
              )}
            </label>
            <span className="text-[11px] text-slate-400">Supported: .csv (Max 5MB)</span>
          </div>
        </div>
      </div>

      {/* Batches & Filter Selector */}
      <div className="bg-white rounded-xl border border-slate-200/90 shadow-sm overflow-hidden">
        <div className="p-4 sm:p-5 border-b border-slate-100 flex flex-col md:flex-row md:items-center justify-between gap-4">
          {/* Tabs */}
          <div className="flex items-center gap-1 overflow-x-auto pb-1 md:pb-0">
            {[
              { id: 'ALL', label: 'All Records', count: transactions.length },
              { id: 'MANUAL_REVIEW', label: 'Manual Review Queue', count: stats.manualReviewCount, alert: stats.manualReviewCount > 0 },
              { id: 'EXACT_MATCH', label: 'Exact Matches', count: stats.exactMatchesCount },
              { id: 'RECONCILED', label: 'Reconciled & Posted', count: transactions.filter((t) => t.matchStatus === 'RECONCILED').length },
              { id: 'UNMATCHED', label: 'Unmatched', count: stats.unmatchedCount },
            ].map((tab) => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition cursor-pointer flex items-center gap-1.5 whitespace-nowrap ${
                  activeTab === tab.id
                    ? 'bg-slate-900 text-white shadow-xs'
                    : 'text-slate-600 hover:bg-slate-100'
                }`}
              >
                <span>{tab.label}</span>
                <span
                  className={`px-1.5 py-0.2 rounded-full text-[10px] font-mono ${
                    activeTab === tab.id
                      ? 'bg-slate-800 text-slate-200'
                      : tab.alert
                      ? 'bg-amber-100 text-amber-800 font-bold'
                      : 'bg-slate-100 text-slate-600'
                  }`}
                >
                  {tab.count}
                </span>
              </button>
            ))}
          </div>

          {/* Search & Batch Dropdown */}
          <div className="flex items-center gap-2.5">
            <div className="relative">
              <Search className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                placeholder="Search UTR, flat, narration..."
                className="w-48 sm:w-56 pl-8 pr-3 py-1.5 bg-slate-50 border border-slate-200 text-xs rounded-lg focus:outline-none focus:border-teal-500"
              />
            </div>

            {batches.length > 0 && (
              <select
                value={selectedBatchId}
                onChange={(e) => setSelectedBatchId(e.target.value)}
                className="py-1.5 px-2 bg-slate-50 border border-slate-200 text-xs rounded-lg text-slate-700 font-medium focus:outline-none focus:border-teal-500"
              >
                <option value="">All Batches ({batches.length})</option>
                {batches.map((b) => (
                  <option key={b.id} value={b.id}>
                    Batch #{b.id} ({b.filename || b.fileName || b.batchId})
                  </option>
                ))}
              </select>
            )}
          </div>
        </div>

        {/* Transactions Table */}
        {loading ? (
          <div className="p-12 text-center text-xs text-slate-400">
            <RotateCw className="w-6 h-6 mx-auto mb-2 text-teal-600 animate-spin" />
            Loading bank transactions from PostgreSQL...
          </div>
        ) : filteredTransactions.length === 0 ? (
          <div className="p-12 text-center text-xs text-slate-400">
            <FileSpreadsheet className="w-8 h-8 mx-auto mb-2 text-slate-300" />
            No statement transactions found in this view. Upload a bank statement CSV to begin.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 text-slate-500 font-semibold uppercase tracking-wider text-[10px] border-b border-slate-100">
                <tr>
                  <th className="px-4 py-3">Bank UTR / Ref</th>
                  <th className="px-3 py-3">Date</th>
                  <th className="px-4 py-3">Bank Narration String</th>
                  <th className="px-3 py-3 text-right">Credit Amount</th>
                  <th className="px-3 py-3">Candidate Flat</th>
                  <th className="px-3 py-3 text-center">Confidence</th>
                  <th className="px-4 py-3 text-center">Status</th>
                  <th className="px-3 py-3 text-center">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-mono text-[11px]">
                {filteredTransactions.map((tx) => (
                  <tr key={tx.id} className="hover:bg-slate-50/80 transition">
                    <td className="px-4 py-3 font-bold text-slate-800">
                      <div className="truncate max-w-[130px]" title={tx.utrReference}>
                        {tx.utrReference}
                      </div>
                      {tx.upiId && (
                        <span className="text-[9px] text-slate-400 font-sans block truncate max-w-[120px]">
                          {tx.upiId}
                        </span>
                      )}
                    </td>
                    <td className="px-3 py-3 text-slate-500 text-[10px] whitespace-nowrap">
                      {tx.transactionDate}
                    </td>
                    <td className="px-4 py-3 max-w-[240px] truncate text-slate-600 text-[10px]" title={tx.narration}>
                      {tx.narration}
                    </td>
                    <td className="px-3 py-3 text-right font-bold text-slate-900 whitespace-nowrap">
                      {formatINR(tx.amount)}
                    </td>
                    <td className="px-3 py-3 font-sans">
                      {tx.candidateFlatNumber ? (
                        <div>
                          <span className="font-semibold text-slate-800">
                            {tx.candidateTowerName} • #{tx.candidateFlatNumber}
                          </span>
                          <span className="text-[10px] text-slate-400 block font-mono">
                            Flat ID #{tx.candidateFlatId}
                          </span>
                        </div>
                      ) : (
                        <span className="text-[10px] text-slate-400 italic">Unassigned</span>
                      )}
                    </td>
                    <td className="px-3 py-3 text-center font-bold text-teal-700">
                      {tx.confidenceScore ? `${tx.confidenceScore}%` : '—'}
                    </td>
                    <td className="px-4 py-3 text-center">
                      <MatchStatusBadge status={tx.matchStatus} />
                    </td>
                    <td className="px-3 py-3 text-center">
                      <button
                        onClick={() => handleOpenReview(tx)}
                        className={`px-2.5 py-1 rounded-lg text-xs font-semibold font-sans transition cursor-pointer inline-flex items-center gap-1 ${
                          tx.matchStatus === 'MANUAL_REVIEW'
                            ? 'bg-amber-100 hover:bg-amber-200 text-amber-900 font-bold'
                            : 'bg-slate-100 hover:bg-slate-200 text-slate-700'
                        }`}
                      >
                        {tx.matchStatus === 'RECONCILED' ? 'Details' : 'Review & Match'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        <div className="p-4 bg-slate-50 border-t border-slate-100 flex flex-col sm:flex-row items-center justify-between text-xs text-slate-500 gap-2">
          <span>
            Deterministic rules: Exact UTR (100%), Flat Token + Exact Amount (95%), UPI ID (90%).
          </span>
          <span className="font-medium text-slate-700">
            Immutable Double-Counting Guard Active
          </span>
        </div>
      </div>

      {/* =========================================================================
          DRAWER / MODAL: Review & Match Bank Transaction
         ========================================================================= */}
      {selectedTxn && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-2xl border border-slate-200 max-w-lg w-full p-6 animate-in fade-in zoom-in-95 max-h-[90vh] flex flex-col">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <div className="flex items-center gap-2">
                <div className="p-2 bg-teal-50 rounded-xl text-teal-600">
                  <FileSpreadsheet className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="font-bold text-slate-900 text-sm">
                    Reconciliation Review: {selectedTxn.utrReference}
                  </h3>
                  <span className="text-[10px] text-slate-400 font-mono">
                    PostgreSQL Bank Line Item #{selectedTxn.id}
                  </span>
                </div>
              </div>
              <button
                onClick={() => setSelectedTxn(null)}
                className="p-1 rounded-lg hover:bg-slate-100 text-slate-400 hover:text-slate-700 transition cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <div className="flex-1 overflow-y-auto my-4 space-y-4 text-xs font-sans">
              {/* Transaction Metadata Card */}
              <div className="p-3.5 bg-slate-50 rounded-xl border border-slate-200 space-y-2">
                <div className="flex justify-between">
                  <span className="text-slate-500">Deposit Amount:</span>
                  <span className="text-sm font-bold text-emerald-700 font-mono">
                    {formatINR(selectedTxn.amount)}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500">Date:</span>
                  <span className="font-mono text-slate-800">{selectedTxn.transactionDate}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500">Match Status:</span>
                  <MatchStatusBadge status={selectedTxn.matchStatus} />
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500">Match Confidence:</span>
                  <span className="font-mono font-bold text-teal-700">
                    {selectedTxn.confidenceScore ? `${selectedTxn.confidenceScore}%` : 'N/A'}
                  </span>
                </div>
                {selectedTxn.matchRuleApplied && (
                  <div className="flex justify-between">
                    <span className="text-slate-500">Rule Applied:</span>
                    <span className="font-mono text-[10px] text-slate-700">
                      {selectedTxn.matchRuleApplied}
                    </span>
                  </div>
                )}
              </div>

              {/* Bank Narration String */}
              <div>
                <label className="block text-slate-700 font-semibold mb-1">
                  Raw Bank Statement Narration:
                </label>
                <div className="p-3 bg-slate-100 rounded-xl font-mono text-[11px] text-slate-800 break-all select-all border border-slate-200">
                  {selectedTxn.narration}
                </div>
              </div>

              {/* Target Flat Selection for Manual Matching */}
              <div>
                <label className="block text-slate-700 font-semibold mb-1">
                  Assigned Apartment / Flat
                </label>
                {selectedTxn.matchStatus === 'RECONCILED' ? (
                  <div className="p-2.5 bg-emerald-50 text-emerald-900 rounded-xl font-semibold border border-emerald-200">
                    {selectedTxn.candidateTowerName} • #{selectedTxn.candidateFlatNumber} (Flat ID #{selectedTxn.candidateFlatId})
                  </div>
                ) : (
                  <div className="space-y-2">
                    <select
                      value={targetFlatId}
                      onChange={(e) => setTargetFlatId(e.target.value)}
                      className="w-full p-2 bg-slate-50 border border-slate-200 rounded-xl text-xs font-semibold focus:outline-none focus:border-teal-500"
                    >
                      <option value="">-- Choose Flat to Credit --</option>
                      {availableFlats.map((fl) => (
                        <option key={fl.flatId} value={fl.flatId}>
                          {fl.label}
                        </option>
                      ))}
                    </select>

                    <div className="flex items-center gap-2">
                      <span className="text-slate-500 text-[11px]">Or Flat ID:</span>
                      <input
                        type="number"
                        placeholder="e.g. 1"
                        value={targetFlatId}
                        onChange={(e) => setTargetFlatId(e.target.value)}
                        className="w-24 px-2 py-1 bg-slate-50 border border-slate-200 rounded-lg text-xs font-mono"
                      />
                    </div>
                  </div>
                )}
              </div>

              {/* Internal Audit Notes */}
              <div>
                <label className="block text-slate-700 font-semibold mb-1">
                  Auditor Verification Notes
                </label>
                <textarea
                  rows="2"
                  value={reviewNotes}
                  onChange={(e) => setReviewNotes(e.target.value)}
                  placeholder="e.g. Verified resident deposit via HDFC Corporate bank portal, credit applied against monthly invoice."
                  disabled={selectedTxn.matchStatus === 'RECONCILED'}
                  className="w-full p-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs focus:outline-none focus:border-teal-500"
                />
              </div>

              {/* Error Alert */}
              {reviewError && (
                <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl text-rose-800 text-xs flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
                  <span>{reviewError}</span>
                </div>
              )}
            </div>

            {/* Action Buttons */}
            <div className="flex items-center justify-between pt-3 border-t border-slate-100">
              <button
                type="button"
                onClick={() => setSelectedTxn(null)}
                className="px-4 py-2 rounded-xl border border-slate-200 text-slate-600 hover:bg-slate-50 font-medium text-xs cursor-pointer"
              >
                Close
              </button>

              {selectedTxn.matchStatus !== 'RECONCILED' && (
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => handleApprovalAction('REJECT')}
                    disabled={approving}
                    className="px-3.5 py-2 rounded-xl border border-rose-200 text-rose-700 hover:bg-rose-50 font-semibold text-xs cursor-pointer disabled:opacity-50"
                  >
                    Reject Line Item
                  </button>

                  <button
                    type="button"
                    onClick={() => handleApprovalAction('APPROVE')}
                    disabled={approving || !targetFlatId}
                    className="px-4 py-2 rounded-xl bg-teal-600 hover:bg-teal-700 text-white font-semibold text-xs transition cursor-pointer flex items-center gap-1.5 shadow-sm disabled:opacity-50"
                  >
                    {approving ? (
                      <>
                        <RotateCw className="w-3.5 h-3.5 animate-spin" />
                        Posting to Flat Ledger...
                      </>
                    ) : (
                      <>
                        <Check className="w-3.5 h-3.5" />
                        Approve & Post to Ledger
                      </>
                    )}
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}