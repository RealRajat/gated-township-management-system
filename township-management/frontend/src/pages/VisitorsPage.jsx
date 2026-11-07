import { useState, useEffect, useCallback, useRef } from 'react';
import {
  ShieldCheck,
  UserCheck,
  UserX,
  Clock,
  Plus,
  Search,
  Filter,
  CheckCircle2,
  AlertCircle,
  X,
  QrCode,
  Phone,
  Building,
  Car,
  Truck,
  Wrench,
  Wifi,
  WifiOff,
  RefreshCw,
  Trash2,
  Eye,
  Camera,
  Check,
  FileText,
  Users,
  KeyRound,
  ShieldAlert,
  ArrowRight,
  Sparkles,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { visitorApi, townshipApi } from '../services/api';
import StatCard from '../components/StatCard';

const VISITOR_TYPES = [
  { value: 'GUEST', label: 'Guest / Family', icon: Users },
  { value: 'DELIVERY', label: 'Delivery / Courier', icon: Truck },
  { value: 'CAB', label: 'Cab / Taxi', icon: Car },
  { value: 'SERVICE', label: 'Service / Technician', icon: Wrench },
  { value: 'VENDOR', label: 'Regular Vendor', icon: FileText },
];

const LOCAL_STORAGE_OFFLINE_KEY = 'township_gate_offline_queue';
const LOCAL_STORAGE_CACHE_KEY = 'township_gate_active_passes';

export default function VisitorsPage() {
  const { user, isHelpDesk, isGuard } = useAuth();

  // Data states
  const [visitors, setVisitors] = useState([]);
  const [pendingApprovals, setPendingApprovals] = useState([]);
  const [overview, setOverview] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  // Tab & Filters
  const [activeTab, setActiveTab] = useState(isGuard ? 'gate' : 'all');
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [typeFilter, setTypeFilter] = useState('ALL');

  // Offline Tolerance State
  const [isOnline, setIsOnline] = useState(navigator.onLine);
  const [offlineQueue, setOfflineQueue] = useState(() => {
    try {
      const saved = localStorage.getItem(LOCAL_STORAGE_OFFLINE_KEY);
      return saved ? JSON.parse(saved) : [];
    } catch {
      return [];
    }
  });
  const [syncingOffline, setSyncingOffline] = useState(false);

  // Modals
  const [showPreApproveModal, setShowPreApproveModal] = useState(false);
  const [showWalkInModal, setShowWalkInModal] = useState(false);
  const [showPassModal, setShowPassModal] = useState(false);
  const [selectedVisitor, setSelectedVisitor] = useState(null);
  const [detailPhotoUrl, setDetailPhotoUrl] = useState(null);

  // Pre-Approve Form State
  const userFlats = user?.flats || [];
  const [formFlatId, setFormFlatId] = useState('');
  const [formName, setFormName] = useState('');
  const [formPhone, setFormPhone] = useState('');
  const [formType, setFormType] = useState('GUEST');
  const [formValidFrom, setFormValidFrom] = useState('');
  const [formValidUntil, setFormValidUntil] = useState('');
  const [formPurpose, setFormPurpose] = useState('');
  const [formVehicle, setFormVehicle] = useState('');
  const [formIsOneTime, setFormIsOneTime] = useState(true);
  const [submittingForm, setSubmittingForm] = useState(false);

  // Gate Lookup & Action State
  const [gateQuery, setGateQuery] = useState('');
  const [lookupResults, setLookupResults] = useState([]);
  const [lookupLoading, setLookupLoading] = useState(false);
  const [actionLoadingId, setActionLoadingId] = useState(null);

  // Walk-In Form State (Guard)
  const [towers, setTowers] = useState([]);
  const [selectedTowerId, setSelectedTowerId] = useState('T07');
  const [towerFlats, setTowerFlats] = useState([]);
  const [walkInFlatId, setWalkInFlatId] = useState('');
  const [walkInName, setWalkInName] = useState('');
  const [walkInPhone, setWalkInPhone] = useState('');
  const [walkInType, setWalkInType] = useState('DELIVERY');
  const [walkInPurpose, setWalkInPurpose] = useState('');
  const [walkInVehicle, setWalkInVehicle] = useState('');
  const [walkInGate, setWalkInGate] = useState('Gate 1 (Main)');
  const [walkInPhoto, setWalkInPhoto] = useState(null);
  const [walkInPhotoPreview, setWalkInPhotoPreview] = useState(null);
  const [submittingWalkIn, setSubmittingWalkIn] = useState(false);

  // Online / Offline Detection
  useEffect(() => {
    const handleOnline = () => setIsOnline(true);
    const handleOffline = () => setIsOnline(false);
    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);
    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  // Save offline queue to storage
  useEffect(() => {
    try {
      localStorage.setItem(LOCAL_STORAGE_OFFLINE_KEY, JSON.stringify(offlineQueue));
    } catch (e) {
      console.warn('Could not save offline queue:', e);
    }
  }, [offlineQueue]);

  // Load Initial Towers for Walk-in Selector
  useEffect(() => {
    if (isGuard || isHelpDesk) {
      townshipApi.getTowers().then((res) => {
        setTowers(res || []);
      }).catch(console.warn);
    }
  }, [isGuard, isHelpDesk]);

  // Load flats when tower changes in walk-in modal
  useEffect(() => {
    if (selectedTowerId) {
      townshipApi.getFlatsByTower(selectedTowerId).then((res) => {
        setTowerFlats(res || []);
        if (res && res.length > 0) {
          setWalkInFlatId(String(res[0].id));
        }
      }).catch(console.warn);
    }
  }, [selectedTowerId]);

  // Load Main Data
  const loadData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      if (isGuard) {
        const [overviewRes, activePasses] = await Promise.all([
          visitorApi.getGateOverview(),
          visitorApi.getActivePassesForCache().catch(() => []),
        ]);
        setOverview(overviewRes);

        // Cache active passes locally for offline resilience
        try {
          localStorage.setItem(LOCAL_STORAGE_CACHE_KEY, JSON.stringify(activePasses || []));
        } catch (e) {
          console.warn('Could not cache active passes:', e);
        }

        // Load active visitors inside
        const insideList = await visitorApi.getAllVisitors({ status: 'CHECKED_IN' });
        setVisitors(insideList || []);
      } else if (isHelpDesk) {
        const [allList, overviewRes] = await Promise.all([
          visitorApi.getAllVisitors({ status: statusFilter !== 'ALL' ? statusFilter : null }),
          visitorApi.getGateOverview(),
        ]);
        setVisitors(allList || []);
        setOverview(overviewRes);
      } else {
        // Resident
        const [myList, pendingList] = await Promise.all([
          visitorApi.getMyVisitors(statusFilter !== 'ALL' ? statusFilter : null),
          visitorApi.getMyPendingApprovals(),
        ]);
        setVisitors(myList || []);
        setPendingApprovals(pendingList || []);
      }
    } catch (err) {
      console.error('Failed to load visitors data:', err);
      setError(err.message || 'Failed to load visitor records');
    } finally {
      setLoading(false);
    }
  }, [isGuard, isHelpDesk, statusFilter]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Polling for pending resident approvals (every 10 seconds for residents and guards)
  useEffect(() => {
    const interval = setInterval(() => {
      if (!isHelpDesk && !isGuard) {
        visitorApi.getMyPendingApprovals().then((res) => {
          setPendingApprovals(res || []);
        }).catch(() => {});
      } else if (isGuard) {
        visitorApi.getGateOverview().then((res) => {
          setOverview(res);
        }).catch(() => {});
      }
    }, 10000);
    return () => clearInterval(interval);
  }, [isGuard, isHelpDesk]);

  // Open Pass Card Details Modal
  const openPassDetails = async (visitor) => {
    setSelectedVisitor(visitor);
    setShowPassModal(true);
    if (detailPhotoUrl) {
      URL.revokeObjectURL(detailPhotoUrl);
      setDetailPhotoUrl(null);
    }
    if (visitor.hasPhoto) {
      try {
        const blobUrl = await visitorApi.getVisitorPhotoBlobUrl(visitor.id);
        setDetailPhotoUrl(blobUrl);
      } catch (e) {
        console.warn('Could not load photo preview:', e);
      }
    }
  };

  const closePassModal = () => {
    setShowPassModal(false);
    setSelectedVisitor(null);
    if (detailPhotoUrl) {
      URL.revokeObjectURL(detailPhotoUrl);
      setDetailPhotoUrl(null);
    }
  };

  // Pre-Approve Modal Helpers
  const openPreApproveModal = () => {
    if (userFlats.length > 0) {
      setFormFlatId(String(userFlats[0].flatId));
    }
    setFormName('');
    setFormPhone('');
    setFormType('GUEST');
    setFormPurpose('');
    setFormVehicle('');
    setFormIsOneTime(true);

    // Default: From Now to +12 Hours in local datetime-local format
    const now = new Date();
    const future = new Date(now.getTime() + 12 * 60 * 60 * 1000);
    const toLocalISO = (d) => new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 16);

    setFormValidFrom(toLocalISO(now));
    setFormValidUntil(toLocalISO(future));
    setShowPreApproveModal(true);
  };

  // Submit Pre-Approval
  const handlePreApproveSubmit = async (e) => {
    e.preventDefault();
    if (!formFlatId) {
      alert('Please select an apartment flat.');
      return;
    }
    if (!formName.trim() || !formPhone.trim()) {
      alert('Please provide visitor name and contact phone.');
      return;
    }

    setSubmittingForm(true);
    try {
      const fromIso = new Date(formValidFrom).toISOString().slice(0, 19);
      const untilIso = new Date(formValidUntil).toISOString().slice(0, 19);

      const created = await visitorApi.preApproveVisitor({
        flatId: Number(formFlatId),
        visitorName: formName.trim(),
        visitorPhone: formPhone.trim(),
        visitorType: formType,
        validFrom: fromIso,
        validUntil: untilIso,
        purpose: formPurpose.trim() || null,
        vehicleNumber: formVehicle.trim() || null,
        isOneTime: formIsOneTime,
      });

      setSuccessMessage(`Visitor Pass Created: ${created.passCode} for ${created.visitorName}`);
      setTimeout(() => setSuccessMessage(null), 5000);
      setShowPreApproveModal(false);
      loadData();
      openPassDetails(created);
    } catch (err) {
      alert('Failed to pre-approve visitor: ' + (err.message || ''));
    } finally {
      setSubmittingForm(false);
    }
  };

  // Resident: Approve or Reject Pending Request
  const handleResidentDecision = async (visitorId, approved, reason = '') => {
    setActionLoadingId(visitorId);
    try {
      const updated = await visitorApi.processApprovalDecision(visitorId, {
        approved,
        rejectionReason: reason || (approved ? null : 'Entry denied by resident'),
      });
      setSuccessMessage(
        approved
          ? `Approved entry for ${updated.visitorName}. Pass Code: ${updated.passCode}`
          : `Rejected entry for ${updated.visitorName}`
      );
      setTimeout(() => setSuccessMessage(null), 4000);
      loadData();
    } catch (err) {
      alert('Error updating approval decision: ' + (err.message || ''));
    } finally {
      setActionLoadingId(null);
    }
  };

  // Resident: Revoke Pass
  const handleRevokePass = async (visitorId) => {
    if (!confirm('Are you sure you want to revoke this visitor pass? It cannot be used once revoked.')) {
      return;
    }
    setActionLoadingId(visitorId);
    try {
      const updated = await visitorApi.revokePass(visitorId);
      setSuccessMessage(`Pass ${updated.passCode} has been revoked.`);
      setTimeout(() => setSuccessMessage(null), 4000);
      loadData();
      if (selectedVisitor?.id === visitorId) {
        setSelectedVisitor(updated);
      }
    } catch (err) {
      alert('Failed to revoke pass: ' + (err.message || ''));
    } finally {
      setActionLoadingId(null);
    }
  };

  // Gate Guard: Search / Lookup
  const handleGateSearch = async (e) => {
    e?.preventDefault();
    if (!gateQuery.trim()) return;
    setLookupLoading(true);
    try {
      if (isOnline) {
        const results = await visitorApi.gateLookup(gateQuery.trim());
        setLookupResults(results || []);
      } else {
        // Offline Lookup: Match against locally cached passes
        const cachedRaw = localStorage.getItem(LOCAL_STORAGE_CACHE_KEY);
        const cachedList = cachedRaw ? JSON.parse(cachedRaw) : [];
        const term = gateQuery.trim().toLowerCase();
        const matches = cachedList.filter(
          (p) =>
            p.passCode.toLowerCase().includes(term) ||
            p.passToken.toLowerCase().includes(term) ||
            p.visitorPhone.includes(term) ||
            p.visitorName.toLowerCase().includes(term)
        );
        setLookupResults(
          matches.map((m) => ({
            ...m,
            id: 0,
            towerName: m.towerName,
            flatNumber: m.flatNumber,
            isOfflineCached: true,
          }))
        );
      }
    } catch (err) {
      alert('Lookup failed: ' + (err.message || ''));
    } finally {
      setLookupLoading(false);
    }
  };

  // Gate Guard: Check-In Visitor
  const handleGateCheckIn = async (passCodeOrToken, vehicleNum = '') => {
    setActionLoadingId(passCodeOrToken);
    try {
      if (isOnline) {
        const updated = await visitorApi.gateCheckIn({
          passCodeOrToken,
          gate: 'Gate 1 (Main)',
          vehicleNumber: vehicleNum || null,
        });
        setSuccessMessage(`CHECK-IN SUCCESS: ${updated.visitorName} (Flat ${updated.flatNumber})`);
        setTimeout(() => setSuccessMessage(null), 5000);
        setLookupResults([]);
        setGateQuery('');
        loadData();
      } else {
        // Queue Offline Check-In
        const offlineId = 'OFF-' + Date.now() + '-' + Math.random().toString(36).substring(2, 8);
        const offlineEvent = {
          offlineSyncId: offlineId,
          action: 'CHECK_IN',
          passCodeOrToken,
          timestamp: new Date().toISOString().slice(0, 19),
          gate: 'Gate 1 (Offline)',
          vehicleNumber: vehicleNum || null,
        };

        setOfflineQueue((prev) => [...prev, offlineEvent]);
        setSuccessMessage(`OFFLINE CHECK-IN QUEUED: Pass ${passCodeOrToken} saved in buffer.`);
        setTimeout(() => setSuccessMessage(null), 5000);
        setLookupResults([]);
        setGateQuery('');
      }
    } catch (err) {
      alert('Check-In Failed: ' + (err.message || ''));
    } finally {
      setActionLoadingId(null);
    }
  };

  // Gate Guard: Check-Out Visitor
  const handleGateCheckOut = async (visitorId, passCode) => {
    setActionLoadingId(visitorId || passCode);
    try {
      if (isOnline) {
        const updated = await visitorApi.gateCheckOut(visitorId, { gate: 'Gate 1 (Main)' });
        setSuccessMessage(`CHECK-OUT RECORDED: ${updated.visitorName} exited premises.`);
        setTimeout(() => setSuccessMessage(null), 4000);
        loadData();
      } else {
        // Queue Offline Check-Out
        const offlineId = 'OFF-' + Date.now() + '-' + Math.random().toString(36).substring(2, 8);
        const offlineEvent = {
          offlineSyncId: offlineId,
          action: 'CHECK_OUT',
          passCodeOrToken: passCode,
          timestamp: new Date().toISOString().slice(0, 19),
          gate: 'Gate 1 (Offline)',
        };

        setOfflineQueue((prev) => [...prev, offlineEvent]);
        setSuccessMessage(`OFFLINE CHECK-OUT QUEUED: ${passCode} marked for exit.`);
        setTimeout(() => setSuccessMessage(null), 4000);
      }
    } catch (err) {
      alert('Check-Out Failed: ' + (err.message || ''));
    } finally {
      setActionLoadingId(null);
    }
  };

  // Trigger Offline Sync to Backend
  const handleSyncOfflineEvents = async () => {
    if (offlineQueue.length === 0) {
      alert('Offline sync queue is empty.');
      return;
    }
    if (!isOnline) {
      alert('Cannot synchronize: Gate device is currently offline.');
      return;
    }

    setSyncingOffline(true);
    try {
      const res = await visitorApi.syncOfflineEvents(offlineQueue);
      setSuccessMessage(
        `Offline Sync Complete: ${res.successfullySynced} synced, ${res.duplicatesSkipped} duplicates skipped, ${res.failed} failed.`
      );
      setTimeout(() => setSuccessMessage(null), 6000);
      setOfflineQueue([]);
      localStorage.removeItem(LOCAL_STORAGE_OFFLINE_KEY);
      loadData();
    } catch (err) {
      alert('Offline sync failed: ' + (err.message || ''));
    } finally {
      setSyncingOffline(false);
    }
  };

  // Gate Guard: Walk-In Registration
  const handleWalkInSubmit = async (e) => {
    e.preventDefault();
    if (!walkInFlatId) {
      alert('Please select a destination flat.');
      return;
    }
    if (!walkInName.trim() || !walkInPhone.trim()) {
      alert('Please enter visitor name and mobile phone.');
      return;
    }

    if (!isOnline) {
      alert(
        'Offline Notice: Unapproved walk-in requests require active resident mobile verification and cannot be approved while the gate is offline.'
      );
      return;
    }

    setSubmittingWalkIn(true);
    try {
      let created;
      if (walkInPhoto) {
        const formData = new FormData();
        formData.append('flatId', walkInFlatId);
        formData.append('visitorName', walkInName.trim());
        formData.append('visitorPhone', walkInPhone.trim());
        formData.append('visitorType', walkInType);
        if (walkInPurpose) formData.append('purpose', walkInPurpose.trim());
        if (walkInVehicle) formData.append('vehicleNumber', walkInVehicle.trim());
        formData.append('gate', walkInGate);
        formData.append('photo', walkInPhoto);
        created = await visitorApi.gateWalkIn(formData, true);
      } else {
        created = await visitorApi.gateWalkIn({
          flatId: Number(walkInFlatId),
          visitorName: walkInName.trim(),
          visitorPhone: walkInPhone.trim(),
          visitorType: walkInType,
          purpose: walkInPurpose.trim() || null,
          vehicleNumber: walkInVehicle.trim() || null,
          gate: walkInGate,
        });
      }

      setSuccessMessage(
        `Approval Request Sent: ${created.visitorName} (Pass ${created.passCode}). Waiting for resident response.`
      );
      setTimeout(() => setSuccessMessage(null), 5000);
      setShowWalkInModal(false);
      resetWalkInForm();
      loadData();
    } catch (err) {
      alert('Failed to register walk-in visitor: ' + (err.message || ''));
    } finally {
      setSubmittingWalkIn(false);
    }
  };

  const resetWalkInForm = () => {
    setWalkInName('');
    setWalkInPhone('');
    setWalkInType('DELIVERY');
    setWalkInPurpose('');
    setWalkInVehicle('');
    setWalkInPhoto(null);
    if (walkInPhotoPreview) {
      URL.revokeObjectURL(walkInPhotoPreview);
      setWalkInPhotoPreview(null);
    }
  };

  // Help Desk: 90-Day Privacy Cleanup
  const handlePrivacyCleanup = async () => {
    if (!confirm('Run 90-Day Visitor Privacy Retention cleanup? All visitor photos older than 90 days will be physically deleted.')) {
      return;
    }
    try {
      const res = await visitorApi.cleanupPhotos();
      setSuccessMessage(`Privacy Purge Complete: ${res.purgedPhotoCount} expired visitor photos physically deleted.`);
      setTimeout(() => setSuccessMessage(null), 6000);
    } catch (err) {
      alert('Cleanup failed: ' + (err.message || ''));
    }
  };

  // Filter visitors in memory
  const filteredVisitors = visitors.filter((v) => {
    const term = searchTerm.toLowerCase();
    const matchesSearch =
      !term ||
      v.visitorName.toLowerCase().includes(term) ||
      v.visitorPhone.includes(term) ||
      v.passCode.toLowerCase().includes(term) ||
      (v.flatNumber && v.flatNumber.toLowerCase().includes(term));
    const matchesType = typeFilter === 'ALL' || v.visitorType === typeFilter;
    return matchesSearch && matchesType;
  });

  return (
    <div className="space-y-6">
      {/* Success Notification Banner */}
      {successMessage && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-xl flex items-center justify-between shadow-sm animate-fade-in">
          <div className="flex items-center gap-2 text-sm font-semibold">
            <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
            <span>{successMessage}</span>
          </div>
          <button
            onClick={() => setSuccessMessage(null)}
            className="text-emerald-600 hover:text-emerald-900 text-xs font-semibold"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Header and Quick Stats */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold text-slate-900 tracking-tight">
              {isGuard ? 'Gate Security Command & Visitor Triage' : isHelpDesk ? 'Township Visitor Management' : 'My Visitors & Gate Passes'}
            </h1>
            <span
              className={`text-[11px] font-bold px-2 py-0.5 rounded-full ${
                isGuard
                  ? 'bg-amber-100 text-amber-900 border border-amber-300'
                  : isHelpDesk
                  ? 'bg-indigo-50 text-indigo-700 border border-indigo-200'
                  : 'bg-teal-50 text-teal-700 border border-teal-200'
              }`}
            >
              {isGuard ? 'Guard Operations' : isHelpDesk ? 'Staff Overview' : 'Resident Portal'}
            </span>
          </div>
          <p className="text-xs sm:text-sm text-slate-500 mt-0.5">
            {isGuard
              ? 'Fast-track gate access, scan pre-approved QR passes, manage walk-in approvals, and offline-tolerant logging.'
              : 'Pre-approve guests, food & package deliveries, track on-premise arrivals, and approve walk-ins in real time.'}
          </p>
        </div>

        <div className="flex items-center gap-2">
          {/* Online / Offline Status Badge */}
          <div
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold flex items-center gap-1.5 border ${
              isOnline
                ? 'bg-emerald-50 text-emerald-800 border-emerald-200'
                : 'bg-amber-50 text-amber-900 border-amber-300 animate-pulse'
            }`}
          >
            {isOnline ? <Wifi className="w-3.5 h-3.5 text-emerald-600" /> : <WifiOff className="w-3.5 h-3.5 text-amber-600" />}
            <span>{isOnline ? 'Gate Online' : 'Gate Offline'}</span>
          </div>

          {/* Sync Offline Queue Button (if offline items exist) */}
          {offlineQueue.length > 0 && (
            <button
              onClick={handleSyncOfflineEvents}
              disabled={syncingOffline || !isOnline}
              className="px-3 py-1.5 bg-amber-600 hover:bg-amber-700 text-white rounded-lg text-xs font-semibold flex items-center gap-1 shadow-sm transition disabled:opacity-50"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${syncingOffline ? 'animate-spin' : ''}`} />
              Sync Buffer ({offlineQueue.length})
            </button>
          )}

          <button
            onClick={loadData}
            title="Refresh Visitors"
            className="p-2 border border-slate-200 hover:bg-slate-50 text-slate-600 rounded-lg transition"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          </button>

          {!isGuard && (
            <button
              onClick={openPreApproveModal}
              className="inline-flex items-center gap-1.5 px-4 py-2 bg-teal-600 hover:bg-teal-700 text-white rounded-lg text-xs font-semibold shadow-sm transition"
            >
              <Plus className="w-4 h-4" />
              Pre-Approve Visitor
            </button>
          )}

          {isGuard && (
            <button
              onClick={() => {
                resetWalkInForm();
                setShowWalkInModal(true);
              }}
              className="inline-flex items-center gap-1.5 px-4 py-2 bg-slate-900 hover:bg-slate-800 text-white rounded-lg text-xs font-semibold shadow-sm transition"
            >
              <UserCheck className="w-4 h-4" />
              Log Walk-In Entry
            </button>
          )}
        </div>
      </div>

      {/* KPI Cards for Gate / Staff */}
      {(isGuard || isHelpDesk) && overview && (
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          <StatCard
            title="Currently Inside"
            value={overview.currentlyInside || 0}
            subtext="Visitors on township premises"
            icon={UserCheck}
            colorScheme="teal"
          />
          <StatCard
            title="Today's Gate Entries"
            value={overview.todayTotalCheckIns || 0}
            subtext="Recorded check-ins today"
            icon={CheckCircle2}
            colorScheme="emerald"
          />
          <div className="bg-amber-50 border-2 border-amber-300 rounded-xl p-4 shadow-sm">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-amber-900 uppercase tracking-wider">
                Pending Approvals
              </span>
              <Clock className="w-5 h-5 text-amber-600" />
            </div>
            <div className="text-2xl font-black text-amber-950 mt-2">
              {overview.pendingApprovals || 0}
            </div>
            <p className="text-[11px] font-semibold text-amber-800 mt-1">
              Awaiting resident mobile confirmation
            </p>
          </div>
          <StatCard
            title="Upcoming Pre-Approved"
            value={overview.preApprovedUpcoming || 0}
            subtext="Expected visitor passes"
            icon={KeyRound}
            colorScheme="indigo"
          />
        </div>
      )}

      {/* RESIDENT: PENDING APPROVALS ALERT BANNER */}
      {!isGuard && !isHelpDesk && pendingApprovals.length > 0 && (
        <div className="bg-amber-50 border-2 border-amber-400 rounded-2xl p-5 shadow-sm space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <ShieldAlert className="w-5 h-5 text-amber-600 animate-pulse" />
              <h3 className="font-bold text-sm text-amber-950">
                Action Required: {pendingApprovals.length} Visitor(s) at Gate Awaiting Your Approval
              </h3>
            </div>
            <span className="text-[11px] font-mono bg-amber-200/60 text-amber-900 px-2.5 py-0.5 rounded-full font-bold">
              Gate 1 Intercom Request
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {pendingApprovals.map((req) => (
              <div
                key={req.id}
                className="bg-white p-4 rounded-xl border border-amber-200 shadow-xs flex flex-col justify-between gap-3"
              >
                <div>
                  <div className="flex items-center justify-between">
                    <span className="font-mono text-xs font-bold text-slate-800 bg-slate-100 px-2 py-0.5 rounded">
                      {req.passCode}
                    </span>
                    <span className="text-xs font-semibold text-teal-700 bg-teal-50 px-2 py-0.5 rounded">
                      {req.visitorType}
                    </span>
                  </div>
                  <h4 className="font-bold text-sm text-slate-900 mt-2">{req.visitorName}</h4>
                  <p className="text-xs text-slate-500 flex items-center gap-1 mt-0.5">
                    <Phone className="w-3 h-3" />
                    {req.visitorPhone}
                  </p>
                  {req.purpose && (
                    <p className="text-xs text-slate-700 mt-2 italic bg-slate-50 p-2 rounded-lg border border-slate-100">
                      &ldquo;{req.purpose}&rdquo;
                    </p>
                  )}
                  {req.vehicleNumber && (
                    <p className="text-[11px] text-slate-500 mt-1">Vehicle: {req.vehicleNumber}</p>
                  )}
                </div>

                <div className="flex items-center gap-2 pt-2 border-t border-slate-100">
                  <button
                    onClick={() => handleResidentDecision(req.id, true)}
                    disabled={actionLoadingId === req.id}
                    className="flex-1 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-bold flex items-center justify-center gap-1.5 shadow-sm transition disabled:opacity-50"
                  >
                    <UserCheck className="w-4 h-4" />
                    Approve Entry
                  </button>
                  <button
                    onClick={() => handleResidentDecision(req.id, false)}
                    disabled={actionLoadingId === req.id}
                    className="py-2 px-3 bg-rose-50 hover:bg-rose-100 text-rose-700 rounded-lg text-xs font-bold flex items-center justify-center gap-1 border border-rose-200 transition disabled:opacity-50"
                  >
                    <UserX className="w-4 h-4" />
                    Reject
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* GATE GUARD INTERFACE: QUICK SCAN / SEARCH & ACTIONS                       */}
      {/* ========================================================================= */}
      {isGuard && (
        <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-5 space-y-4">
          <div className="flex items-center justify-between border-b border-slate-100 pb-3">
            <div className="flex items-center gap-2">
              <QrCode className="w-5 h-5 text-teal-600" />
              <h2 className="font-bold text-base text-slate-900">
                Gate 1 Check-In Scanner & Fast Lookup
              </h2>
            </div>
            <span className="text-xs text-slate-400">
              Works offline with cached pre-approved passes
            </span>
          </div>

          <form onSubmit={handleGateSearch} className="flex gap-2">
            <div className="relative flex-1">
              <Search className="w-5 h-5 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                value={gateQuery}
                onChange={(e) => setGateQuery(e.target.value)}
                placeholder="Scan QR token, enter Pass Code (e.g. VP-892411) or Visitor Mobile Number..."
                className="w-full pl-11 pr-4 py-3 bg-slate-50 border border-slate-200 text-sm font-medium rounded-xl focus:outline-none focus:border-teal-500 focus:bg-white transition"
              />
            </div>
            <button
              type="submit"
              disabled={lookupLoading || !gateQuery.trim()}
              className="px-6 py-3 bg-teal-600 hover:bg-teal-700 text-white font-bold text-sm rounded-xl shadow-sm transition disabled:opacity-50 flex items-center gap-2"
            >
              {lookupLoading ? <RefreshCw className="w-4 h-4 animate-spin" /> : <Search className="w-4 h-4" />}
              Verify Pass
            </button>
          </form>

          {/* Lookup Results Card */}
          {lookupResults.length > 0 && (
            <div className="space-y-3 pt-2">
              <h3 className="text-xs font-bold text-slate-600 uppercase tracking-wider">
                Matching Visitor Pass Records ({lookupResults.length})
              </h3>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                {lookupResults.map((item) => (
                  <div
                    key={item.passCode}
                    className="p-4 rounded-xl border border-teal-200 bg-teal-50/40 shadow-xs space-y-3"
                  >
                    <div className="flex items-center justify-between">
                      <span className="font-mono text-sm font-black text-slate-900 bg-white px-2.5 py-1 rounded border border-slate-200">
                        {item.passCode}
                      </span>
                      <span
                        className={`text-xs font-bold px-2.5 py-1 rounded-full ${
                          item.status === 'PRE_APPROVED' || item.status === 'APPROVED'
                            ? 'bg-emerald-100 text-emerald-800 border border-emerald-300'
                            : item.status === 'CHECKED_IN'
                            ? 'bg-blue-100 text-blue-800 border border-blue-300'
                            : 'bg-slate-100 text-slate-700'
                        }`}
                      >
                        {item.status}
                      </span>
                    </div>

                    <div>
                      <h4 className="font-bold text-base text-slate-900">{item.visitorName}</h4>
                      <p className="text-xs text-slate-600 font-medium">
                        Phone: <strong>{item.visitorPhone}</strong> • Type: <strong>{item.visitorType}</strong>
                      </p>
                      <p className="text-xs text-slate-800 font-bold mt-1 bg-white p-2 rounded-lg border border-slate-200 flex items-center gap-1.5">
                        <Building className="w-4 h-4 text-teal-600" />
                        Destination: Tower {item.towerName} • Flat {item.flatNumber}
                      </p>
                      {item.residentName && (
                        <p className="text-[11px] text-slate-500 mt-1">Resident: {item.residentName}</p>
                      )}
                    </div>

                    {item.status === 'PRE_APPROVED' || item.status === 'APPROVED' ? (
                      <button
                        onClick={() => handleGateCheckIn(item.passCode, item.vehicleNumber)}
                        disabled={actionLoadingId === item.passCode}
                        className="w-full py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs rounded-lg shadow-sm transition disabled:opacity-50 flex items-center justify-center gap-1.5"
                      >
                        <UserCheck className="w-4 h-4" />
                        Confirm & Allow Gate Entry
                      </button>
                    ) : item.status === 'CHECKED_IN' ? (
                      <button
                        onClick={() => handleGateCheckOut(item.id, item.passCode)}
                        disabled={actionLoadingId === item.id}
                        className="w-full py-2.5 bg-slate-900 hover:bg-slate-800 text-white font-bold text-xs rounded-lg shadow-sm transition disabled:opacity-50 flex items-center justify-center gap-1.5"
                      >
                        <UserX className="w-4 h-4" />
                        Record Gate Check-Out
                      </button>
                    ) : (
                      <div className="p-2 bg-slate-100 rounded text-center text-xs text-slate-600 font-semibold">
                        Entry Denied: Pass status is {item.status}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* Filter and Search Bar for Visitor Logs */}
      <div className="bg-white rounded-xl border border-slate-200/90 shadow-sm p-4 flex flex-col lg:flex-row lg:items-center justify-between gap-3">
        <div className="relative flex-1">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Search by visitor name, mobile phone, pass code, or flat..."
            className="w-full pl-9 pr-3 py-2 bg-slate-50 border border-slate-200 text-xs rounded-lg focus:outline-none focus:border-teal-500 focus:bg-white transition"
          />
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {/* Status Filter */}
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="px-2.5 py-1.5 bg-slate-50 border border-slate-200 text-xs rounded-lg text-slate-700 font-medium focus:outline-none"
          >
            <option value="ALL">All Statuses</option>
            <option value="PRE_APPROVED">Pre-Approved</option>
            <option value="PENDING_APPROVAL">Pending Approval</option>
            <option value="APPROVED">Approved</option>
            <option value="CHECKED_IN">Checked In (Inside)</option>
            <option value="CHECKED_OUT">Checked Out</option>
            <option value="EXPIRED">Expired</option>
            <option value="REVOKED">Revoked</option>
          </select>

          {/* Type Filter */}
          <select
            value={typeFilter}
            onChange={(e) => setTypeFilter(e.target.value)}
            className="px-2.5 py-1.5 bg-slate-50 border border-slate-200 text-xs rounded-lg text-slate-700 font-medium focus:outline-none"
          >
            <option value="ALL">All Categories</option>
            {VISITOR_TYPES.map((t) => (
              <option key={t.value} value={t.value}>
                {t.label}
              </option>
            ))}
          </select>

          {/* Help Desk Privacy Cleanup Action */}
          {isHelpDesk && (
            <button
              onClick={handlePrivacyCleanup}
              title="Run 90-Day Privacy Retention Policy"
              className="px-3 py-1.5 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-semibold rounded-lg flex items-center gap-1 transition"
            >
              <Trash2 className="w-3.5 h-3.5 text-slate-500" />
              90-Day Purge
            </button>
          )}
        </div>
      </div>

      {/* Visitors List / Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="p-4 sm:p-5 border-b border-slate-100 flex items-center justify-between">
          <div>
            <h2 className="text-sm font-bold text-slate-900">
              {isGuard ? 'Active On-Premises & Expected Visitors' : 'Visitor Pass Registry'}
            </h2>
            <span className="text-xs text-slate-400">
              Showing {filteredVisitors.length} visitor passes
            </span>
          </div>
        </div>

        {loading ? (
          <div className="p-12 text-center text-slate-400 text-sm flex flex-col items-center gap-3">
            <RefreshCw className="w-6 h-6 animate-spin text-teal-600" />
            <span>Loading visitor records...</span>
          </div>
        ) : filteredVisitors.length === 0 ? (
          <div className="p-12 text-center">
            <UserCheck className="w-10 h-10 text-slate-300 mx-auto mb-2" />
            <p className="text-sm font-semibold text-slate-700">No visitors found</p>
            <p className="text-xs text-slate-400 mt-1">
              No matching records for your current search or status filter.
            </p>
          </div>
        ) : (
          <div className="divide-y divide-slate-100">
            {filteredVisitors.map((item) => (
              <div
                key={item.id}
                className="p-4 sm:p-5 hover:bg-slate-50/70 transition flex flex-col md:flex-row md:items-center justify-between gap-4"
              >
                <div className="space-y-1.5 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="font-mono text-xs font-bold text-slate-800 bg-slate-100 px-2.5 py-0.5 rounded border border-slate-200">
                      {item.passCode}
                    </span>

                    <span className="font-semibold text-xs text-slate-900 bg-slate-50 px-2 py-0.5 rounded border border-slate-200 flex items-center gap-1">
                      <Building className="w-3 h-3 text-slate-400" />
                      {item.towerName} • Flat {item.flatNumber}
                    </span>

                    <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-indigo-50 text-indigo-700 border border-indigo-200">
                      {item.visitorType}
                    </span>

                    {item.isOfflineSync && (
                      <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-amber-50 text-amber-800 border border-amber-200">
                        Offline Synced
                      </span>
                    )}

                    {item.hasPhoto && (
                      <span className="text-[10px] font-medium px-2 py-0.5 rounded bg-teal-50 text-teal-700 border border-teal-200 flex items-center gap-1">
                        <Camera className="w-3 h-3" />
                        Photo
                      </span>
                    )}
                  </div>

                  <h3 className="text-sm font-semibold text-slate-900">{item.visitorName}</h3>

                  <div className="flex flex-wrap items-center gap-3 text-xs text-slate-400">
                    <span>
                      Mobile: <strong className="text-slate-700">{item.visitorPhone}</strong>
                    </span>
                    <span>•</span>
                    {item.purpose && (
                      <>
                        <span>Purpose: {item.purpose}</span>
                        <span>•</span>
                      </>
                    )}
                    <span>
                      Valid:{' '}
                      {item.validFrom
                        ? new Date(item.validFrom).toLocaleDateString('en-IN', {
                            day: '2-digit',
                            month: 'short',
                            hour: '2-digit',
                            minute: '2-digit',
                          })
                        : 'Immediate'}{' '}
                      to{' '}
                      {item.validUntil
                        ? new Date(item.validUntil).toLocaleDateString('en-IN', {
                            day: '2-digit',
                            month: 'short',
                            hour: '2-digit',
                            minute: '2-digit',
                          })
                        : 'End of Day'}
                    </span>
                  </div>
                </div>

                <div className="flex items-center gap-3 shrink-0 self-start md:self-center">
                  <span
                    className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold ${
                      item.status === 'CHECKED_IN'
                        ? 'bg-blue-50 text-blue-700 border border-blue-200'
                        : item.status === 'CHECKED_OUT'
                        ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                        : item.status === 'PRE_APPROVED'
                        ? 'bg-teal-50 text-teal-700 border border-teal-200'
                        : item.status === 'PENDING_APPROVAL'
                        ? 'bg-amber-50 text-amber-800 border border-amber-300'
                        : item.status === 'REVOKED'
                        ? 'bg-rose-50 text-rose-700 border border-rose-200'
                        : 'bg-slate-100 text-slate-700 border border-slate-200'
                    }`}
                  >
                    {item.status}
                  </span>

                  {/* Actions depending on role */}
                  {isGuard && item.status === 'CHECKED_IN' && (
                    <button
                      onClick={() => handleGateCheckOut(item.id, item.passCode)}
                      disabled={actionLoadingId === item.id}
                      className="px-3 py-1.5 bg-slate-900 hover:bg-slate-800 text-white text-xs font-semibold rounded-lg transition"
                    >
                      Record Exit
                    </button>
                  )}

                  {!isGuard && !isHelpDesk && item.status === 'PRE_APPROVED' && (
                    <button
                      onClick={() => handleRevokePass(item.id)}
                      disabled={actionLoadingId === item.id}
                      className="px-3 py-1.5 bg-rose-50 hover:bg-rose-100 text-rose-700 text-xs font-semibold rounded-lg border border-rose-200 transition"
                    >
                      Revoke Pass
                    </button>
                  )}

                  <button
                    onClick={() => openPassDetails(item)}
                    className="p-1.5 border border-slate-200 hover:bg-slate-100 text-slate-600 rounded-lg transition"
                    title="View Pass & QR Details"
                  >
                    <Eye className="w-4 h-4" />
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* ========================================================================= */}
      {/* MODAL: PRE-APPROVE VISITOR (RESIDENT)                                     */}
      {/* ========================================================================= */}
      {showPreApproveModal && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 overflow-y-auto">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl space-y-4 my-8 border border-slate-100">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <div className="flex items-center gap-2">
                <div className="p-2 bg-teal-50 text-teal-600 rounded-lg">
                  <KeyRound className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-slate-900">Pre-Approve Visitor Pass</h3>
                  <p className="text-xs text-slate-500">Fast-track gate access with secure QR token</p>
                </div>
              </div>
              <button
                onClick={() => setShowPreApproveModal(false)}
                className="p-1.5 text-slate-400 hover:text-slate-600 rounded-lg hover:bg-slate-100"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handlePreApproveSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">
                  Destination Flat <span className="text-rose-500">*</span>
                </label>
                {userFlats.length > 0 ? (
                  <select
                    value={formFlatId}
                    onChange={(e) => setFormFlatId(e.target.value)}
                    required
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 text-xs rounded-lg font-medium focus:outline-none"
                  >
                    {userFlats.map((f) => (
                      <option key={f.flatId} value={f.flatId}>
                        Tower {f.towerName} ({f.towerId}) — Flat {f.flatNumber}
                      </option>
                    ))}
                  </select>
                ) : (
                  <input
                    type="number"
                    value={formFlatId}
                    onChange={(e) => setFormFlatId(e.target.value)}
                    placeholder="Enter Flat ID (e.g. 1)"
                    required
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 text-xs rounded-lg focus:outline-none"
                  />
                )}
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    Visitor Full Name <span className="text-rose-500">*</span>
                  </label>
                  <input
                    type="text"
                    value={formName}
                    onChange={(e) => setFormName(e.target.value)}
                    placeholder="e.g. Rohit Sharma"
                    required
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 text-xs rounded-lg focus:outline-none"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    Mobile Phone <span className="text-rose-500">*</span>
                  </label>
                  <input
                    type="tel"
                    value={formPhone}
                    onChange={(e) => setFormPhone(e.target.value)}
                    placeholder="e.g. +91 98220 12345"
                    required
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 text-xs rounded-lg focus:outline-none"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">
                  Visitor Category
                </label>
                <div className="grid grid-cols-3 gap-2">
                  {VISITOR_TYPES.map((t) => (
                    <button
                      key={t.value}
                      type="button"
                      onClick={() => setFormType(t.value)}
                      className={`p-2 rounded-lg text-xs font-semibold flex flex-col items-center gap-1 border transition ${
                        formType === t.value
                          ? 'bg-teal-50 border-teal-500 text-teal-800 shadow-xs'
                          : 'bg-slate-50 border-slate-200 text-slate-600 hover:bg-slate-100'
                      }`}
                    >
                      <t.icon className="w-4 h-4" />
                      <span>{t.label}</span>
                    </button>
                  ))}
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    Valid From <span className="text-rose-500">*</span>
                  </label>
                  <input
                    type="datetime-local"
                    value={formValidFrom}
                    onChange={(e) => setFormValidFrom(e.target.value)}
                    required
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 text-xs rounded-lg focus:outline-none"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    Valid Until <span className="text-rose-500">*</span>
                  </label>
                  <input
                    type="datetime-local"
                    value={formValidUntil}
                    onChange={(e) => setFormValidUntil(e.target.value)}
                    required
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 text-xs rounded-lg focus:outline-none"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    Purpose of Visit
                  </label>
                  <input
                    type="text"
                    value={formPurpose}
                    onChange={(e) => setFormPurpose(e.target.value)}
                    placeholder="e.g. Dinner party, package drop"
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 text-xs rounded-lg focus:outline-none"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    Vehicle Number (Optional)
                  </label>
                  <input
                    type="text"
                    value={formVehicle}
                    onChange={(e) => setFormVehicle(e.target.value)}
                    placeholder="e.g. MH12 AB 1234"
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 text-xs rounded-lg focus:outline-none"
                  />
                </div>
              </div>

              <div className="pt-2">
                <label className="flex items-center gap-2 cursor-pointer text-xs font-medium text-slate-700">
                  <input
                    type="checkbox"
                    checked={formIsOneTime}
                    onChange={(e) => setFormIsOneTime(e.target.checked)}
                    className="rounded text-teal-600 focus:ring-teal-500"
                  />
                  <span>One-time pass (Pass expires immediately upon exit)</span>
                </label>
              </div>

              <div className="flex items-center justify-end gap-2 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setShowPreApproveModal(false)}
                  className="px-4 py-2 border border-slate-200 hover:bg-slate-50 text-slate-700 text-xs font-semibold rounded-lg transition"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submittingForm}
                  className="px-5 py-2 bg-teal-600 hover:bg-teal-700 text-white text-xs font-semibold rounded-lg shadow-sm transition disabled:opacity-50 flex items-center gap-1"
                >
                  {submittingForm ? <RefreshCw className="w-3.5 h-3.5 animate-spin" /> : 'Generate Pass'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* MODAL: WALK-IN VISITOR REGISTRATION (GUARD)                               */}
      {/* ========================================================================= */}
      {showWalkInModal && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 overflow-y-auto">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-2xl space-y-4 my-8 border border-slate-100">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <div className="flex items-center gap-2">
                <div className="p-2 bg-slate-900 text-white rounded-lg">
                  <UserCheck className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-slate-900">Walk-In Visitor Entry</h3>
                  <p className="text-xs text-slate-500">
                    Sends real-time approval request to resident mobile
                  </p>
                </div>
              </div>
              <button
                onClick={() => setShowWalkInModal(false)}
                className="p-1.5 text-slate-400 hover:text-slate-600 rounded-lg hover:bg-slate-100"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleWalkInSubmit} className="space-y-4">
              {/* Destination Flat Selector */}
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    Select Tower <span className="text-rose-500">*</span>
                  </label>
                  <select
                    value={selectedTowerId}
                    onChange={(e) => setSelectedTowerId(e.target.value)}
                    className="w-full px-2.5 py-2 bg-slate-50 border border-slate-200 text-xs rounded-lg font-medium focus:outline-none"
                  >
                    {towers.map((t) => (
                      <option key={t.id} value={t.id}>
                        {t.id} - {t.name}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    Destination Flat <span className="text-rose-500">*</span>
                  </label>
                  <select
                    value={walkInFlatId}
                    onChange={(e) => setWalkInFlatId(e.target.value)}
                    required
                    className="w-full px-2.5 py-2 bg-slate-50 border border-slate-200 text-xs rounded-lg font-medium focus:outline-none"
                  >
                    {towerFlats.map((f) => (
                      <option key={f.id} value={f.id}>
                        Flat {f.flatNumber} (Floor {f.floorNumber})
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    Visitor Full Name <span className="text-rose-500">*</span>
                  </label>
                  <input
                    type="text"
                    value={walkInName}
                    onChange={(e) => setWalkInName(e.target.value)}
                    placeholder="e.g. Swiggy Delivery Partner"
                    required
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 text-xs rounded-lg focus:outline-none"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    Visitor Phone <span className="text-rose-500">*</span>
                  </label>
                  <input
                    type="tel"
                    value={walkInPhone}
                    onChange={(e) => setWalkInPhone(e.target.value)}
                    placeholder="e.g. +91 98220 99881"
                    required
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 text-xs rounded-lg focus:outline-none"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">Category</label>
                <div className="grid grid-cols-3 gap-2">
                  {VISITOR_TYPES.map((t) => (
                    <button
                      key={t.value}
                      type="button"
                      onClick={() => setWalkInType(t.value)}
                      className={`p-2 rounded-lg text-xs font-semibold flex flex-col items-center gap-1 border transition ${
                        walkInType === t.value
                          ? 'bg-slate-900 border-slate-900 text-white shadow-xs'
                          : 'bg-slate-50 border-slate-200 text-slate-600 hover:bg-slate-100'
                      }`}
                    >
                      <t.icon className="w-4 h-4" />
                      <span>{t.label}</span>
                    </button>
                  ))}
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    Purpose / Delivery Company
                  </label>
                  <input
                    type="text"
                    value={walkInPurpose}
                    onChange={(e) => setWalkInPurpose(e.target.value)}
                    placeholder="e.g. Swiggy Food Order #8821"
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 text-xs rounded-lg focus:outline-none"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    Vehicle Number (Optional)
                  </label>
                  <input
                    type="text"
                    value={walkInVehicle}
                    onChange={(e) => setWalkInVehicle(e.target.value)}
                    placeholder="e.g. MH12 AB 9988"
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 text-xs rounded-lg focus:outline-none"
                  />
                </div>
              </div>

              {/* Optional Photo Attachment */}
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">
                  Capture Visitor Photo (Max 5MB)
                </label>
                <input
                  type="file"
                  accept="image/jpeg,image/png,image/webp"
                  onChange={(e) => {
                    const f = e.target.files?.[0];
                    if (f) {
                      setWalkInPhoto(f);
                      setWalkInPhotoPreview(URL.createObjectURL(f));
                    }
                  }}
                  className="w-full text-xs text-slate-500 file:mr-2 file:py-1 file:px-3 file:rounded-md file:border-0 file:text-xs file:font-semibold file:bg-slate-100 file:text-slate-700 hover:file:bg-slate-200"
                />
                {walkInPhotoPreview && (
                  <img
                    src={walkInPhotoPreview}
                    alt="Preview"
                    className="mt-2 w-20 h-20 object-cover rounded-lg border border-slate-200"
                  />
                )}
              </div>

              <div className="flex items-center justify-end gap-2 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setShowWalkInModal(false)}
                  className="px-4 py-2 border border-slate-200 hover:bg-slate-50 text-slate-700 text-xs font-semibold rounded-lg transition"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submittingWalkIn}
                  className="px-5 py-2 bg-slate-900 hover:bg-slate-800 text-white text-xs font-semibold rounded-lg shadow-sm transition disabled:opacity-50 flex items-center gap-1"
                >
                  {submittingWalkIn ? (
                    <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                  ) : (
                    'Dispatch Approval Request'
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* MODAL: VISITOR PASS DETAILS & QR PREVIEW                                  */}
      {/* ========================================================================= */}
      {showPassModal && selectedVisitor && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 overflow-y-auto">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-2xl space-y-4 my-8 border border-slate-100">
            <div className="flex items-center justify-between pb-2 border-b border-slate-100">
              <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">
                Gated Township Visitor Pass
              </span>
              <button
                onClick={closePassModal}
                className="p-1.5 text-slate-400 hover:text-slate-600 rounded-lg hover:bg-slate-100"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Pass Graphic Card */}
            <div className="bg-gradient-to-br from-slate-900 via-slate-800 to-teal-950 p-5 rounded-2xl text-white shadow-md space-y-3">
              <div className="flex items-center justify-between">
                <div>
                  <div className="text-[10px] text-teal-300 font-bold uppercase tracking-wider">
                    Pune Grand Vista Gate Pass
                  </div>
                  <div className="text-xl font-black tracking-tight">{selectedVisitor.passCode}</div>
                </div>
                <div className="w-12 h-12 bg-white/10 rounded-xl border border-white/20 flex items-center justify-center">
                  <QrCode className="w-7 h-7 text-teal-300" />
                </div>
              </div>

              <div className="pt-2 border-t border-white/10 grid grid-cols-2 gap-2 text-xs">
                <div>
                  <span className="text-slate-400 text-[10px] block">Visitor</span>
                  <span className="font-bold">{selectedVisitor.visitorName}</span>
                </div>
                <div>
                  <span className="text-slate-400 text-[10px] block">Destination</span>
                  <span className="font-bold">
                    {selectedVisitor.towerName} • {selectedVisitor.flatNumber}
                  </span>
                </div>
                <div>
                  <span className="text-slate-400 text-[10px] block">Category</span>
                  <span className="font-semibold text-teal-300">{selectedVisitor.visitorType}</span>
                </div>
                <div>
                  <span className="text-slate-400 text-[10px] block">Status</span>
                  <span className="font-bold text-emerald-300">{selectedVisitor.status}</span>
                </div>
              </div>

              <div className="text-[10px] text-slate-300 border-t border-white/10 pt-2 font-mono flex items-center justify-between">
                <span>Valid: {new Date(selectedVisitor.validUntil).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                <span className="text-teal-400 font-bold">1-Time Entry</span>
              </div>
            </div>

            {/* Photo Preview if available */}
            {selectedVisitor.hasPhoto && detailPhotoUrl && (
              <div className="p-2 border border-slate-200 rounded-xl bg-slate-50 text-center">
                <span className="text-[10px] text-slate-500 block mb-1">Gate Photo Capture</span>
                <img
                  src={detailPhotoUrl}
                  alt="Visitor Photo"
                  className="max-h-40 rounded-lg mx-auto object-cover"
                />
              </div>
            )}

            {/* Pass Activity History */}
            {selectedVisitor.activities && selectedVisitor.activities.length > 0 && (
              <div className="space-y-1.5 pt-2">
                <span className="text-[10px] font-bold text-slate-500 uppercase tracking-wider block">
                  Gate Audit Trail
                </span>
                <div className="max-h-36 overflow-y-auto divide-y divide-slate-100 text-xs border border-slate-100 rounded-lg p-2 bg-slate-50">
                  {selectedVisitor.activities.map((a) => (
                    <div key={a.id} className="py-1">
                      <div className="flex items-center justify-between text-[10px] text-slate-500">
                        <strong className="text-slate-800">{a.actionType}</strong>
                        <span>{new Date(a.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                      </div>
                      <p className="text-[11px] text-slate-600">{a.details}</p>
                    </div>
                  ))}
                </div>
              </div>
            )}

            <div className="pt-2 text-center">
              <button
                onClick={closePassModal}
                className="w-full py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold rounded-lg transition"
              >
                Close Pass
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
