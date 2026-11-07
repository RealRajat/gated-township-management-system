import { useState, useEffect, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  CreditCard,
  Search,
  Filter,
  Download,
  Receipt,
  CheckCircle2,
  Clock,
  XCircle,
  ExternalLink,
  ArrowDownRight,
  TrendingUp,
  AlertTriangle,
  RotateCw,
  ShieldCheck,
  Building2,
  DollarSign,
  Printer,
  X,
  Sparkles,
  Info,
  Check,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { paymentApi, billingApi } from '../services/api';
import StatCard from '../components/StatCard';

export default function PaymentsPage() {
  const { user, isHelpDesk } = useAuth();
  const [searchParams] = useSearchParams();
  const preselectedBillId = searchParams.get('billId');

  // Loading & error states
  const [loading, setLoading] = useState(true);
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  // Data states
  const [payments, setPayments] = useState([]);
  const [residentBills, setResidentBills] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedMode, setSelectedMode] = useState('ALL');
  const [selectedStatus, setSelectedStatus] = useState('ALL');

  // Test Mode Payment Modal State
  const [showPaymentModal, setShowPaymentModal] = useState(false);
  const [selectedBill, setSelectedBill] = useState(null);
  const [paymentAmountType, setPaymentAmountType] = useState('FULL'); // 'FULL' | 'CUSTOM'
  const [customAmount, setCustomAmount] = useState('');
  const [paymentMethod, setPaymentMethod] = useState('UPI_MOCK');
  const [simulateFailure, setSimulateFailure] = useState(false);
  const [failureReason, setFailureReason] = useState('INSUFFICIENT_FUNDS');
  const [processingPayment, setProcessingPayment] = useState(false);
  const [paymentError, setPaymentError] = useState('');

  // Receipt Modal State
  const [showReceiptModal, setShowReceiptModal] = useState(false);
  const [receiptData, setReceiptData] = useState(null);
  const [loadingReceipt, setLoadingReceipt] = useState(false);

  // Currency formatter
  const formatINR = (val) => {
    if (val === null || val === undefined) return '₹0.00';
    const num = Number(val);
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 2,
    }).format(num);
  };

  // Load live data
  const loadData = async () => {
    setLoading(true);
    setErrorMsg('');
    try {
      if (isHelpDesk) {
        // Help Desk: load all payments across township
        const res = await paymentApi.getAllPayments({ status: selectedStatus !== 'ALL' ? selectedStatus : null });
        setPayments(res.content || res || []);
      } else {
        // Resident: load my payment history and my outstanding bills
        const [historyRes, billsRes] = await Promise.all([
          paymentApi.getMyHistory(),
          billingApi.getMyBills(),
        ]);
        setPayments(historyRes || []);
        setResidentBills(billsRes?.bills || []);

        // If URL had ?billId, auto-open payment modal for that bill
        if (preselectedBillId && billsRes?.bills) {
          const target = billsRes.bills.find((b) => String(b.id) === String(preselectedBillId));
          if (target && target.outstandingBalance > 0) {
            setSelectedBill(target);
            setShowPaymentModal(true);
          }
        }
      }
    } catch (err) {
      setErrorMsg(err.message || 'Failed to load payment transactions from server.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [isHelpDesk, selectedStatus]);

  // Outstanding bills that can be paid
  const unpaidBills = useMemo(() => {
    return residentBills.filter((b) => b.outstandingBalance > 0);
  }, [residentBills]);

  // Open payment modal for a specific bill
  const handleInitiatePayment = (bill) => {
    setSelectedBill(bill);
    setPaymentAmountType('FULL');
    setCustomAmount(String(bill.outstandingBalance));
    setPaymentMethod('UPI_MOCK');
    setSimulateFailure(false);
    setPaymentError('');
    setShowPaymentModal(true);
  };

  // Execute Test Mode Payment
  const handleSubmitTestPayment = async (e) => {
    e.preventDefault();
    if (!selectedBill) return;

    setProcessingPayment(true);
    setPaymentError('');

    const targetAmount =
      paymentAmountType === 'FULL'
        ? Number(selectedBill.outstandingBalance)
        : Number(customAmount);

    if (isNaN(targetAmount) || targetAmount <= 0) {
      setPaymentError('Payment amount must be greater than zero.');
      setProcessingPayment(false);
      return;
    }

    if (targetAmount > Number(selectedBill.outstandingBalance)) {
      setPaymentError(
        `Amount cannot exceed outstanding balance of ${formatINR(selectedBill.outstandingBalance)}.`
      );
      setProcessingPayment(false);
      return;
    }

    const payload = {
      billId: selectedBill.id,
      amount: targetAmount,
      paymentMethod,
      idempotencyKey: `FE-${Date.now()}-${Math.random().toString(36).substring(2, 8)}`,
      simulateFailure,
      failureReason: simulateFailure ? failureReason : null,
    };

    try {
      const receipt = await paymentApi.testPay(payload);
      setShowPaymentModal(false);
      setReceiptData(receipt);
      setShowReceiptModal(true);
      setSuccessMsg(`Test payment of ${formatINR(targetAmount)} processed successfully!`);
      // Reload balances and history
      await loadData();
    } catch (err) {
      setPaymentError(err.message || 'Test payment processing failed.');
    } finally {
      setProcessingPayment(false);
    }
  };

  // View Receipt for an existing transaction
  const handleViewReceipt = async (paymentId) => {
    setLoadingReceipt(true);
    try {
      const receipt = await paymentApi.getReceipt(paymentId);
      setReceiptData(receipt);
      setShowReceiptModal(true);
    } catch (err) {
      alert(`Could not fetch receipt: ${err.message}`);
    } finally {
      setLoadingReceipt(false);
    }
  };

  // Print Receipt
  const handlePrintReceipt = () => {
    window.print();
  };

  // Filtered payments list
  const filteredPayments = useMemo(() => {
    return payments.filter((txn) => {
      const search = searchTerm.toLowerCase();
      const matchesSearch =
        (txn.residentName && txn.residentName.toLowerCase().includes(search)) ||
        (txn.flatNumber && String(txn.flatNumber).toLowerCase().includes(search)) ||
        (txn.towerName && txn.towerName.toLowerCase().includes(search)) ||
        (txn.referenceNumber && txn.referenceNumber.toLowerCase().includes(search)) ||
        (txn.receiptNumber && txn.receiptNumber.toLowerCase().includes(search));

      const matchesMode =
        selectedMode === 'ALL' ||
        (txn.paymentMethod && txn.paymentMethod.toUpperCase().includes(selectedMode));

      const matchesStatus =
        selectedStatus === 'ALL' ||
        (txn.status && txn.status.toUpperCase() === selectedStatus);

      return matchesSearch && matchesMode && matchesStatus;
    });
  }, [payments, searchTerm, selectedMode, selectedStatus]);

  // Aggregate KPI Calculations
  const stats = useMemo(() => {
    const successPayments = payments.filter((p) => p.status === 'SUCCESS');
    const totalCollected = successPayments.reduce(
      (sum, p) => sum + (Number(p.amount) || 0),
      0
    );
    const count = successPayments.length;
    const failedCount = payments.filter((p) => p.status === 'FAILED').length;

    return {
      totalCollected,
      count,
      failedCount,
      residentOutstanding: unpaidBills.reduce(
        (sum, b) => sum + (Number(b.outstandingBalance) || 0),
        0
      ),
    };
  }, [payments, unpaidBills]);

  return (
    <div className="space-y-6">
      {/* Test Mode Top Watermark Badge */}
      <div className="p-3 bg-amber-500/10 border border-amber-500/30 rounded-2xl flex items-center justify-between text-xs text-amber-900">
        <div className="flex items-center gap-2.5">
          <span className="px-2 py-0.5 rounded-md bg-amber-600 text-white font-mono font-bold text-[10px] tracking-wider uppercase">
            Test Mode Active
          </span>
          <span className="font-medium">
            Simulated payment transactions only. Zero real funds charged. All payments post immutable credit entries into PostgreSQL flat ledgers.
          </span>
        </div>
        <span className="text-[11px] font-mono text-amber-700 hidden sm:inline">
          Gateway Mock v1.0
        </span>
      </div>

      {/* Page Title & Context Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="text-xs font-semibold uppercase tracking-wider text-teal-600 font-mono">
              Phase 6 • Payment System & Auditing
            </span>
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
          </div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight mt-0.5">
            {isHelpDesk ? 'Township Payment Gateway Transactions' : 'My Maintenance Payments & Receipts'}
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 mt-0.5">
            {isHelpDesk
              ? 'Real-time auditing of test payments, verified UTR receipts, and double-entry ledger postings across 28 towers.'
              : 'Pay outstanding maintenance bills in test mode with full or partial amounts, and download GST receipts.'}
          </p>
        </div>

        <div className="flex items-center gap-2">
          {!isHelpDesk && unpaidBills.length > 0 && (
            <button
              onClick={() => handleInitiatePayment(unpaidBills[0])}
              className="inline-flex items-center gap-1.5 px-4 py-2 bg-teal-600 hover:bg-teal-700 text-white rounded-xl text-xs font-semibold shadow-sm transition cursor-pointer"
            >
              <CreditCard className="w-4 h-4" />
              Pay Bill (Test Mode)
            </button>
          )}

          <button
            onClick={loadData}
            disabled={loading}
            className="p-2 rounded-xl bg-white border border-slate-200 hover:bg-slate-50 text-slate-600 transition cursor-pointer"
            title="Refresh payments"
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
        {isHelpDesk ? (
          <>
            <StatCard
              title="Township Collections"
              value={formatINR(stats.totalCollected)}
              subtext="Verified in PostgreSQL"
              icon={CreditCard}
              colorScheme="teal"
              trend="up"
              trendLabel="Test Mode Total"
            />
            <StatCard
              title="Verified Receipts"
              value={`${stats.count} Receipts`}
              subtext="Audited credit entries"
              icon={Receipt}
              colorScheme="emerald"
              trend="up"
              trendLabel="100% Reconciled"
            />
            <StatCard
              title="Simulated Declines"
              value={`${stats.failedCount} Failed`}
              subtext="Rejected or simulated error"
              icon={XCircle}
              colorScheme="rose"
              trend="down"
              trendLabel="Declined"
            />
            <StatCard
              title="Payment Rails"
              value="UPI / NEFT / Cards"
              subtext="Instant Mock Settlement"
              icon={TrendingUp}
              colorScheme="sky"
              trend="neutral"
              trendLabel="Multi-Rail"
            />
          </>
        ) : (
          <>
            <StatCard
              title="Total Paid by You"
              value={formatINR(stats.totalCollected)}
              subtext={`${stats.count} Test receipts issued`}
              icon={CheckCircle2}
              colorScheme="emerald"
              trend="up"
              trendLabel="Verified"
            />
            <StatCard
              title="Outstanding Dues"
              value={formatINR(stats.residentOutstanding)}
              subtext={unpaidBills.length > 0 ? `${unpaidBills.length} Bill(s) awaiting payment` : 'Account is fully settled'}
              icon={DollarSign}
              colorScheme={stats.residentOutstanding > 0 ? 'amber' : 'emerald'}
              trend={stats.residentOutstanding > 0 ? 'down' : 'up'}
              trendLabel={stats.residentOutstanding > 0 ? 'Action Needed' : 'Zero Dues'}
            />
            <StatCard
              title="Payment Mode"
              value="Test Gateway"
              subtext="Instant Mock Verification"
              icon={ShieldCheck}
              colorScheme="teal"
              trend="neutral"
              trendLabel="Safe Sandbox"
            />
            <StatCard
              title="Receipts Available"
              value={`${stats.count} Downloadable`}
              subtext="Audited GST format"
              icon={Receipt}
              colorScheme="sky"
              trend="neutral"
              trendLabel="Instant PDF"
            />
          </>
        )}
      </div>

      {/* RESIDENT ONLY: Outstanding Invoices Section */}
      {!isHelpDesk && unpaidBills.length > 0 && (
        <div className="bg-white rounded-2xl border border-amber-200/80 p-5 shadow-sm space-y-4 bg-gradient-to-br from-amber-50/40 to-white">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <div className="p-2 rounded-xl bg-amber-100 text-amber-800">
                <Receipt className="w-4 h-4" />
              </div>
              <div>
                <h2 className="text-sm font-bold text-slate-900">
                  Pending Maintenance Dues ({unpaidBills.length})
                </h2>
                <p className="text-xs text-slate-500">
                  Select a bill to initiate a simulated payment in test mode.
                </p>
              </div>
            </div>
            <span className="text-xs font-mono font-bold text-amber-800 bg-amber-100 px-3 py-1 rounded-full">
              Total Outstanding: {formatINR(stats.residentOutstanding)}
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {unpaidBills.map((bill) => (
              <div
                key={bill.id}
                className="p-4 rounded-xl border border-slate-200 bg-white shadow-xs hover:border-teal-400 transition flex flex-col justify-between"
              >
                <div>
                  <div className="flex items-center justify-between text-xs text-slate-500 mb-2">
                    <span className="font-bold text-slate-800 font-mono text-sm">
                      {bill.billingMonth}
                    </span>
                    <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-amber-100 text-amber-800">
                      {bill.status}
                    </span>
                  </div>

                  <div className="text-xs space-y-1 text-slate-600 font-sans">
                    <div className="font-semibold text-slate-900">
                      {bill.towerName} • Unit #{bill.flatNumber}
                    </div>
                    <div>Area: {bill.flatAreaSqFt} sq.ft @ ₹{bill.ratePerSqFt}</div>
                    <div className="text-slate-400 text-[11px]">Due Date: {bill.dueDate}</div>
                  </div>

                  <div className="mt-3 pt-3 border-t border-slate-100 flex items-baseline justify-between">
                    <span className="text-xs text-slate-500">Outstanding:</span>
                    <span className="text-base font-bold text-teal-700 font-mono">
                      {formatINR(bill.outstandingBalance)}
                    </span>
                  </div>
                </div>

                <button
                  onClick={() => handleInitiatePayment(bill)}
                  className="mt-4 w-full py-2 px-3 bg-teal-600 hover:bg-teal-700 text-white rounded-xl text-xs font-semibold shadow-xs transition flex items-center justify-center gap-1.5 cursor-pointer"
                >
                  <CreditCard className="w-3.5 h-3.5" />
                  Pay Now (Test Mode)
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Transactions Table with Search & Filter */}
      <div className="bg-white rounded-xl border border-slate-200/90 shadow-sm overflow-hidden">
        <div className="p-4 sm:p-5 border-b border-slate-100 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <span className="text-sm font-bold text-slate-900 tracking-tight">
              {isHelpDesk ? 'All Township Payment Transactions' : 'Your Payment History & Receipts'}
            </span>
            <span className="text-xs text-slate-400 ml-2">({filteredPayments.length} records)</span>
          </div>

          <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
            <div className="relative">
              <Search className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                placeholder="Search ref, receipt, flat, or resident..."
                className="w-full sm:w-64 pl-8 pr-3 py-1.5 bg-slate-50 border border-slate-200 text-xs rounded-lg focus:outline-none focus:border-teal-500 focus:bg-white transition"
              />
            </div>

            <div className="flex items-center gap-1 bg-slate-100 p-1 rounded-lg">
              {['ALL', 'UPI', 'CARD', 'NET_BANKING'].map((m) => (
                <button
                  key={m}
                  onClick={() => setSelectedMode(m)}
                  className={`px-2.5 py-1 text-[11px] font-medium rounded-md transition cursor-pointer ${
                    selectedMode === m ? 'bg-white text-slate-900 shadow-sm font-semibold' : 'text-slate-500 hover:text-slate-900'
                  }`}
                >
                  {m === 'ALL' ? 'All Rails' : m.replace('_', ' ')}
                </button>
              ))}
            </div>

            <select
              value={selectedStatus}
              onChange={(e) => setSelectedStatus(e.target.value)}
              className="py-1 px-2.5 bg-slate-50 border border-slate-200 text-xs rounded-lg focus:outline-none focus:border-teal-500 font-medium cursor-pointer"
            >
              <option value="ALL">All Status</option>
              <option value="SUCCESS">SUCCESS</option>
              <option value="FAILED">FAILED</option>
            </select>
          </div>
        </div>

        {loading ? (
          <div className="p-12 text-center text-xs text-slate-400">
            <RotateCw className="w-6 h-6 mx-auto mb-2 text-teal-600 animate-spin" />
            Loading verified payment records from PostgreSQL...
          </div>
        ) : filteredPayments.length === 0 ? (
          <div className="p-12 text-center text-xs text-slate-400">
            <Receipt className="w-8 h-8 mx-auto mb-2 text-slate-300" />
            No payment transactions found matching your criteria.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 text-slate-500 font-semibold uppercase tracking-wider text-[10px] border-b border-slate-100">
                <tr>
                  <th className="px-4 py-3">Receipt #</th>
                  <th className="px-4 py-3">Reference ID</th>
                  <th className="px-4 py-3">Flat & Tower</th>
                  <th className="px-3 py-3">Bill Period</th>
                  <th className="px-3 py-3">Payment Mode</th>
                  <th className="px-3 py-3">Timestamp</th>
                  <th className="px-3 py-3 text-right">Amount</th>
                  <th className="px-4 py-3 text-center">Status</th>
                  <th className="px-3 py-3 text-center">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 font-mono text-[11px]">
                {filteredPayments.map((txn) => (
                  <tr key={txn.id} className="hover:bg-slate-50/80 transition">
                    <td className="px-4 py-3 font-bold text-teal-700">
                      {txn.receiptNumber || '—'}
                    </td>
                    <td className="px-4 py-3 text-slate-700 font-medium">
                      <div className="truncate max-w-[150px]" title={txn.referenceNumber}>
                        {txn.referenceNumber}
                      </div>
                      <span className="text-[9px] text-amber-700 bg-amber-50 px-1 py-0.2 rounded border border-amber-200">
                        TEST MODE
                      </span>
                    </td>
                    <td className="px-4 py-3 font-sans">
                      <span className="font-semibold text-slate-800">
                        {txn.towerName} #{txn.flatNumber}
                      </span>
                      {txn.residentName && (
                        <span className="text-[10px] text-slate-400 block truncate max-w-[130px]">
                          {txn.residentName}
                        </span>
                      )}
                    </td>
                    <td className="px-3 py-3 text-slate-700 font-semibold">
                      {txn.billingMonth || '—'}
                    </td>
                    <td className="px-3 py-3">
                      <span className="px-2 py-0.5 rounded text-[10px] font-medium bg-slate-100 text-slate-700 border border-slate-200 font-sans">
                        {txn.paymentMethod?.replace('_', ' ') || 'UPI'}
                      </span>
                    </td>
                    <td className="px-3 py-3 text-slate-400 text-[10px]">
                      {txn.paymentDate
                        ? new Date(txn.paymentDate).toLocaleString('en-IN', {
                            dateStyle: 'short',
                            timeStyle: 'short',
                          })
                        : '—'}
                    </td>
                    <td className="px-3 py-3 text-right font-bold text-slate-900">
                      {formatINR(txn.amount)}
                    </td>
                    <td className="px-4 py-3 text-center">
                      <span
                        className={`inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-semibold font-sans ${
                          txn.status === 'SUCCESS'
                            ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                            : txn.status === 'PENDING'
                            ? 'bg-amber-50 text-amber-700 border border-amber-200'
                            : 'bg-rose-50 text-rose-700 border border-rose-200'
                        }`}
                      >
                        {txn.status}
                      </span>
                    </td>
                    <td className="px-3 py-3 text-center">
                      {txn.status === 'SUCCESS' && (
                        <button
                          onClick={() => handleViewReceipt(txn.id)}
                          className="text-teal-600 hover:text-teal-700 p-1.5 rounded-lg hover:bg-teal-50 transition cursor-pointer inline-flex items-center gap-1 font-sans text-xs font-semibold"
                          title="View Verified Receipt"
                        >
                          <Receipt className="w-3.5 h-3.5" />
                          <span>Receipt</span>
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        <div className="p-4 bg-slate-50 border-t border-slate-100 flex flex-col sm:flex-row items-center justify-between text-xs text-slate-500 gap-2">
          <span>
            Connected to PostgreSQL <code className="font-mono font-semibold">payments</code> table & flat ledgers.
          </span>
          <span className="font-medium text-slate-700">
            Idempotent Mock Payment Gateway v1.0
          </span>
        </div>
      </div>

      {/* =========================================================================
          MODAL: Process Test Mode Payment
         ========================================================================= */}
      {showPaymentModal && selectedBill && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-2xl border border-slate-200 max-w-lg w-full p-6 animate-in fade-in zoom-in-95">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <div className="flex items-center gap-2">
                <div className="p-2 bg-teal-50 rounded-xl text-teal-600">
                  <CreditCard className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="font-bold text-slate-900 text-sm">
                    Simulate Payment (Test Mode)
                  </h3>
                  <span className="text-[10px] text-amber-700 font-mono font-semibold">
                    [TEST GATEWAY — NO REAL MONEY WILL BE CHARGED]
                  </span>
                </div>
              </div>
              <button
                onClick={() => setShowPaymentModal(false)}
                className="p-1 rounded-lg hover:bg-slate-100 text-slate-400 hover:text-slate-700 transition cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handleSubmitTestPayment} className="space-y-4 mt-4 text-xs">
              {/* Bill Details Summary Card */}
              <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200 space-y-1.5 font-sans">
                <div className="flex justify-between text-slate-500">
                  <span>Apartment:</span>
                  <span className="font-bold text-slate-900 font-mono">
                    {selectedBill.towerName} • Unit #{selectedBill.flatNumber}
                  </span>
                </div>
                <div className="flex justify-between text-slate-500">
                  <span>Billing Cycle:</span>
                  <span className="font-semibold text-slate-800 font-mono">
                    {selectedBill.billingMonth}
                  </span>
                </div>
                <div className="flex justify-between text-slate-500">
                  <span>Total Bill Amount:</span>
                  <span className="font-mono text-slate-800">
                    {formatINR(selectedBill.baseAmount)}
                  </span>
                </div>
                <div className="flex justify-between text-slate-500">
                  <span>Previously Paid:</span>
                  <span className="font-mono text-emerald-700">
                    {formatINR(selectedBill.paidAmount)}
                  </span>
                </div>
                <div className="pt-2 border-t border-slate-200 flex justify-between items-baseline">
                  <span className="font-bold text-slate-800">Outstanding Balance:</span>
                  <span className="text-base font-bold text-teal-700 font-mono">
                    {formatINR(selectedBill.outstandingBalance)}
                  </span>
                </div>
              </div>

              {/* Payment Amount Type: Full vs Partial */}
              <div>
                <label className="block text-slate-700 font-semibold mb-1.5">
                  Select Payment Amount
                </label>
                <div className="grid grid-cols-2 gap-2">
                  <button
                    type="button"
                    onClick={() => setPaymentAmountType('FULL')}
                    className={`p-2.5 rounded-xl border text-left transition cursor-pointer ${
                      paymentAmountType === 'FULL'
                        ? 'border-teal-500 bg-teal-50/50 text-teal-900 ring-1 ring-teal-500'
                        : 'border-slate-200 bg-white text-slate-600 hover:border-slate-300'
                    }`}
                  >
                    <div className="font-semibold">Full Balance</div>
                    <div className="font-mono font-bold text-teal-700 text-xs mt-0.5">
                      {formatINR(selectedBill.outstandingBalance)}
                    </div>
                  </button>

                  <button
                    type="button"
                    onClick={() => setPaymentAmountType('CUSTOM')}
                    className={`p-2.5 rounded-xl border text-left transition cursor-pointer ${
                      paymentAmountType === 'CUSTOM'
                        ? 'border-teal-500 bg-teal-50/50 text-teal-900 ring-1 ring-teal-500'
                        : 'border-slate-200 bg-white text-slate-600 hover:border-slate-300'
                    }`}
                  >
                    <div className="font-semibold">Partial Amount</div>
                    <div className="text-[10px] text-slate-400 mt-0.5">Custom installment</div>
                  </button>
                </div>

                {paymentAmountType === 'CUSTOM' && (
                  <div className="mt-2.5">
                    <label className="block text-[11px] text-slate-500 mb-1">
                      Enter Custom Partial Payment Amount (INR)
                    </label>
                    <div className="relative">
                      <span className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 font-mono font-bold">
                        ₹
                      </span>
                      <input
                        type="number"
                        min="1"
                        max={Number(selectedBill.outstandingBalance)}
                        step="0.01"
                        value={customAmount}
                        onChange={(e) => setCustomAmount(e.target.value)}
                        placeholder="e.g. 2500"
                        className="w-full pl-7 pr-3 py-2 bg-slate-50 border border-slate-200 rounded-xl font-mono text-xs focus:outline-none focus:border-teal-500"
                        required
                      />
                    </div>
                    <span className="text-[10px] text-slate-400 mt-1 block">
                      Max allowed: {formatINR(selectedBill.outstandingBalance)} (Overpayment prevented)
                    </span>
                  </div>
                )}
              </div>

              {/* Payment Rail Selector */}
              <div>
                <label className="block text-slate-700 font-semibold mb-1.5">
                  Mock Payment Method
                </label>
                <div className="grid grid-cols-3 gap-2">
                  {[
                    { id: 'UPI_MOCK', label: 'Mock UPI', sub: 'Instant (PhonePe/GPay)' },
                    { id: 'NET_BANKING_MOCK', label: 'Net Banking', sub: 'HDFC / ICICI / SBI' },
                    { id: 'CARD_MOCK', label: 'Test Card', sub: 'Visa / Mastercard' },
                  ].map((m) => (
                    <button
                      key={m.id}
                      type="button"
                      onClick={() => setPaymentMethod(m.id)}
                      className={`p-2 rounded-xl border text-left transition cursor-pointer ${
                        paymentMethod === m.id
                          ? 'border-teal-500 bg-teal-50/50 text-teal-900 ring-1 ring-teal-500'
                          : 'border-slate-200 bg-white text-slate-600 hover:border-slate-300'
                      }`}
                    >
                      <div className="font-semibold text-[11px]">{m.label}</div>
                      <div className="text-[9px] text-slate-400">{m.sub}</div>
                    </button>
                  ))}
                </div>
              </div>

              {/* Failure Simulation Toggle for testing error handling */}
              <div className="p-3 bg-slate-50 border border-slate-200 rounded-xl space-y-2">
                <label className="flex items-center gap-2 cursor-pointer select-none">
                  <input
                    type="checkbox"
                    checked={simulateFailure}
                    onChange={(e) => setSimulateFailure(e.target.checked)}
                    className="w-4 h-4 text-teal-600 rounded border-slate-300 focus:ring-teal-500"
                  />
                  <span className="font-semibold text-slate-800 text-xs">
                    Simulate Payment Gateway Failure (Error Testing)
                  </span>
                </label>

                {simulateFailure && (
                  <div className="pl-6 space-y-1.5">
                    <label className="block text-[11px] text-slate-500 font-medium">
                      Simulated Failure Code:
                    </label>
                    <select
                      value={failureReason}
                      onChange={(e) => setFailureReason(e.target.value)}
                      className="w-full p-1.5 bg-white border border-slate-200 rounded-lg text-xs"
                    >
                      <option value="INSUFFICIENT_FUNDS">Insufficient Funds in Mock Account</option>
                      <option value="BANK_DECLINE">Bank Gateway Timeout / Network Decline</option>
                      <option value="OTP_EXPIRED">Mock 2FA OTP Authentication Expired</option>
                      <option value="LIMIT_EXCEEDED">Daily UPI Transaction Limit Exceeded</option>
                    </select>
                  </div>
                )}
              </div>

              {/* Error Banner */}
              {paymentError && (
                <div className="p-3 bg-rose-50 border border-rose-200 rounded-xl text-rose-800 text-xs flex items-center gap-2">
                  <XCircle className="w-4 h-4 text-rose-600 shrink-0" />
                  <span>{paymentError}</span>
                </div>
              )}

              {/* Action Buttons */}
              <div className="flex items-center justify-end gap-2 pt-2 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setShowPaymentModal(false)}
                  className="px-4 py-2 rounded-xl border border-slate-200 text-slate-600 hover:bg-slate-50 font-medium cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={processingPayment}
                  className="px-5 py-2 rounded-xl bg-teal-600 hover:bg-teal-700 text-white font-semibold transition cursor-pointer flex items-center gap-1.5 disabled:opacity-50 shadow-sm"
                >
                  {processingPayment ? (
                    <>
                      <RotateCw className="w-3.5 h-3.5 animate-spin" />
                      Authorizing Test Gateway...
                    </>
                  ) : (
                    <>
                      <CreditCard className="w-3.5 h-3.5" />
                      Authorize Test Payment (
                      {formatINR(
                        paymentAmountType === 'FULL'
                          ? selectedBill.outstandingBalance
                          : Number(customAmount) || 0
                      )}
                      )
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* =========================================================================
          MODAL: Official GST Maintenance Receipt Viewer
         ========================================================================= */}
      {showReceiptModal && receiptData && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-2xl border border-slate-200 max-w-xl w-full p-6 animate-in fade-in zoom-in-95 max-h-[90vh] flex flex-col relative overflow-hidden">
            {/* Top Watermark for test mode */}
            <div className="absolute -right-12 top-6 rotate-45 bg-amber-500 text-white font-mono font-bold text-[9px] uppercase px-14 py-1 shadow-xs tracking-wider select-none pointer-events-none">
              TEST MODE
            </div>

            {/* Receipt Modal Header */}
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <div className="flex items-center gap-2">
                <div className="p-2 bg-emerald-50 rounded-xl text-emerald-600">
                  <CheckCircle2 className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="font-bold text-slate-900 text-sm">
                    Maintenance E-Receipt
                  </h3>
                  <span className="text-[10px] text-slate-400 font-mono">
                    PostgreSQL Audited • Receipt #{receiptData.receiptNumber}
                  </span>
                </div>
              </div>
              <button
                onClick={() => setShowReceiptModal(false)}
                className="p-1 rounded-lg hover:bg-slate-100 text-slate-400 hover:text-slate-700 transition cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Printable Receipt Body */}
            <div id="printable-receipt" className="flex-1 overflow-y-auto my-4 p-5 rounded-2xl bg-slate-50 border border-slate-200 space-y-4 font-sans text-xs">
              {/* Society Title Header */}
              <div className="text-center pb-3 border-b border-slate-200">
                <h2 className="text-sm font-bold text-slate-900 uppercase tracking-tight">
                  {receiptData.societyName || 'Pune Grand Vista Co-operative Housing Society'}
                </h2>
                <p className="text-[11px] text-slate-500 mt-0.5">
                  {receiptData.societyAddress || 'Hinjawadi Phase 1, Pune, Maharashtra 411057'}
                </p>
                <div className="mt-1.5 inline-block px-2.5 py-0.5 rounded-full bg-emerald-100 text-emerald-800 text-[10px] font-bold font-mono">
                  TAX INVOICE & MAINTENANCE RECEIPT
                </div>
              </div>

              {/* Meta Grid */}
              <div className="grid grid-cols-2 gap-3 text-[11px] font-mono">
                <div>
                  <span className="text-slate-400 block text-[10px]">Receipt Number</span>
                  <span className="font-bold text-slate-800">{receiptData.receiptNumber}</span>
                </div>
                <div className="text-right">
                  <span className="text-slate-400 block text-[10px]">Date & Timestamp</span>
                  <span className="font-bold text-slate-800">
                    {new Date(receiptData.paymentDate).toLocaleString('en-IN')}
                  </span>
                </div>
                <div>
                  <span className="text-slate-400 block text-[10px]">Transaction Reference</span>
                  <span className="font-bold text-teal-700">{receiptData.referenceNumber || receiptData.transactionReference}</span>
                </div>
                <div className="text-right">
                  <span className="text-slate-400 block text-[10px]">Payment Method</span>
                  <span className="font-bold text-slate-800 font-sans">
                    {receiptData.paymentMethod?.replace('_', ' ')}
                  </span>
                </div>
              </div>

              {/* Resident & Apartment Details */}
              <div className="p-3 bg-white rounded-xl border border-slate-200 space-y-1 font-sans">
                <div className="text-[10px] font-bold uppercase text-slate-400 tracking-wider">
                  Billed To Resident
                </div>
                <div className="font-bold text-slate-900 text-sm">
                  {receiptData.residentName}
                </div>
                <div className="text-slate-600 text-[11px] font-mono">
                  Apartment: {receiptData.towerName} • Unit #{receiptData.flatNumber} (Flat #{receiptData.flatId})
                </div>
                {receiptData.residentPhone && (
                  <div className="text-slate-400 text-[10px] font-mono">
                    Phone: {receiptData.residentPhone}
                  </div>
                )}
              </div>

              {/* Financial Calculation Breakdown Table */}
              <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-100 text-slate-600 font-semibold text-[10px] uppercase">
                    <tr>
                      <th className="px-3 py-2">Description</th>
                      <th className="px-3 py-2 text-right">Amount (INR)</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 font-mono text-[11px]">
                    <tr>
                      <td className="px-3 py-2 text-slate-800 font-sans">
                        Outstanding Assessment Prior to Payment
                      </td>
                      <td className="px-3 py-2 text-right text-slate-700">
                        {formatINR(receiptData.previousOutstanding)}
                      </td>
                    </tr>
                    <tr className="bg-emerald-50/50">
                      <td className="px-3 py-2 text-emerald-900 font-bold font-sans">
                        Amount Paid In This Receipt
                      </td>
                      <td className="px-3 py-2 text-right font-bold text-emerald-700 text-xs">
                        {formatINR(receiptData.amountPaid)}
                      </td>
                    </tr>
                    <tr className="bg-slate-50">
                      <td className="px-3 py-2 text-slate-800 font-semibold font-sans">
                        Remaining Outstanding Balance
                      </td>
                      <td className="px-3 py-2 text-right font-bold text-slate-900">
                        {formatINR(receiptData.remainingBalance)}
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>

              {/* Watermark & Legal Footer */}
              <div className="pt-2 text-center text-[10px] text-slate-400 font-mono space-y-0.5">
                <div>*** {receiptData.watermark || 'TEST MODE - SIMULATED FINANCIAL RECEIPT'} ***</div>
                <div>Computer generated test receipt. Immutable entry logged in PostgreSQL flat ledger.</div>
              </div>
            </div>

            {/* Receipt Modal Actions */}
            <div className="flex items-center justify-between pt-2 border-t border-slate-100">
              <span className="text-[11px] text-slate-500 font-medium">
                Bill Status: <span className="font-bold text-teal-700">{receiptData.billStatus}</span>
              </span>

              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={handlePrintReceipt}
                  className="px-3.5 py-1.5 rounded-xl border border-slate-200 text-slate-700 hover:bg-slate-50 font-semibold text-xs flex items-center gap-1.5 cursor-pointer shadow-xs"
                >
                  <Printer className="w-3.5 h-3.5" />
                  Print / Save PDF
                </button>
                <button
                  type="button"
                  onClick={() => setShowReceiptModal(false)}
                  className="px-4 py-1.5 rounded-xl bg-slate-900 hover:bg-slate-800 text-white font-semibold text-xs cursor-pointer shadow-xs"
                >
                  Done
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}