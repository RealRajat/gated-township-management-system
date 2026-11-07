import { useState, useEffect, useCallback, useRef } from 'react';
import {
  AlertCircle,
  AlertTriangle,
  CheckCircle2,
  Clock,
  Search,
  Filter,
  Plus,
  X,
  ShieldAlert,
  Sparkles,
  Bot,
  Star,
  Upload,
  Link2,
  Users,
  Wrench,
  Eye,
  RefreshCw,
  FileText,
  Flame,
  Zap,
  Lock,
  MessageSquare,
  Building,
  Image as ImageIcon,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { complaintApi, townshipApi } from '../services/api';
import StatCard from '../components/StatCard';

const CATEGORIES = [
  { value: 'PLUMBING', label: 'Plumbing' },
  { value: 'ELECTRICAL', label: 'Electrical' },
  { value: 'LIFT', label: 'Lift / Elevator' },
  { value: 'SECURITY', label: 'Security' },
  { value: 'HOUSEKEEPING', label: 'Housekeeping' },
  { value: 'PARKING', label: 'Parking' },
  { value: 'CIVIL_MAINTENANCE', label: 'Civil Maintenance' },
  { value: 'OTHER', label: 'Other' },
];

const PRIORITIES = [
  { value: 'LOW', label: 'Low (48h)' },
  { value: 'MEDIUM', label: 'Medium (24h)' },
  { value: 'HIGH', label: 'High (8h)' },
  { value: 'CRITICAL', label: 'Critical (2h)' },
];

const MAINTENANCE_TEAMS = [
  'Plumbing Quick-Response Team',
  'Electrical High-Voltage & Substation Crew',
  'Schindler Lift Maintenance & Emergency Team',
  'Township Security Command',
  'Housekeeping & Waste Management',
  'Civil Works & Structural Engineering',
  'Central Facility Help Desk',
];

export default function ComplaintsPage() {
  const { user, isHelpDesk } = useAuth();

  // Data states
  const [complaints, setComplaints] = useState([]);
  const [overview, setOverview] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  // Filters
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [categoryFilter, setCategoryFilter] = useState('ALL');
  const [priorityFilter, setPriorityFilter] = useState('ALL');
  const [overdueOnly, setOverdueOnly] = useState(false);

  // Active Modals / Drawers
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [selectedComplaintId, setSelectedComplaintId] = useState(null);
  const [detailData, setDetailData] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailPhotoUrl, setDetailPhotoUrl] = useState(null);

  // New Complaint Form State
  const [formFlatId, setFormFlatId] = useState('');
  const [formTitle, setFormTitle] = useState('');
  const [formDescription, setFormDescription] = useState('');
  const [formCategory, setFormCategory] = useState('');
  const [formPriority, setFormPriority] = useState('');
  const [formPhoto, setFormPhoto] = useState(null);
  const [photoPreviewUrl, setPhotoPreviewUrl] = useState(null);

  // AI Pre-Check State
  const [aiPreviewLoading, setAiPreviewLoading] = useState(false);
  const [aiPreviewResult, setAiPreviewResult] = useState(null);
  const [submittingComplaint, setSubmittingComplaint] = useState(false);

  // Help Desk Management Action States
  const [assignTeam, setAssignTeam] = useState('');
  const [assignStaffName, setAssignStaffName] = useState('');
  const [assignLoading, setAssignLoading] = useState(false);

  const [updateStatusVal, setUpdateStatusVal] = useState('');
  const [resolutionNotes, setResolutionNotes] = useState('');
  const [statusLoading, setStatusLoading] = useState(false);

  const [internalNoteText, setInternalNoteText] = useState('');
  const [internalNoteLoading, setInternalNoteLoading] = useState(false);

  // Resident Rating State
  const [ratingStars, setRatingStars] = useState(5);
  const [ratingFeedback, setRatingFeedback] = useState('');
  const [ratingLoading, setRatingLoading] = useState(false);

  // Resident flat options
  const userFlats = user?.flats || [];

  const loadData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      if (isHelpDesk) {
        const [complaintList, kpis] = await Promise.all([
          complaintApi.getAllComplaints({
            status: statusFilter,
            category: categoryFilter,
            priority: priorityFilter,
            overdueOnly,
          }),
          complaintApi.getOverview(),
        ]);
        setComplaints(complaintList || []);
        setOverview(kpis);
      } else {
        const myComplaints = await complaintApi.getMyComplaints();
        setComplaints(myComplaints || []);
      }
    } catch (err) {
      console.error('Failed to load complaints:', err);
      setError(err.message || 'Failed to load complaints');
    } finally {
      setLoading(false);
    }
  }, [isHelpDesk, statusFilter, categoryFilter, priorityFilter, overdueOnly]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Load detailed complaint when opened
  const openComplaintDetails = async (id) => {
    setSelectedComplaintId(id);
    setDetailLoading(true);
    setDetailData(null);
    if (detailPhotoUrl) {
      URL.revokeObjectURL(detailPhotoUrl);
      setDetailPhotoUrl(null);
    }
    try {
      const data = await complaintApi.getComplaintById(id);
      setDetailData(data);
      setUpdateStatusVal(data.status);
      setAssignTeam(data.assignedTeam || MAINTENANCE_TEAMS[0]);
      setResolutionNotes(data.resolutionNotes || '');

      // Load photo if available
      if (data.hasPhoto) {
        try {
          const blobUrl = await complaintApi.getPhotoBlobUrl(id);
          setDetailPhotoUrl(blobUrl);
        } catch (imgErr) {
          console.warn('Could not load photo preview:', imgErr);
        }
      }
    } catch (err) {
      console.error('Error fetching complaint details:', err);
      setError('Could not load complaint details: ' + (err.message || ''));
    } finally {
      setDetailLoading(false);
    }
  };

  const closeComplaintDetails = () => {
    setSelectedComplaintId(null);
    setDetailData(null);
    if (detailPhotoUrl) {
      URL.revokeObjectURL(detailPhotoUrl);
      setDetailPhotoUrl(null);
    }
  };

  // Run AI & Safety preview check
  const handleRunAiPreview = async () => {
    if (!formDescription.trim()) return;
    setAiPreviewLoading(true);
    try {
      const res = await complaintApi.previewClassification({
        title: formTitle || 'Resident Complaint',
        description: formDescription,
        category: formCategory || null,
        priority: formPriority || null,
      });
      setAiPreviewResult(res);
      // If critical safety escalation detected, automatically lock priority to CRITICAL
      if (res.isCriticalSafetyEscalation) {
        setFormPriority('CRITICAL');
        setFormCategory(res.suggestedCategory);
      } else if (!formCategory && res.suggestedCategory) {
        setFormCategory(res.suggestedCategory);
      }
    } catch (err) {
      console.warn('AI preview check failed:', err);
    } finally {
      setAiPreviewLoading(false);
    }
  };

  // Apply AI suggestions
  const applyAiSuggestions = () => {
    if (!aiPreviewResult) return;
    setFormCategory(aiPreviewResult.suggestedCategory);
    setFormPriority(aiPreviewResult.suggestedPriority);
  };

  // Handle Photo selection
  const handlePhotoChange = (e) => {
    const file = e.target.files?.[0];
    if (!file) {
      setFormPhoto(null);
      setPhotoPreviewUrl(null);
      return;
    }

    if (file.size > 5 * 1024 * 1024) {
      alert('Photo must be smaller than 5 MB.');
      e.target.value = '';
      return;
    }

    const allowed = ['image/jpeg', 'image/png', 'image/webp'];
    if (!allowed.includes(file.type)) {
      alert('Only JPG, PNG, and WebP image formats are permitted.');
      e.target.value = '';
      return;
    }

    setFormPhoto(file);
    setPhotoPreviewUrl(URL.createObjectURL(file));
  };

  // Create Complaint Submission
  const handleCreateComplaint = async (e) => {
    e.preventDefault();
    if (!formFlatId) {
      alert('Please select an apartment flat.');
      return;
    }
    if (!formTitle.trim() || !formDescription.trim()) {
      alert('Please enter a title and description.');
      return;
    }

    setSubmittingComplaint(true);
    try {
      let result;
      if (formPhoto) {
        const formData = new FormData();
        formData.append('flatId', formFlatId);
        formData.append('title', formTitle.trim());
        formData.append('description', formDescription.trim());
        if (formCategory) formData.append('category', formCategory);
        if (formPriority) formData.append('priority', formPriority);
        formData.append('photo', formPhoto);
        result = await complaintApi.createComplaint(formData, true);
      } else {
        result = await complaintApi.createComplaint({
          flatId: Number(formFlatId),
          title: formTitle.trim(),
          description: formDescription.trim(),
          category: formCategory || null,
          priority: formPriority || null,
        });
      }

      setSuccessMessage(`Complaint logged successfully: ${result.complaintNumber}`);
      setTimeout(() => setSuccessMessage(null), 5000);
      setShowCreateModal(false);
      resetCreateForm();
      loadData();
    } catch (err) {
      alert('Failed to log complaint: ' + (err.message || 'Unknown error'));
    } finally {
      setSubmittingComplaint(false);
    }
  };

  const resetCreateForm = () => {
    setFormFlatId(userFlats.length > 0 ? String(userFlats[0].flatId) : '');
    setFormTitle('');
    setFormDescription('');
    setFormCategory('');
    setFormPriority('');
    setFormPhoto(null);
    if (photoPreviewUrl) {
      URL.revokeObjectURL(photoPreviewUrl);
      setPhotoPreviewUrl(null);
    }
    setAiPreviewResult(null);
  };

  // Open modal with default flat
  const openCreateModal = () => {
    resetCreateForm();
    if (userFlats.length > 0) {
      setFormFlatId(String(userFlats[0].flatId));
    }
    setShowCreateModal(true);
  };

  // Help Desk: Assign Team
  const handleAssignSubmit = async (e) => {
    e.preventDefault();
    if (!assignTeam) return;
    setAssignLoading(true);
    try {
      const updated = await complaintApi.assignComplaint(selectedComplaintId, {
        assignedTeam: assignTeam,
        notes: `Assigned to ${assignTeam} by Help Desk operator.`,
      });
      setDetailData(updated);
      setSuccessMessage(`Assigned ticket ${updated.complaintNumber} to ${assignTeam}`);
      setTimeout(() => setSuccessMessage(null), 4000);
      loadData();
    } catch (err) {
      alert('Failed to assign team: ' + (err.message || ''));
    } finally {
      setAssignLoading(false);
    }
  };

  // Help Desk: Update Status
  const handleStatusSubmit = async (e) => {
    e.preventDefault();
    if (!updateStatusVal) return;
    setStatusLoading(true);
    try {
      const updated = await complaintApi.updateComplaintStatus(selectedComplaintId, {
        newStatus: updateStatusVal,
        resolutionNotes: resolutionNotes.trim() || null,
      });
      setDetailData(updated);
      setSuccessMessage(`Updated ticket ${updated.complaintNumber} to ${updateStatusVal}`);
      setTimeout(() => setSuccessMessage(null), 4000);
      loadData();
    } catch (err) {
      alert('Failed to update status: ' + (err.message || ''));
    } finally {
      setStatusLoading(false);
    }
  };

  // Help Desk: Add Internal Note
  const handleInternalNoteSubmit = async (e) => {
    e.preventDefault();
    if (!internalNoteText.trim()) return;
    setInternalNoteLoading(true);
    try {
      const updated = await complaintApi.addInternalNote(selectedComplaintId, {
        internalNote: internalNoteText.trim(),
      });
      setDetailData(updated);
      setInternalNoteText('');
      setSuccessMessage('Internal staff note added.');
      setTimeout(() => setSuccessMessage(null), 3000);
    } catch (err) {
      alert('Failed to add internal note: ' + (err.message || ''));
    } finally {
      setInternalNoteLoading(false);
    }
  };

  // Help Desk: Link Duplicate
  const handleLinkDuplicate = async (masterComplaintId) => {
    if (!confirm('Link this complaint as a duplicate? Note: Neither complaint will be closed or deleted.')) {
      return;
    }
    try {
      const updated = await complaintApi.linkDuplicate(selectedComplaintId, {
        masterComplaintId: Number(masterComplaintId),
        notes: 'Identified as duplicate report in same tower.',
      });
      setDetailData(updated);
      setSuccessMessage(`Linked as duplicate to master #${masterComplaintId}`);
      setTimeout(() => setSuccessMessage(null), 4000);
      loadData();
    } catch (err) {
      alert('Failed to link duplicate: ' + (err.message || ''));
    }
  };

  // Resident: Submit Rating
  const handleRateSubmit = async (e) => {
    e.preventDefault();
    setRatingLoading(true);
    try {
      const updated = await complaintApi.rateComplaint(selectedComplaintId, {
        rating: ratingStars,
        feedback: ratingFeedback.trim() || null,
      });
      setDetailData(updated);
      setSuccessMessage('Thank you! Your rating has been recorded.');
      setTimeout(() => setSuccessMessage(null), 4000);
      loadData();
    } catch (err) {
      alert('Failed to submit rating: ' + (err.message || ''));
    } finally {
      setRatingLoading(false);
    }
  };

  // Filter complaints in memory for search term
  const filteredComplaints = complaints.filter((c) => {
    if (!searchTerm.trim()) return true;
    const term = searchTerm.toLowerCase();
    const num = (c.complaintNumber || '').toLowerCase();
    const title = (c.title || '').toLowerCase();
    const resident = (c.residentName || '').toLowerCase();
    const tower = (c.towerName || '').toLowerCase();
    const flat = (c.flatNumber || '').toLowerCase();
    return (
      num.includes(term) ||
      title.includes(term) ||
      resident.includes(term) ||
      tower.includes(term) ||
      flat.includes(term)
    );
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

      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h1 className="text-2xl font-bold text-slate-900 tracking-tight">
              {isHelpDesk ? 'Township Help Desk Queue' : 'My Complaints & Service Requests'}
            </h1>
            <span
              className={`text-[11px] font-bold px-2 py-0.5 rounded-full ${
                isHelpDesk
                  ? 'bg-indigo-50 text-indigo-700 border border-indigo-200'
                  : 'bg-teal-50 text-teal-700 border border-teal-200'
              }`}
            >
              {isHelpDesk ? 'Staff Operations' : 'Resident Portal'}
            </span>
          </div>
          <p className="text-xs sm:text-sm text-slate-500 mt-0.5">
            {isHelpDesk
              ? 'Triage resident tickets, dispatch maintenance teams, monitor emergency safety escalations, and manage resolution SLAs.'
              : 'File maintenance issues, monitor technician dispatch in real-time, and rate service quality.'}
          </p>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={loadData}
            title="Refresh Complaints"
            className="p-2 border border-slate-200 hover:bg-slate-50 text-slate-600 rounded-lg transition"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          </button>

          <button
            onClick={openCreateModal}
            className="inline-flex items-center gap-1.5 px-4 py-2 bg-teal-600 hover:bg-teal-700 text-white rounded-lg text-xs font-semibold shadow-sm transition"
          >
            <Plus className="w-4 h-4" />
            Log New Complaint
          </button>
        </div>
      </div>

      {/* KPI Cards */}
      {isHelpDesk && overview ? (
        <div className="grid grid-cols-2 lg:grid-cols-6 gap-3">
          <StatCard
            title="Total Tickets"
            value={overview.totalComplaints || 0}
            subtext="All 28 towers"
            icon={FileText}
            colorScheme="slate"
          />
          <StatCard
            title="Open Tickets"
            value={overview.openComplaints || 0}
            subtext="Awaiting work"
            icon={AlertCircle}
            colorScheme="amber"
          />
          <div className="bg-rose-50 border-2 border-rose-400 rounded-xl p-4 shadow-sm relative overflow-hidden">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-rose-800 uppercase tracking-wider">
                Critical Safety
              </span>
              <ShieldAlert className="w-5 h-5 text-rose-600 animate-pulse" />
            </div>
            <div className="text-2xl font-black text-rose-900 mt-2">
              {overview.criticalSafetyComplaints || 0}
            </div>
            <p className="text-[11px] font-semibold text-rose-700 mt-1">
              Immediate 15m SLA
            </p>
          </div>
          <StatCard
            title="In Progress"
            value={overview.inProgressComplaints || 0}
            subtext="Field crews active"
            icon={Wrench}
            colorScheme="teal"
          />
          <div className="bg-orange-50 border border-orange-200 rounded-xl p-4 shadow-sm">
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold text-orange-800 uppercase tracking-wider">
                Overdue
              </span>
              <Clock className="w-5 h-5 text-orange-600" />
            </div>
            <div className="text-2xl font-black text-orange-900 mt-2">
              {overview.overdueComplaints || 0}
            </div>
            <p className="text-[11px] font-medium text-orange-700 mt-1">
              Resolution breach
            </p>
          </div>
          <StatCard
            title="Resolved / Closed"
            value={overview.resolvedComplaints || 0}
            subtext="Closed tickets"
            icon={CheckCircle2}
            colorScheme="emerald"
          />
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div className="bg-white rounded-xl border border-slate-200 p-4 shadow-sm flex items-center gap-3">
            <div className="p-3 bg-teal-50 text-teal-600 rounded-xl">
              <AlertCircle className="w-6 h-6" />
            </div>
            <div>
              <div className="text-xs text-slate-500 font-medium">My Active Complaints</div>
              <div className="text-xl font-bold text-slate-900">
                {complaints.filter((c) => c.status !== 'RESOLVED' && c.status !== 'CLOSED').length}
              </div>
            </div>
          </div>

          <div className="bg-white rounded-xl border border-slate-200 p-4 shadow-sm flex items-center gap-3">
            <div className="p-3 bg-emerald-50 text-emerald-600 rounded-xl">
              <CheckCircle2 className="w-6 h-6" />
            </div>
            <div>
              <div className="text-xs text-slate-500 font-medium">Resolved / Completed</div>
              <div className="text-xl font-bold text-slate-900">
                {complaints.filter((c) => c.status === 'RESOLVED' || c.status === 'CLOSED').length}
              </div>
            </div>
          </div>

          <div className="bg-rose-50 border border-rose-200 rounded-xl p-4 shadow-sm flex items-center gap-3">
            <div className="p-3 bg-rose-100 text-rose-700 rounded-xl">
              <ShieldAlert className="w-6 h-6" />
            </div>
            <div>
              <div className="text-xs text-rose-800 font-bold uppercase tracking-wider">
                Emergency Hotline
              </div>
              <div className="text-sm font-bold text-rose-900">
                Gate 1 Control: +91 20 6712 9999
              </div>
            </div>
          </div>
        </div>
      )}

      {/* AI Engine & Safety Banner */}
      <div className="bg-gradient-to-r from-slate-900 via-indigo-950 to-slate-900 rounded-xl p-5 text-white shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div className="flex items-start gap-3.5">
          <div className="p-2.5 rounded-xl bg-violet-600/30 border border-violet-400/30 text-violet-300 shrink-0">
            <Bot className="w-6 h-6 text-violet-300" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="font-bold text-sm text-white">
                Intelligent AI Triage & Safety Rule Engine
              </span>
              <span className="text-[10px] bg-violet-500/20 text-violet-300 border border-violet-400/30 px-2 py-0.5 rounded font-mono">
                Active
              </span>
            </div>
            <p className="text-xs text-slate-300 mt-1 max-w-2xl leading-relaxed">
              Resident reports are processed through NLP semantic classification with automatic detection of
              critical safety hazards (fire, gas leaks, trapped elevator, electrical sparking). Critical hazards
              lock urgency to emergency priority and trigger priority team dispatch.
            </p>
          </div>
        </div>
        <div className="shrink-0 flex items-center gap-2">
          <span className="text-xs text-emerald-400 font-mono bg-emerald-950/60 border border-emerald-700/40 px-3 py-1.5 rounded-lg flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-ping" />
            Safety Override Active
          </span>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="bg-white rounded-xl border border-slate-200/90 shadow-sm p-4 flex flex-col lg:flex-row lg:items-center justify-between gap-3">
        <div className="relative flex-1">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Search by ticket #, title, resident name, flat or tower..."
            className="w-full pl-9 pr-3 py-2 bg-slate-50 border border-slate-200 text-xs rounded-lg focus:outline-none focus:border-teal-500 focus:bg-white transition"
          />
        </div>

        {isHelpDesk && (
          <div className="flex flex-wrap items-center gap-2">
            {/* Status Filter */}
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="px-2.5 py-1.5 bg-slate-50 border border-slate-200 text-xs rounded-lg text-slate-700 font-medium focus:outline-none"
            >
              <option value="ALL">All Statuses</option>
              <option value="SUBMITTED">Submitted</option>
              <option value="ASSIGNED">Assigned</option>
              <option value="IN_PROGRESS">In Progress</option>
              <option value="ON_HOLD">On Hold</option>
              <option value="RESOLVED">Resolved</option>
              <option value="CLOSED">Closed</option>
            </select>

            {/* Category Filter */}
            <select
              value={categoryFilter}
              onChange={(e) => setCategoryFilter(e.target.value)}
              className="px-2.5 py-1.5 bg-slate-50 border border-slate-200 text-xs rounded-lg text-slate-700 font-medium focus:outline-none"
            >
              <option value="ALL">All Categories</option>
              {CATEGORIES.map((c) => (
                <option key={c.value} value={c.value}>
                  {c.label}
                </option>
              ))}
            </select>

            {/* Priority Filter */}
            <select
              value={priorityFilter}
              onChange={(e) => setPriorityFilter(e.target.value)}
              className="px-2.5 py-1.5 bg-slate-50 border border-slate-200 text-xs rounded-lg text-slate-700 font-medium focus:outline-none"
            >
              <option value="ALL">All Priorities</option>
              <option value="CRITICAL">Critical</option>
              <option value="HIGH">High</option>
              <option value="MEDIUM">Medium</option>
              <option value="LOW">Low</option>
            </select>

            {/* Overdue Toggle */}
            <label className="flex items-center gap-1.5 text-xs text-slate-600 bg-slate-50 border border-slate-200 px-3 py-1.5 rounded-lg cursor-pointer">
              <input
                type="checkbox"
                checked={overdueOnly}
                onChange={(e) => setOverdueOnly(e.target.checked)}
                className="rounded text-rose-600 focus:ring-rose-500"
              />
              <span className="font-semibold text-rose-700">Overdue SLA</span>
            </label>
          </div>
        )}
      </div>

      {/* Complaints Queue List */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="p-4 sm:p-5 border-b border-slate-100 flex items-center justify-between">
          <div>
            <h2 className="text-sm font-bold text-slate-900">
              {isHelpDesk ? 'Complaints Triage Queue' : 'My Registered Complaints'}
            </h2>
            <span className="text-xs text-slate-400">
              Showing {filteredComplaints.length} tickets
            </span>
          </div>
        </div>

        {loading ? (
          <div className="p-12 text-center text-slate-400 text-sm flex flex-col items-center gap-3">
            <RefreshCw className="w-6 h-6 animate-spin text-teal-600" />
            <span>Loading complaints from server...</span>
          </div>
        ) : filteredComplaints.length === 0 ? (
          <div className="p-12 text-center">
            <CheckCircle2 className="w-10 h-10 text-slate-300 mx-auto mb-2" />
            <p className="text-sm font-semibold text-slate-700">No complaints found</p>
            <p className="text-xs text-slate-400 mt-1">
              {isHelpDesk
                ? 'All queues are clear or no tickets match your search filters.'
                : 'You currently have no registered complaints.'}
            </p>
          </div>
        ) : (
          <div className="divide-y divide-slate-100">
            {filteredComplaints.map((item) => {
              const isCritical = item.isCriticalSafetyEscalation || item.priority === 'CRITICAL';
              const isOverdue = item.isOverdue;

              return (
                <div
                  key={item.id}
                  className={`p-4 sm:p-5 hover:bg-slate-50/70 transition flex flex-col md:flex-row md:items-center justify-between gap-4 ${
                    isCritical ? 'bg-rose-50/40 border-l-4 border-l-rose-500' : ''
                  }`}
                >
                  <div className="space-y-2 flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="font-mono text-xs font-bold text-slate-800 bg-slate-100 px-2.5 py-0.5 rounded border border-slate-200">
                        {item.complaintNumber}
                      </span>

                      <span className="font-semibold text-xs text-slate-900 bg-slate-50 px-2 py-0.5 rounded border border-slate-200 flex items-center gap-1">
                        <Building className="w-3 h-3 text-slate-400" />
                        {item.towerName} • Flat {item.flatNumber}
                      </span>

                      {/* Safety Alert Badge */}
                      {isCritical && (
                        <span className="text-[10px] font-black px-2 py-0.5 rounded-full bg-rose-600 text-white flex items-center gap-1 animate-pulse">
                          <ShieldAlert className="w-3 h-3" />
                          CRITICAL SAFETY
                          {item.safetyTriggerKeyword ? ` (${item.safetyTriggerKeyword})` : ''}
                        </span>
                      )}

                      {/* Category Badge */}
                      <span className="text-[10px] font-semibold px-2 py-0.5 rounded bg-slate-100 text-slate-700 border border-slate-200">
                        {item.category}
                      </span>

                      {/* Priority Badge */}
                      <span
                        className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                          item.priority === 'CRITICAL'
                            ? 'bg-rose-100 text-rose-800 border border-rose-300'
                            : item.priority === 'HIGH'
                            ? 'bg-amber-100 text-amber-800 border border-amber-300'
                            : item.priority === 'MEDIUM'
                            ? 'bg-blue-100 text-blue-800 border border-blue-300'
                            : 'bg-slate-100 text-slate-700 border border-slate-200'
                        }`}
                      >
                        {item.priority}
                      </span>

                      {/* Overdue Badge */}
                      {isOverdue && (
                        <span className="text-[10px] font-black px-2 py-0.5 rounded bg-red-100 text-red-800 border border-red-300 flex items-center gap-1">
                          <Clock className="w-3 h-3 text-red-600" />
                          OVERDUE SLA
                        </span>
                      )}

                      {/* Has Photo Badge */}
                      {item.hasPhoto && (
                        <span className="text-[10px] font-medium px-2 py-0.5 rounded bg-indigo-50 text-indigo-700 border border-indigo-200 flex items-center gap-1">
                          <ImageIcon className="w-3 h-3" />
                          Photo
                        </span>
                      )}

                      {/* Duplicate Link Badge */}
                      {item.parentDuplicateOfId && (
                        <span className="text-[10px] font-medium px-2 py-0.5 rounded bg-amber-50 text-amber-800 border border-amber-200 flex items-center gap-1">
                          <Link2 className="w-3 h-3" />
                          Duplicate of #{item.parentDuplicateComplaintNumber || item.parentDuplicateOfId}
                        </span>
                      )}
                    </div>

                    <h3 className="text-sm font-semibold text-slate-900">{item.title}</h3>

                    <div className="flex flex-wrap items-center gap-3 text-xs text-slate-400">
                      <span>
                        Resident: <strong className="text-slate-700">{item.residentName}</strong>
                      </span>
                      <span>•</span>
                      <span>
                        Assigned:{' '}
                        <span className="text-teal-700 font-medium">
                          {item.assignedTeam || 'Unassigned'}
                        </span>
                      </span>
                      <span>•</span>
                      <span>
                        Filed: {item.createdAt ? new Date(item.createdAt).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' }) : 'Recently'}
                      </span>

                      {item.residentRating && (
                        <>
                          <span>•</span>
                          <span className="flex items-center gap-1 text-amber-600 font-bold">
                            <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-400" />
                            {item.residentRating}/5
                          </span>
                        </>
                      )}
                    </div>
                  </div>

                  <div className="flex items-center gap-3 shrink-0 self-start md:self-center">
                    <span
                      className={`inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold ${
                        item.status === 'RESOLVED' || item.status === 'CLOSED'
                          ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                          : item.status === 'IN_PROGRESS'
                          ? 'bg-sky-50 text-sky-700 border border-sky-200'
                          : item.status === 'ASSIGNED'
                          ? 'bg-indigo-50 text-indigo-700 border border-indigo-200'
                          : item.status === 'ON_HOLD'
                          ? 'bg-amber-50 text-amber-700 border border-amber-200'
                          : 'bg-slate-100 text-slate-700 border border-slate-200'
                      }`}
                    >
                      {item.status}
                    </span>

                    <button
                      onClick={() => openComplaintDetails(item.id)}
                      className="px-3.5 py-1.5 bg-slate-900 hover:bg-slate-800 text-white text-xs font-semibold rounded-lg transition flex items-center gap-1.5 shadow-sm"
                    >
                      <Eye className="w-3.5 h-3.5" />
                      {isHelpDesk ? 'Manage & Dispatch' : 'View Details'}
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* ========================================================================= */}
      {/* MODAL: LOG NEW COMPLAINT                                                  */}
      {/* ========================================================================= */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 overflow-y-auto">
          <div className="bg-white rounded-2xl max-w-2xl w-full p-6 shadow-2xl space-y-5 my-8 border border-slate-100">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <div className="flex items-center gap-2">
                <div className="p-2 bg-teal-50 text-teal-600 rounded-lg">
                  <FileText className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="text-base font-bold text-slate-900">Log Maintenance Complaint</h3>
                  <p className="text-xs text-slate-500">
                    Pune Grand Vista Facility Management System
                  </p>
                </div>
              </div>
              <button
                onClick={() => setShowCreateModal(false)}
                className="p-1.5 text-slate-400 hover:text-slate-600 rounded-lg hover:bg-slate-100"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCreateComplaint} className="space-y-4">
              {/* Flat Selector */}
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">
                  Apartment Flat <span className="text-rose-500">*</span>
                </label>
                {userFlats.length > 0 ? (
                  <select
                    value={formFlatId}
                    onChange={(e) => setFormFlatId(e.target.value)}
                    required
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 text-xs rounded-lg focus:outline-none focus:border-teal-500 font-medium"
                  >
                    {userFlats.map((f) => (
                      <option key={f.flatId} value={f.flatId}>
                        Tower {f.towerName} ({f.towerId}) — Flat {f.flatNumber} ({f.areaSqFt} sq ft)
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
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 text-xs rounded-lg focus:outline-none focus:border-teal-500"
                  />
                )}
              </div>

              {/* Title */}
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">
                  Complaint Title <span className="text-rose-500">*</span>
                </label>
                <input
                  type="text"
                  value={formTitle}
                  onChange={(e) => setFormTitle(e.target.value)}
                  placeholder="e.g. Passenger elevator stuck on 12th floor with resident inside"
                  required
                  maxLength={150}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 text-xs rounded-lg focus:outline-none focus:border-teal-500"
                />
              </div>

              {/* Description */}
              <div>
                <div className="flex items-center justify-between mb-1">
                  <label className="block text-xs font-bold text-slate-700">
                    Detailed Description <span className="text-rose-500">*</span>
                  </label>
                  <button
                    type="button"
                    onClick={handleRunAiPreview}
                    disabled={aiPreviewLoading || !formDescription.trim()}
                    className="text-[11px] font-semibold text-violet-700 hover:text-violet-900 flex items-center gap-1 disabled:opacity-50"
                  >
                    <Sparkles className="w-3.5 h-3.5" />
                    {aiPreviewLoading ? 'Analyzing...' : 'AI Pre-Check & Safety Audit'}
                  </button>
                </div>
                <textarea
                  value={formDescription}
                  onChange={(e) => setFormDescription(e.target.value)}
                  onBlur={() => {
                    if (formDescription.trim() && !aiPreviewResult) {
                      handleRunAiPreview();
                    }
                  }}
                  rows={3}
                  required
                  placeholder="Describe the issue in detail. Safety hazards (fire, gas leaks, lift entrapment, electrical sparking) trigger instant emergency escalation."
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 text-xs rounded-lg focus:outline-none focus:border-teal-500"
                />
              </div>

              {/* AI & Safety Analysis Result Box */}
              {aiPreviewResult && (
                <div
                  className={`p-3.5 rounded-xl border text-xs space-y-2 ${
                    aiPreviewResult.isCriticalSafetyEscalation
                      ? 'bg-rose-50 border-rose-300 text-rose-900'
                      : 'bg-violet-50/70 border-violet-200 text-violet-900'
                  }`}
                >
                  <div className="flex items-start justify-between gap-2">
                    <div className="flex items-center gap-1.5 font-bold">
                      {aiPreviewResult.isCriticalSafetyEscalation ? (
                        <>
                          <ShieldAlert className="w-4 h-4 text-rose-600 animate-pulse shrink-0" />
                          <span className="text-rose-700 uppercase tracking-wide">
                            🚨 Critical Safety Override Triggered!
                          </span>
                        </>
                      ) : (
                        <>
                          <Bot className="w-4 h-4 text-violet-600 shrink-0" />
                          <span>AI Classification Suggestions</span>
                        </>
                      )}
                    </div>

                    {!aiPreviewResult.isCriticalSafetyEscalation && (
                      <button
                        type="button"
                        onClick={applyAiSuggestions}
                        className="text-[10px] font-bold px-2 py-0.5 bg-violet-600 hover:bg-violet-700 text-white rounded shadow-xs"
                      >
                        Apply Suggestions
                      </button>
                    )}
                  </div>

                  <p className="text-[11px] leading-relaxed">
                    {aiPreviewResult.aiReasoning}
                  </p>

                  <div className="flex flex-wrap items-center gap-2 pt-1 font-mono text-[10px]">
                    <span className="px-2 py-0.5 bg-white/80 rounded border">
                      Category: <strong>{aiPreviewResult.suggestedCategory}</strong>
                    </span>
                    <span className="px-2 py-0.5 bg-white/80 rounded border">
                      Urgency: <strong>{aiPreviewResult.suggestedPriority}</strong>
                    </span>
                    <span className="px-2 py-0.5 bg-white/80 rounded border">
                      Confidence: {aiPreviewResult.confidenceScore}%
                    </span>
                    <span className="px-2 py-0.5 bg-white/80 rounded border">
                      Classifier: {aiPreviewResult.classifierSource}
                    </span>
                  </div>
                </div>
              )}

              {/* Category & Priority Row */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    Category (Optional / Auto-suggested)
                  </label>
                  <select
                    value={formCategory}
                    onChange={(e) => setFormCategory(e.target.value)}
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 text-xs rounded-lg focus:outline-none focus:border-teal-500 font-medium"
                  >
                    <option value="">Auto-Detect from Description</option>
                    {CATEGORIES.map((c) => (
                      <option key={c.value} value={c.value}>
                        {c.label}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 mb-1">
                    Urgency Priority
                  </label>
                  <select
                    value={formPriority}
                    onChange={(e) => setFormPriority(e.target.value)}
                    disabled={aiPreviewResult?.isCriticalSafetyEscalation}
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 text-xs rounded-lg focus:outline-none focus:border-teal-500 font-medium disabled:bg-rose-100 disabled:text-rose-900"
                  >
                    <option value="">Auto-Detect from Description</option>
                    {PRIORITIES.map((p) => (
                      <option key={p.value} value={p.value}>
                        {p.label}
                      </option>
                    ))}
                  </select>
                  {aiPreviewResult?.isCriticalSafetyEscalation && (
                    <span className="text-[10px] text-rose-600 font-semibold block mt-0.5">
                      Locked to CRITICAL due to safety keyword detection.
                    </span>
                  )}
                </div>
              </div>

              {/* Optional Photo Attachment */}
              <div>
                <label className="block text-xs font-bold text-slate-700 mb-1">
                  Optional Photo Attachment (Max 5 MB, JPG/PNG/WebP)
                </label>
                <div className="border-2 border-dashed border-slate-200 rounded-xl p-3 text-center hover:border-slate-300 transition">
                  <input
                    type="file"
                    accept="image/jpeg,image/png,image/webp"
                    onChange={handlePhotoChange}
                    id="complaint-photo-input"
                    className="hidden"
                  />
                  <label
                    htmlFor="complaint-photo-input"
                    className="cursor-pointer flex flex-col items-center justify-center gap-1.5"
                  >
                    <Upload className="w-5 h-5 text-slate-400" />
                    <span className="text-xs font-semibold text-teal-700 hover:text-teal-800">
                      {formPhoto ? formPhoto.name : 'Click to select or capture a photo'}
                    </span>
                    <span className="text-[10px] text-slate-400">
                      Stored in secure isolated facility media storage.
                    </span>
                  </label>
                </div>

                {photoPreviewUrl && (
                  <div className="mt-2 relative inline-block">
                    <img
                      src={photoPreviewUrl}
                      alt="Complaint preview"
                      className="w-24 h-24 object-cover rounded-lg border border-slate-200 shadow-xs"
                    />
                    <button
                      type="button"
                      onClick={() => {
                        setFormPhoto(null);
                        setPhotoPreviewUrl(null);
                      }}
                      className="absolute -top-1.5 -right-1.5 p-1 bg-rose-600 text-white rounded-full hover:bg-rose-700"
                    >
                      <X className="w-3 h-3" />
                    </button>
                  </div>
                )}
              </div>

              {/* Form Action Buttons */}
              <div className="flex items-center justify-end gap-2 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="px-4 py-2 border border-slate-200 hover:bg-slate-50 text-slate-700 text-xs font-semibold rounded-lg transition"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submittingComplaint}
                  className="px-5 py-2 bg-teal-600 hover:bg-teal-700 text-white text-xs font-semibold rounded-lg shadow-sm transition disabled:opacity-50 flex items-center gap-1.5"
                >
                  {submittingComplaint ? (
                    <>
                      <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                      Filing Ticket...
                    </>
                  ) : (
                    'Submit Complaint'
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ========================================================================= */}
      {/* DRAWER / MODAL: COMPLAINT DETAILS & MANAGEMENT                            */}
      {/* ========================================================================= */}
      {selectedComplaintId && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4 overflow-y-auto">
          <div className="bg-white rounded-2xl max-w-3xl w-full p-6 shadow-2xl space-y-6 my-8 border border-slate-100 max-h-[90vh] overflow-y-auto">
            {/* Drawer Header */}
            <div className="flex items-start justify-between pb-4 border-b border-slate-100">
              <div className="space-y-1">
                <div className="flex flex-wrap items-center gap-2">
                  <span className="font-mono text-xs font-bold text-slate-800 bg-slate-100 px-2.5 py-0.5 rounded border">
                    {detailData?.complaintNumber || 'CMP-DETAIL'}
                  </span>
                  <span className="font-bold text-xs text-slate-900">
                    {detailData?.towerName} • Flat {detailData?.flatNumber}
                  </span>
                  {detailData?.isCriticalSafetyEscalation && (
                    <span className="text-[10px] font-black px-2 py-0.5 rounded-full bg-rose-600 text-white animate-pulse">
                      CRITICAL SAFETY ESCALATION
                    </span>
                  )}
                  {detailData?.isOverdue && (
                    <span className="text-[10px] font-black px-2 py-0.5 rounded bg-red-100 text-red-800 border border-red-300">
                      OVERDUE SLA
                    </span>
                  )}
                </div>
                <h2 className="text-base font-bold text-slate-900">{detailData?.title}</h2>
              </div>
              <button
                onClick={closeComplaintDetails}
                className="p-1.5 text-slate-400 hover:text-slate-600 rounded-lg hover:bg-slate-100"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {detailLoading ? (
              <div className="p-12 text-center text-slate-400 text-sm flex flex-col items-center gap-3">
                <RefreshCw className="w-6 h-6 animate-spin text-teal-600" />
                <span>Loading ticket details...</span>
              </div>
            ) : detailData ? (
              <div className="space-y-6">
                {/* Critical Safety Notice if applicable */}
                {detailData.isCriticalSafetyEscalation && (
                  <div className="p-4 bg-rose-50 border-2 border-rose-300 rounded-xl text-rose-900 flex items-start gap-3">
                    <ShieldAlert className="w-6 h-6 text-rose-600 shrink-0 mt-0.5" />
                    <div>
                      <h4 className="font-bold text-xs uppercase tracking-wide text-rose-900">
                        Emergency Safety Keyword Triggered: {detailData.safetyTriggerKeyword}
                      </h4>
                      <p className="text-xs text-rose-800 mt-1">
                        This issue was flagged as an imminent life safety or property hazard. Urgency is locked
                        at CRITICAL with immediate 15-minute response SLA. AI models cannot downgrade this status.
                      </p>
                    </div>
                  </div>
                )}

                {/* Complaint Overview Grid */}
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 bg-slate-50 p-3.5 rounded-xl border border-slate-100 text-xs">
                  <div>
                    <span className="text-slate-400 block text-[10px] uppercase font-bold">Category</span>
                    <span className="font-semibold text-slate-800">{detailData.category}</span>
                  </div>
                  <div>
                    <span className="text-slate-400 block text-[10px] uppercase font-bold">Priority</span>
                    <span
                      className={`font-bold ${
                        detailData.priority === 'CRITICAL' ? 'text-rose-600' : 'text-slate-800'
                      }`}
                    >
                      {detailData.priority}
                    </span>
                  </div>
                  <div>
                    <span className="text-slate-400 block text-[10px] uppercase font-bold">Status</span>
                    <span className="font-semibold text-teal-700">{detailData.status}</span>
                  </div>
                  <div>
                    <span className="text-slate-400 block text-[10px] uppercase font-bold">Assigned Team</span>
                    <span className="font-semibold text-slate-800">
                      {detailData.assignedTeam || 'Unassigned'}
                    </span>
                  </div>
                  <div>
                    <span className="text-slate-400 block text-[10px] uppercase font-bold">Resident</span>
                    <span className="font-semibold text-slate-800">{detailData.residentName}</span>
                  </div>
                  <div>
                    <span className="text-slate-400 block text-[10px] uppercase font-bold">Phone</span>
                    <span className="font-semibold text-slate-800">
                      {detailData.residentPhone || 'Not provided'}
                    </span>
                  </div>
                  <div>
                    <span className="text-slate-400 block text-[10px] uppercase font-bold">Filed Date</span>
                    <span className="font-semibold text-slate-800">
                      {new Date(detailData.createdAt).toLocaleDateString('en-IN', {
                        day: '2-digit',
                        month: 'short',
                        hour: '2-digit',
                        minute: '2-digit',
                      })}
                    </span>
                  </div>
                  <div>
                    <span className="text-slate-400 block text-[10px] uppercase font-bold">
                      Resolution SLA
                    </span>
                    <span
                      className={`font-semibold ${
                        detailData.isOverdue ? 'text-red-600 font-black' : 'text-slate-800'
                      }`}
                    >
                      {detailData.resolutionDeadline
                        ? new Date(detailData.resolutionDeadline).toLocaleDateString('en-IN', {
                            day: '2-digit',
                            month: 'short',
                            hour: '2-digit',
                            minute: '2-digit',
                          })
                        : 'Standard SLA'}
                    </span>
                  </div>
                </div>

                {/* Description */}
                <div>
                  <h4 className="text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
                    Resident Description
                  </h4>
                  <div className="p-3 bg-slate-50 border border-slate-200 rounded-xl text-xs text-slate-800 whitespace-pre-wrap leading-relaxed">
                    {detailData.description}
                  </div>
                </div>

                {/* Photo Attachment View */}
                {detailData.hasPhoto && (
                  <div>
                    <h4 className="text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5 flex items-center gap-1.5">
                      <ImageIcon className="w-3.5 h-3.5" />
                      Attached Photo Proof
                    </h4>
                    {detailPhotoUrl ? (
                      <div className="border border-slate-200 rounded-xl p-2 bg-slate-50 inline-block">
                        <img
                          src={detailPhotoUrl}
                          alt="Complaint attachment"
                          className="max-h-60 rounded-lg object-contain shadow-xs"
                        />
                        <div className="text-[10px] text-slate-400 mt-1">
                          {detailData.photoOriginalFilename} ({(detailData.photoSizeBytes / 1024).toFixed(1)} KB)
                        </div>
                      </div>
                    ) : (
                      <div className="p-3 bg-slate-50 rounded-lg text-xs text-slate-400">
                        Loading secure photo attachment...
                      </div>
                    )}
                  </div>
                )}

                {/* Public Resolution Notes */}
                {detailData.resolutionNotes && (
                  <div>
                    <h4 className="text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
                      Facility Resolution Notes
                    </h4>
                    <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-xl text-xs text-emerald-900 leading-relaxed">
                      {detailData.resolutionNotes}
                    </div>
                  </div>
                )}

                {/* Resident Rating Section */}
                {!isHelpDesk &&
                  (detailData.status === 'RESOLVED' || detailData.status === 'CLOSED') && (
                    <div className="p-4 bg-amber-50/60 border border-amber-200 rounded-xl space-y-3">
                      <div className="flex items-center gap-1.5">
                        <Star className="w-4 h-4 fill-amber-500 text-amber-500" />
                        <h4 className="text-xs font-bold text-amber-900">
                          {detailData.residentRating
                            ? 'Your Service Rating & Feedback'
                            : 'Rate This Resolution'}
                        </h4>
                      </div>

                      {detailData.residentRating ? (
                        <div className="space-y-1 text-xs">
                          <div className="flex items-center gap-1">
                            {[1, 2, 3, 4, 5].map((s) => (
                              <Star
                                key={s}
                                className={`w-4 h-4 ${
                                  s <= detailData.residentRating
                                    ? 'fill-amber-400 text-amber-400'
                                    : 'text-slate-300'
                                }`}
                              />
                            ))}
                            <span className="font-bold text-slate-700 ml-1.5">
                              {detailData.residentRating} out of 5 stars
                            </span>
                          </div>
                          {detailData.residentFeedback && (
                            <p className="text-slate-600 italic mt-1">
                              &ldquo;{detailData.residentFeedback}&rdquo;
                            </p>
                          )}
                        </div>
                      ) : (
                        <form onSubmit={handleRateSubmit} className="space-y-3">
                          <div className="flex items-center gap-1">
                            {[1, 2, 3, 4, 5].map((star) => (
                              <button
                                key={star}
                                type="button"
                                onClick={() => setRatingStars(star)}
                                className="p-1 hover:scale-110 transition"
                              >
                                <Star
                                  className={`w-6 h-6 ${
                                    star <= ratingStars
                                      ? 'fill-amber-400 text-amber-400'
                                      : 'text-slate-300'
                                  }`}
                                />
                              </button>
                            ))}
                            <span className="text-xs font-bold text-amber-800 ml-2">
                              {ratingStars} / 5 Stars
                            </span>
                          </div>

                          <input
                            type="text"
                            value={ratingFeedback}
                            onChange={(e) => setRatingFeedback(e.target.value)}
                            placeholder="Optional feedback comment on response speed or technician quality..."
                            className="w-full px-3 py-1.5 bg-white border border-amber-200 text-xs rounded-lg focus:outline-none"
                          />

                          <button
                            type="submit"
                            disabled={ratingLoading}
                            className="px-4 py-1.5 bg-amber-600 hover:bg-amber-700 text-white text-xs font-semibold rounded-lg shadow-sm transition disabled:opacity-50"
                          >
                            {ratingLoading ? 'Submitting...' : 'Submit Rating'}
                          </button>
                        </form>
                      )}
                    </div>
                  )}

                {/* =================================================================== */}
                {/* HELP DESK ACTIONS SECTION                                           */}
                {/* =================================================================== */}
                {isHelpDesk && (
                  <div className="p-4 bg-slate-50 border border-slate-200 rounded-xl space-y-4">
                    <div className="flex items-center gap-2 border-b border-slate-200 pb-2">
                      <Wrench className="w-4 h-4 text-slate-700" />
                      <h4 className="text-xs font-bold text-slate-900 uppercase tracking-wider">
                        Help Desk Management & Dispatch Controls
                      </h4>
                    </div>

                    {/* Team Assignment Form */}
                    <form
                      onSubmit={handleAssignSubmit}
                      className="grid grid-cols-1 sm:grid-cols-3 gap-2 items-end"
                    >
                      <div className="sm:col-span-2">
                        <label className="block text-[11px] font-bold text-slate-700 mb-1">
                          Assign Maintenance Team
                        </label>
                        <select
                          value={assignTeam}
                          onChange={(e) => setAssignTeam(e.target.value)}
                          className="w-full px-2.5 py-1.5 bg-white border border-slate-200 text-xs rounded-lg focus:outline-none font-medium"
                        >
                          {MAINTENANCE_TEAMS.map((t) => (
                            <option key={t} value={t}>
                              {t}
                            </option>
                          ))}
                        </select>
                      </div>
                      <button
                        type="submit"
                        disabled={assignLoading}
                        className="px-3 py-1.5 bg-slate-800 hover:bg-slate-900 text-white text-xs font-semibold rounded-lg transition disabled:opacity-50"
                      >
                        {assignLoading ? 'Assigning...' : 'Dispatch / Assign'}
                      </button>
                    </form>

                    {/* Status Update Form */}
                    <form onSubmit={handleStatusSubmit} className="space-y-2 pt-2 border-t border-slate-200">
                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                        <div>
                          <label className="block text-[11px] font-bold text-slate-700 mb-1">
                            Update Ticket Status
                          </label>
                          <select
                            value={updateStatusVal}
                            onChange={(e) => setUpdateStatusVal(e.target.value)}
                            className="w-full px-2.5 py-1.5 bg-white border border-slate-200 text-xs rounded-lg focus:outline-none font-medium"
                          >
                            <option value="SUBMITTED">SUBMITTED</option>
                            <option value="ASSIGNED">ASSIGNED</option>
                            <option value="IN_PROGRESS">IN_PROGRESS</option>
                            <option value="ON_HOLD">ON_HOLD</option>
                            <option value="RESOLVED">RESOLVED</option>
                            <option value="CLOSED">CLOSED</option>
                          </select>
                        </div>
                        <div>
                          <label className="block text-[11px] font-bold text-slate-700 mb-1">
                            Public Resolution Note (Visible to Resident)
                          </label>
                          <input
                            type="text"
                            value={resolutionNotes}
                            onChange={(e) => setResolutionNotes(e.target.value)}
                            placeholder="e.g. Electrician replaced fuse on Floor 4 DB panel."
                            className="w-full px-2.5 py-1.5 bg-white border border-slate-200 text-xs rounded-lg focus:outline-none"
                          />
                        </div>
                      </div>
                      <button
                        type="submit"
                        disabled={statusLoading}
                        className="px-4 py-1.5 bg-teal-600 hover:bg-teal-700 text-white text-xs font-semibold rounded-lg transition disabled:opacity-50"
                      >
                        {statusLoading ? 'Updating...' : 'Save Status Update'}
                      </button>
                    </form>

                    {/* Internal Staff Note Form */}
                    <form
                      onSubmit={handleInternalNoteSubmit}
                      className="pt-2 border-t border-slate-200 space-y-2"
                    >
                      <label className="block text-[11px] font-bold text-slate-700 flex items-center gap-1">
                        <Lock className="w-3 h-3 text-amber-600" />
                        Internal Staff Note (Strictly Hidden from Residents)
                      </label>
                      <div className="flex gap-2">
                        <input
                          type="text"
                          value={internalNoteText}
                          onChange={(e) => setInternalNoteText(e.target.value)}
                          placeholder="e.g. Schindler vendor ref ticket #SCH-8812. Spares arriving at 4 PM."
                          className="flex-1 px-2.5 py-1.5 bg-white border border-slate-200 text-xs rounded-lg focus:outline-none"
                        />
                        <button
                          type="submit"
                          disabled={internalNoteLoading || !internalNoteText.trim()}
                          className="px-3 py-1.5 bg-amber-600 hover:bg-amber-700 text-white text-xs font-semibold rounded-lg transition disabled:opacity-50"
                        >
                          {internalNoteLoading ? 'Saving...' : 'Add Staff Note'}
                        </button>
                      </div>
                    </form>

                    {/* Duplicate Detection & Linking */}
                    {detailData.potentialDuplicates && detailData.potentialDuplicates.length > 0 && (
                      <div className="pt-2 border-t border-slate-200 space-y-2">
                        <div className="flex items-center gap-1 text-[11px] font-bold text-slate-800">
                          <Link2 className="w-3.5 h-3.5 text-indigo-600" />
                          <span>Potential Duplicate Complaints in Same Tower ({detailData.towerName})</span>
                        </div>
                        <p className="text-[10px] text-slate-500">
                          Linking preserves both records for audit trails. Neither ticket will be deleted or closed.
                        </p>
                        <div className="divide-y divide-slate-100 bg-white rounded-lg border border-slate-200">
                          {detailData.potentialDuplicates.map((dup) => (
                            <div
                              key={dup.id}
                              className="p-2 flex items-center justify-between text-xs"
                            >
                              <div>
                                <span className="font-mono font-bold text-slate-700">
                                  {dup.complaintNumber}
                                </span>
                                <span className="text-slate-500 ml-2">Flat {dup.flatNumber}:</span>{' '}
                                <span className="font-medium text-slate-800">{dup.title}</span>
                              </div>
                              <button
                                type="button"
                                onClick={() => handleLinkDuplicate(dup.id)}
                                className="px-2.5 py-1 bg-indigo-50 hover:bg-indigo-100 text-indigo-700 font-semibold text-[10px] rounded transition"
                              >
                                Link as Duplicate
                              </button>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                )}

                {/* Audit Trail & Activity Timeline */}
                <div>
                  <h4 className="text-xs font-bold text-slate-700 uppercase tracking-wider mb-2 flex items-center gap-1.5">
                    <Clock className="w-3.5 h-3.5" />
                    Activity History & Audit Trail
                  </h4>
                  <div className="divide-y divide-slate-100 border border-slate-200 rounded-xl bg-white overflow-hidden text-xs">
                    {detailData.activities && detailData.activities.length > 0 ? (
                      detailData.activities.map((act) => (
                        <div key={act.id} className="p-3 space-y-1">
                          <div className="flex items-center justify-between">
                            <div className="flex items-center gap-2">
                              <span className="font-bold text-slate-800 text-[11px]">
                                {act.performedByName || 'System'}
                              </span>
                              <span className="text-[10px] font-mono bg-slate-100 text-slate-600 px-1.5 py-0.5 rounded">
                                {act.actionType}
                              </span>
                              {act.isInternalOnly && (
                                <span className="text-[10px] font-bold bg-amber-100 text-amber-800 px-1.5 py-0.5 rounded flex items-center gap-1">
                                  <Lock className="w-2.5 h-2.5" />
                                  Staff Only
                                </span>
                              )}
                            </div>
                            <span className="text-[10px] text-slate-400">
                              {new Date(act.createdAt).toLocaleDateString('en-IN', {
                                day: '2-digit',
                                month: 'short',
                                hour: '2-digit',
                                minute: '2-digit',
                              })}
                            </span>
                          </div>
                          {act.notes && (
                            <p className="text-slate-600 text-xs pl-2 border-l-2 border-slate-200">
                              {act.notes}
                            </p>
                          )}
                        </div>
                      ))
                    ) : (
                      <div className="p-3 text-slate-400 text-center">No activity logged yet.</div>
                    )}
                  </div>
                </div>
              </div>
            ) : (
              <div className="p-6 text-center text-slate-500 text-xs">No complaint selected.</div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
