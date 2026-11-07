import { useState, useEffect } from 'react';
import {
  Bell,
  AlertTriangle,
  Pin,
  PinOff,
  Calendar,
  Clock,
  Building2,
  Check,
  CheckCheck,
  Eye,
  Plus,
  Edit2,
  Trash2,
  Archive,
  RefreshCw,
  Search,
  Filter,
  X,
  ShieldAlert,
  Info,
  ChevronRight,
  FileText,
  AlertCircle,
  Users,
  CheckCircle2,
  Sparkles,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { noticeApi, townshipApi } from '../services/api';

const CATEGORIES = [
  { id: 'ALL', label: 'All Notices' },
  { id: 'EMERGENCY', label: 'Emergency Alerts', icon: AlertTriangle, color: 'text-red-600 bg-red-50 border-red-200' },
  { id: 'MAINTENANCE', label: 'Maintenance', icon: RefreshCw, color: 'text-amber-600 bg-amber-50 border-amber-200' },
  { id: 'BILLING', label: 'Billing & Dues', icon: FileText, color: 'text-blue-600 bg-blue-50 border-blue-200' },
  { id: 'EVENT', label: 'Events & Culture', icon: Sparkles, color: 'text-purple-600 bg-purple-50 border-purple-200' },
  { id: 'GENERAL', label: 'General Society', icon: Info, color: 'text-slate-600 bg-slate-50 border-slate-200' },
];

export default function NoticesPage() {
  const { user, isHelpDesk } = useAuth();

  // Primary tab: 'feed' (Resident & Staff) or 'admin' (Help Desk only)
  const [activeTab, setActiveTab] = useState('feed');
  const [selectedCategory, setSelectedCategory] = useState('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [includeExpired, setIncludeExpired] = useState(false);

  // Notice feed state
  const [notices, setNotices] = useState([]);
  const [kpis, setKpis] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Selected notice for reading in detail modal
  const [viewingNotice, setViewingNotice] = useState(null);

  // Help Desk Admin states
  const [adminNotices, setAdminNotices] = useState([]);
  const [loadingAdmin, setLoadingAdmin] = useState(false);
  const [towers, setTowers] = useState([]);

  // Create / Edit modal state
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [editingNotice, setEditingNotice] = useState(null);
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    category: 'GENERAL',
    priority: 'NORMAL',
    status: 'PUBLISHED',
    targetTowerId: '', // '' = Township-Wide
    isPinned: false,
    publishDate: '',
    expiryDate: '',
  });
  const [submitting, setSubmitting] = useState(false);

  // Fetch notices on mount or filter change
  useEffect(() => {
    fetchResidentFeed();
    fetchKpis();
    if (activeTab === 'admin' && isHelpDesk) {
      fetchAdminNotices();
      fetchTowers();
    }
  }, [activeTab, selectedCategory, includeExpired]);

  const fetchResidentFeed = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await noticeApi.getResidentNotices({
        includeExpired,
        category: selectedCategory !== 'ALL' ? selectedCategory : undefined,
      });
      setNotices(data);
    } catch (err) {
      setError(err.message || 'Failed to load notices');
    } finally {
      setLoading(false);
    }
  };

  const fetchKpis = async () => {
    try {
      const data = await noticeApi.getKpis();
      setKpis(data);
    } catch (err) {
      console.error('Failed to load notice KPIs:', err);
    }
  };

  const fetchAdminNotices = async () => {
    try {
      setLoadingAdmin(true);
      const data = await noticeApi.getAllNoticesAdmin();
      setAdminNotices(data);
    } catch (err) {
      console.error('Failed to load admin notices:', err);
    } finally {
      setLoadingAdmin(false);
    }
  };

  const fetchTowers = async () => {
    try {
      const data = await townshipApi.getTowers();
      setTowers(data);
    } catch (err) {
      console.error('Failed to load towers list:', err);
    }
  };

  const handleOpenNotice = async (notice) => {
    try {
      // Fetch full details which triggers auto-read on backend
      const full = await noticeApi.getNoticeById(notice.id);
      setViewingNotice(full);

      // Optimistically update read state in feeds
      setNotices((prev) =>
        prev.map((n) => (n.id === notice.id ? { ...n, isRead: true } : n))
      );
      fetchKpis();
    } catch (err) {
      alert('Failed to load notice: ' + err.message);
    }
  };

  const handleMarkAllRead = async () => {
    try {
      await noticeApi.markAllAsRead();
      setNotices((prev) => prev.map((n) => ({ ...n, isRead: true })));
      fetchKpis();
    } catch (err) {
      alert('Failed to mark all as read: ' + err.message);
    }
  };

  const handleTogglePin = async (notice) => {
    try {
      const updated = await noticeApi.togglePin(notice.id);
      setAdminNotices((prev) =>
        prev.map((n) => (n.id === notice.id ? updated : n))
      );
      fetchResidentFeed();
      fetchKpis();
    } catch (err) {
      alert('Failed to toggle pin: ' + err.message);
    }
  };

  const handleArchiveNotice = async (notice) => {
    if (!window.confirm(`Are you sure you want to archive notice "${notice.title}"?`)) return;
    try {
      await noticeApi.archiveNotice(notice.id);
      fetchAdminNotices();
      fetchResidentFeed();
      fetchKpis();
    } catch (err) {
      alert('Failed to archive notice: ' + err.message);
    }
  };

  const handleDeleteNotice = async (notice) => {
    if (!window.confirm(`Permanently delete notice "${notice.title}"? This cannot be undone.`)) return;
    try {
      await noticeApi.deleteNotice(notice.id);
      fetchAdminNotices();
      fetchResidentFeed();
      fetchKpis();
    } catch (err) {
      alert('Failed to delete notice: ' + err.message);
    }
  };

  const handleOpenCreateModal = (noticeToEdit = null) => {
    if (noticeToEdit) {
      setEditingNotice(noticeToEdit);
      setFormData({
        title: noticeToEdit.title,
        description: noticeToEdit.description,
        category: noticeToEdit.category,
        priority: noticeToEdit.priority,
        status: noticeToEdit.status,
        targetTowerId: noticeToEdit.targetTowerId || '',
        isPinned: noticeToEdit.isPinned,
        publishDate: noticeToEdit.publishDate ? noticeToEdit.publishDate.slice(0, 16) : '',
        expiryDate: noticeToEdit.expiryDate ? noticeToEdit.expiryDate.slice(0, 16) : '',
      });
    } else {
      setEditingNotice(null);
      setFormData({
        title: '',
        description: '',
        category: 'GENERAL',
        priority: 'NORMAL',
        status: 'PUBLISHED',
        targetTowerId: '',
        isPinned: false,
        publishDate: '',
        expiryDate: '',
      });
    }
    setShowCreateModal(true);
  };

  const handleSaveNotice = async (e) => {
    e.preventDefault();
    try {
      setSubmitting(true);
      const payload = {
        title: formData.title,
        description: formData.description,
        category: formData.category,
        priority: formData.priority,
        status: formData.status,
        targetTowerId: formData.targetTowerId || null,
        isPinned: formData.isPinned,
        publishDate: formData.publishDate ? formData.publishDate + ':00' : undefined,
        expiryDate: formData.expiryDate ? formData.expiryDate + ':00' : undefined,
      };

      if (editingNotice) {
        await noticeApi.updateNotice(editingNotice.id, payload);
      } else {
        await noticeApi.createNotice(payload);
      }

      setShowCreateModal(false);
      setEditingNotice(null);
      fetchAdminNotices();
      fetchResidentFeed();
      fetchKpis();
    } catch (err) {
      alert('Failed to save notice: ' + err.message);
    } finally {
      setSubmitting(false);
    }
  };

  // Find active emergency notice to display top alert banner
  const emergencyNotice = notices.find(
    (n) => n.category === 'EMERGENCY' && n.priority === 'URGENT' && !n.isExpired
  );

  // Search filter
  const filteredNotices = notices.filter((n) => {
    if (!searchQuery.trim()) return true;
    const q = searchQuery.toLowerCase();
    return (
      n.title.toLowerCase().includes(q) ||
      n.description.toLowerCase().includes(q) ||
      (n.targetTowerName && n.targetTowerName.toLowerCase().includes(q))
    );
  });

  const getPriorityBadge = (priority) => {
    switch (priority) {
      case 'URGENT':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-red-100 text-red-700 border border-red-300">
            <span className="w-1.5 h-1.5 rounded-full bg-red-600 animate-ping"></span>
            URGENT
          </span>
        );
      case 'HIGH':
        return (
          <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-amber-100 text-amber-700 border border-amber-300">
            HIGH
          </span>
        );
      case 'NORMAL':
        return (
          <span className="px-2.5 py-0.5 rounded-full text-[10px] font-semibold bg-slate-100 text-slate-700 border border-slate-200">
            NORMAL
          </span>
        );
      case 'LOW':
        return (
          <span className="px-2.5 py-0.5 rounded-full text-[10px] font-medium bg-slate-50 text-slate-500 border border-slate-200">
            LOW
          </span>
        );
      default:
        return null;
    }
  };

  const getCategoryBadge = (category) => {
    const cat = CATEGORIES.find((c) => c.id === category);
    return (
      <span
        className={`px-2 py-0.5 rounded-md text-[11px] font-semibold uppercase tracking-wider border ${
          cat ? cat.color : 'text-slate-600 bg-slate-100 border-slate-200'
        }`}
      >
        {category}
      </span>
    );
  };

  return (
    <div className="space-y-6">
      {/* Top Header / Role Switcher */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white p-6 rounded-2xl border border-slate-200/80 shadow-xs">
        <div>
          <div className="flex items-center gap-2.5">
            <span className="p-2 rounded-xl bg-teal-50 text-teal-600 border border-teal-200/60">
              <Bell className="w-5 h-5" />
            </span>
            <h1 className="text-2xl font-bold text-slate-900 tracking-tight">
              Notices & Announcements
            </h1>
            {kpis && kpis.unreadCount > 0 && (
              <span className="px-2.5 py-0.5 rounded-full text-xs font-bold bg-teal-600 text-white shadow-xs">
                {kpis.unreadCount} new
              </span>
            )}
          </div>
          <p className="text-sm text-slate-500 mt-1">
            Township circulars, emergency updates, scheduled maintenance alerts, and society cultural broadcasts.
          </p>
        </div>

        {/* Global tab buttons */}
        <div className="flex items-center gap-1.5 p-1 bg-slate-100 rounded-xl border border-slate-200 self-start md:self-auto">
          <button
            onClick={() => setActiveTab('feed')}
            className={`px-4 py-2 rounded-lg text-xs font-semibold transition cursor-pointer flex items-center gap-2 ${
              activeTab === 'feed'
                ? 'bg-white text-slate-900 shadow-xs border border-slate-200/60'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            <Bell className="w-4 h-4 text-teal-600" />
            Notice Board
          </button>

          {isHelpDesk && (
            <button
              onClick={() => setActiveTab('admin')}
              className={`px-4 py-2 rounded-lg text-xs font-semibold transition cursor-pointer flex items-center gap-2 ${
                activeTab === 'admin'
                  ? 'bg-white text-slate-900 shadow-xs border border-slate-200/60'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <ShieldAlert className="w-4 h-4 text-indigo-600" />
              Help Desk Admin
            </button>
          )}
        </div>
      </div>

      {/* ==================================================================== */}
      {/* EMERGENCY HIGHLIGHT BANNER                                           */}
      {/* ==================================================================== */}
      {emergencyNotice && activeTab === 'feed' && (
        <div className="bg-gradient-to-r from-red-600 via-rose-600 to-red-700 text-white rounded-2xl p-5 shadow-lg shadow-red-500/10 border border-red-500 relative overflow-hidden flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="flex items-start gap-3.5 z-10">
            <div className="p-2.5 rounded-xl bg-white/20 backdrop-blur-xs text-white shrink-0 mt-0.5">
              <AlertTriangle className="w-6 h-6 animate-pulse" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="px-2 py-0.5 rounded-md bg-white/25 text-[10px] font-bold uppercase tracking-wider">
                  EMERGENCY BROADCAST
                </span>
                <span className="text-xs text-red-100">
                  {emergencyNotice.isTownshipWide ? 'All 28 Towers' : emergencyNotice.targetTowerId}
                </span>
              </div>
              <h3 className="text-base font-bold mt-1 text-white">{emergencyNotice.title}</h3>
              <p className="text-xs text-red-100 mt-1 line-clamp-2 max-w-3xl leading-relaxed">
                {emergencyNotice.description}
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={() => handleOpenNotice(emergencyNotice)}
            className="self-start md:self-auto px-4 py-2 rounded-xl bg-white text-red-700 hover:bg-red-50 text-xs font-bold transition shadow-xs shrink-0 cursor-pointer flex items-center gap-1.5"
          >
            Read Emergency Alert
            <ChevronRight className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* ==================================================================== */}
      {/* TAB 1: RESIDENT NOTICE BOARD                                         */}
      {/* ==================================================================== */}
      {activeTab === 'feed' && (
        <div className="space-y-6">
          {/* Controls Bar: Category Pills + Search + Mark All Read */}
          <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
            {/* Category Pills */}
            <div className="flex items-center gap-1.5 overflow-x-auto pb-1">
              {CATEGORIES.map((cat) => (
                <button
                  key={cat.id}
                  onClick={() => setSelectedCategory(cat.id)}
                  className={`px-3 py-1.5 rounded-full text-xs font-medium whitespace-nowrap transition cursor-pointer ${
                    selectedCategory === cat.id
                      ? 'bg-slate-900 text-white shadow-xs'
                      : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-50'
                  }`}
                >
                  {cat.label}
                </button>
              ))}
            </div>

            {/* Right Tools */}
            <div className="flex items-center gap-2 self-start lg:self-auto">
              <div className="relative">
                <Search className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  placeholder="Search notices..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="pl-8 pr-3 py-1.5 text-xs rounded-xl border border-slate-200 bg-white text-slate-800 placeholder-slate-400 focus:ring-2 focus:ring-teal-500 w-48 sm:w-60"
                />
              </div>

              {/* Show expired checkbox */}
              <label className="flex items-center gap-1.5 text-xs text-slate-600 cursor-pointer select-none bg-white px-2.5 py-1.5 rounded-xl border border-slate-200">
                <input
                  type="checkbox"
                  checked={includeExpired}
                  onChange={(e) => setIncludeExpired(e.target.checked)}
                  className="rounded text-teal-600 focus:ring-teal-500"
                />
                <span>Include Expired</span>
              </label>

              {/* Mark All Read Button */}
              {kpis && kpis.unreadCount > 0 && (
                <button
                  type="button"
                  onClick={handleMarkAllRead}
                  className="px-3 py-1.5 rounded-xl border border-teal-200 bg-teal-50 hover:bg-teal-100 text-teal-700 text-xs font-semibold flex items-center gap-1 cursor-pointer transition"
                  title="Mark all notices as read"
                >
                  <CheckCheck className="w-3.5 h-3.5" />
                  Mark All Read
                </button>
              )}
            </div>
          </div>

          {/* Loading / Error States */}
          {loading && (
            <div className="bg-white p-12 rounded-2xl border border-slate-200 flex flex-col items-center justify-center text-slate-500">
              <RefreshCw className="w-8 h-8 animate-spin text-teal-600 mb-3" />
              <p className="text-sm font-medium">Fetching notices for your tower & township...</p>
            </div>
          )}

          {error && (
            <div className="p-4 rounded-xl bg-red-50 border border-red-200 text-red-700 text-sm flex items-center gap-3">
              <AlertCircle className="w-5 h-5 shrink-0" />
              <div>{error}</div>
            </div>
          )}

          {/* Notice Cards List */}
          {!loading && !error && filteredNotices.length === 0 ? (
            <div className="bg-white p-12 rounded-2xl border border-slate-200 text-center">
              <Bell className="w-10 h-10 text-slate-300 mx-auto mb-2" />
              <h4 className="text-sm font-bold text-slate-800">No Notices Found</h4>
              <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
                No active announcements matching your selected category and tower filters. Check back later for official broadcasts.
              </p>
            </div>
          ) : (
            <div className="space-y-4">
              {filteredNotices.map((notice) => {
                const isEmergency = notice.category === 'EMERGENCY';
                return (
                  <div
                    key={notice.id}
                    onClick={() => handleOpenNotice(notice)}
                    className={`bg-white rounded-2xl border p-5 transition cursor-pointer hover:shadow-md relative flex flex-col justify-between ${
                      notice.isPinned
                        ? 'border-amber-300 bg-amber-50/20 shadow-xs'
                        : isEmergency
                        ? 'border-red-300 bg-red-50/20'
                        : notice.isRead
                        ? 'border-slate-200/80 hover:border-slate-300'
                        : 'border-teal-300 ring-1 ring-teal-400/20 bg-teal-50/10'
                    }`}
                  >
                    <div>
                      {/* Top Badges */}
                      <div className="flex flex-wrap items-center justify-between gap-2 mb-2.5">
                        <div className="flex flex-wrap items-center gap-2">
                          {notice.isPinned && (
                            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[10px] font-bold bg-amber-100 text-amber-800 border border-amber-300">
                              <Pin className="w-3 h-3 fill-amber-700" />
                              PINNED
                            </span>
                          )}

                          {getCategoryBadge(notice.category)}
                          {getPriorityBadge(notice.priority)}

                          {/* Target Scope */}
                          <span className="inline-flex items-center gap-1 text-[11px] font-medium text-slate-600 bg-slate-100 px-2 py-0.5 rounded-md">
                            <Building2 className="w-3 h-3 text-slate-400" />
                            {notice.isTownshipWide ? 'Township-Wide (28 Towers)' : `Tower ${notice.targetTowerId}`}
                          </span>
                        </div>

                        {/* Read Status Pill */}
                        <div className="flex items-center gap-2">
                          {notice.isExpired && (
                            <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-slate-100 text-slate-500">
                              Expired
                            </span>
                          )}

                          {!notice.isRead ? (
                            <span className="inline-flex items-center gap-1 text-[11px] font-bold text-teal-700 bg-teal-50 px-2.5 py-0.5 rounded-full border border-teal-200">
                              <span className="w-2 h-2 rounded-full bg-teal-500"></span>
                              Unread
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1 text-[11px] font-medium text-slate-400">
                              <Check className="w-3.5 h-3.5 text-slate-400" />
                              Read
                            </span>
                          )}
                        </div>
                      </div>

                      {/* Title & Preview */}
                      <h3 className="text-base font-bold text-slate-900 hover:text-teal-600 transition">
                        {notice.title}
                      </h3>
                      <p className="text-xs text-slate-600 mt-1 line-clamp-2 leading-relaxed">
                        {notice.description}
                      </p>
                    </div>

                    {/* Footer metadata */}
                    <div className="mt-4 pt-3 border-t border-slate-100 flex flex-wrap items-center justify-between gap-2 text-xs text-slate-400">
                      <div className="flex items-center gap-3">
                        <span>Issued by: <strong className="text-slate-700">{notice.authorName}</strong></span>
                        <span>•</span>
                        <span>Published: {notice.publishDate?.slice(0, 10)}</span>
                        {notice.expiryDate && (
                          <>
                            <span>•</span>
                            <span>Expires: {notice.expiryDate.slice(0, 10)}</span>
                          </>
                        )}
                      </div>

                      <span className="text-teal-600 font-semibold hover:underline flex items-center gap-1">
                        Read Notice
                        <ChevronRight className="w-3.5 h-3.5" />
                      </span>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}

      {/* ==================================================================== */}
      {/* TAB 2: HELP DESK ADMINISTRATION                                      */}
      {/* ==================================================================== */}
      {activeTab === 'admin' && isHelpDesk && (
        <div className="space-y-6">
          {/* Admin KPI Summary */}
          {kpis && (
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-xs">
                <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Total Notices</span>
                <div className="text-2xl font-bold text-slate-900 mt-1">{kpis.totalNotices}</div>
                <span className="text-[11px] text-teal-600 mt-1 block">Master Announcement Log</span>
              </div>

              <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-xs">
                <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Active Published</span>
                <div className="text-2xl font-bold text-emerald-600 mt-1">{kpis.activePublished}</div>
                <span className="text-[11px] text-slate-500 mt-1 block">Live in resident feed</span>
              </div>

              <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-xs">
                <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Emergency Alerts</span>
                <div className="text-2xl font-bold text-red-600 mt-1">{kpis.emergencyNotices}</div>
                <span className="text-[11px] text-red-500 mt-1 block">High priority broadcasts</span>
              </div>

              <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-xs">
                <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Pinned Notices</span>
                <div className="text-2xl font-bold text-amber-600 mt-1">{kpis.pinnedNotices}</div>
                <span className="text-[11px] text-slate-500 mt-1 block">Pinned at top of feed</span>
              </div>
            </div>
          )}

          {/* Action Header */}
          <div className="bg-white rounded-2xl border border-slate-200 shadow-xs overflow-hidden">
            <div className="p-5 border-b border-slate-100 flex items-center justify-between">
              <div>
                <h3 className="text-base font-bold text-slate-900">Manage Notices & Broadcasts</h3>
                <p className="text-xs text-slate-500 mt-0.5">Author, publish, edit, pin, and archive township communications.</p>
              </div>

              <button
                type="button"
                onClick={() => handleOpenCreateModal()}
                className="px-4 py-2 rounded-xl bg-slate-900 hover:bg-slate-800 text-white text-xs font-semibold flex items-center gap-1.5 cursor-pointer shadow-xs transition"
              >
                <Plus className="w-4 h-4" />
                Publish Notice
              </button>
            </div>

            {/* Admin Notices Table */}
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs text-slate-600">
                <thead className="bg-slate-50 text-slate-400 font-semibold uppercase tracking-wider border-b border-slate-100">
                  <tr>
                    <th className="px-5 py-3.5">Title</th>
                    <th className="px-4 py-3.5">Category & Priority</th>
                    <th className="px-4 py-3.5">Target Scope</th>
                    <th className="px-4 py-3.5">Publish & Expiry</th>
                    <th className="px-4 py-3.5">Reads</th>
                    <th className="px-4 py-3.5">Status</th>
                    <th className="px-5 py-3.5 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {adminNotices.map((n) => (
                    <tr key={n.id} className="hover:bg-slate-50/60 transition">
                      <td className="px-5 py-3.5">
                        <div className="font-bold text-slate-900 flex items-center gap-1.5">
                          {n.isPinned && <Pin className="w-3 h-3 text-amber-600 fill-amber-600 shrink-0" />}
                          <span className="truncate max-w-xs">{n.title}</span>
                        </div>
                        <div className="text-[11px] text-slate-400 truncate max-w-xs">{n.description}</div>
                      </td>
                      <td className="px-4 py-3.5">
                        <div className="flex items-center gap-1.5">
                          {getCategoryBadge(n.category)}
                          {getPriorityBadge(n.priority)}
                        </div>
                      </td>
                      <td className="px-4 py-3.5">
                        <span className="font-medium text-slate-700">
                          {n.isTownshipWide ? 'Township-Wide' : `Tower ${n.targetTowerId}`}
                        </span>
                      </td>
                      <td className="px-4 py-3.5">
                        <div>Pub: {n.publishDate?.slice(0, 10)}</div>
                        <div className="text-[11px] text-slate-400">
                          {n.expiryDate ? `Exp: ${n.expiryDate.slice(0, 10)}` : 'No Expiry'}
                        </div>
                      </td>
                      <td className="px-4 py-3.5 font-mono font-semibold text-slate-800">
                        {n.readCount} reads
                      </td>
                      <td className="px-4 py-3.5">
                        <span
                          className={`px-2 py-0.5 rounded-full text-[10px] font-bold border ${
                            n.status === 'PUBLISHED'
                              ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                              : n.status === 'ARCHIVED'
                              ? 'bg-slate-100 text-slate-600 border-slate-200'
                              : 'bg-amber-50 text-amber-700 border-amber-200'
                          }`}
                        >
                          {n.status}
                        </span>
                      </td>
                      <td className="px-5 py-3.5 text-right space-x-1.5">
                        <button
                          type="button"
                          onClick={() => handleTogglePin(n)}
                          className={`p-1.5 rounded-lg border cursor-pointer ${
                            n.isPinned
                              ? 'bg-amber-50 border-amber-300 text-amber-700'
                              : 'border-slate-200 text-slate-500 hover:bg-slate-100'
                          }`}
                          title={n.isPinned ? 'Unpin notice' : 'Pin notice to top'}
                        >
                          <Pin className="w-3.5 h-3.5" />
                        </button>

                        <button
                          type="button"
                          onClick={() => handleOpenCreateModal(n)}
                          className="p-1.5 rounded-lg border border-slate-200 text-slate-600 hover:bg-slate-100 cursor-pointer"
                          title="Edit notice"
                        >
                          <Edit2 className="w-3.5 h-3.5" />
                        </button>

                        {n.status === 'PUBLISHED' && (
                          <button
                            type="button"
                            onClick={() => handleArchiveNotice(n)}
                            className="p-1.5 rounded-lg border border-slate-200 text-slate-500 hover:bg-slate-100 cursor-pointer"
                            title="Archive notice"
                          >
                            <Archive className="w-3.5 h-3.5" />
                          </button>
                        )}

                        <button
                          type="button"
                          onClick={() => handleDeleteNotice(n)}
                          className="p-1.5 rounded-lg border border-red-200 text-red-600 hover:bg-red-50 cursor-pointer"
                          title="Delete notice"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* ==================================================================== */}
      {/* MODAL 1: VIEW NOTICE DETAILS                                         */}
      {/* ==================================================================== */}
      {viewingNotice && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-2xl w-full p-6 shadow-2xl border border-slate-200 relative animate-in fade-in zoom-in-95 duration-200 max-h-[90vh] overflow-y-auto">
            <button
              onClick={() => setViewingNotice(null)}
              className="absolute top-5 right-5 text-slate-400 hover:text-slate-600 p-1.5 rounded-lg hover:bg-slate-100 transition cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>

            {/* Badges Bar */}
            <div className="flex flex-wrap items-center gap-2 mb-3">
              {viewingNotice.isPinned && (
                <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-md text-[10px] font-bold bg-amber-100 text-amber-800 border border-amber-300">
                  <Pin className="w-3 h-3 fill-amber-700" />
                  PINNED
                </span>
              )}
              {getCategoryBadge(viewingNotice.category)}
              {getPriorityBadge(viewingNotice.priority)}
              <span className="inline-flex items-center gap-1 text-[11px] font-medium text-slate-600 bg-slate-100 px-2 py-0.5 rounded-md">
                <Building2 className="w-3 h-3 text-slate-400" />
                {viewingNotice.isTownshipWide ? 'Township-Wide' : `Tower ${viewingNotice.targetTowerId}`}
              </span>
            </div>

            {/* Title */}
            <h2 className="text-xl font-bold text-slate-900 leading-snug">
              {viewingNotice.title}
            </h2>

            {/* Metadata Bar */}
            <div className="my-3 py-2.5 px-3.5 rounded-xl bg-slate-50 border border-slate-100 flex flex-wrap items-center justify-between gap-3 text-xs text-slate-500">
              <div>Author: <strong className="text-slate-800">{viewingNotice.authorName}</strong></div>
              <div>Published: <strong className="text-slate-800">{viewingNotice.publishDate?.slice(0, 10)}</strong></div>
              {viewingNotice.expiryDate && (
                <div>Expires: <strong className="text-slate-800">{viewingNotice.expiryDate.slice(0, 10)}</strong></div>
              )}
            </div>

            {/* Content Body */}
            <div className="text-xs text-slate-700 leading-relaxed whitespace-pre-line bg-white p-4 rounded-xl border border-slate-100 space-y-2 mt-4 text-justify">
              {viewingNotice.description}
            </div>

            <div className="mt-6 flex items-center justify-between border-t border-slate-100 pt-4">
              <span className="text-[11px] text-emerald-600 flex items-center gap-1 font-medium">
                <CheckCircle2 className="w-3.5 h-3.5" />
                Marked as read on your account
              </span>

              <button
                type="button"
                onClick={() => setViewingNotice(null)}
                className="px-5 py-2 rounded-xl bg-slate-900 hover:bg-slate-800 text-white text-xs font-semibold cursor-pointer"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ==================================================================== */}
      {/* MODAL 2: CREATE / EDIT NOTICE (HELP DESK ONLY)                       */}
      {/* ==================================================================== */}
      {showCreateModal && isHelpDesk && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-xl w-full p-6 shadow-2xl border border-slate-200 relative animate-in fade-in zoom-in-95 duration-200 max-h-[90vh] overflow-y-auto">
            <button
              onClick={() => setShowCreateModal(false)}
              className="absolute top-5 right-5 text-slate-400 hover:text-slate-600 p-1.5 rounded-lg hover:bg-slate-100 transition cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>

            <div className="flex items-center gap-3 mb-5">
              <div className="p-3 rounded-2xl bg-indigo-50 text-indigo-600 border border-indigo-200/50">
                <Bell className="w-6 h-6" />
              </div>
              <div>
                <h3 className="text-lg font-bold text-slate-900">
                  {editingNotice ? 'Edit Notice Announcement' : 'Publish Notice Announcement'}
                </h3>
                <p className="text-xs text-slate-500">Official communication for Pune Grand Vista residents</p>
              </div>
            </div>

            <form onSubmit={handleSaveNotice} className="space-y-4">
              {/* Notice Title */}
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">Notice Headline</label>
                <input
                  type="text"
                  value={formData.title}
                  onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                  placeholder="e.g. Scheduled Lift Maintenance in Tower G"
                  className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 bg-white"
                  required
                />
              </div>

              {/* Category, Priority, Target Tower */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Category</label>
                  <select
                    value={formData.category}
                    onChange={(e) => setFormData({ ...formData, category: e.target.value })}
                    className="w-full px-2.5 py-2 text-xs rounded-xl border border-slate-200 bg-white"
                  >
                    <option value="GENERAL">GENERAL</option>
                    <option value="MAINTENANCE">MAINTENANCE</option>
                    <option value="EMERGENCY">EMERGENCY</option>
                    <option value="EVENT">EVENT</option>
                    <option value="BILLING">BILLING</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Priority</label>
                  <select
                    value={formData.priority}
                    onChange={(e) => setFormData({ ...formData, priority: e.target.value })}
                    className="w-full px-2.5 py-2 text-xs rounded-xl border border-slate-200 bg-white"
                  >
                    <option value="LOW">LOW</option>
                    <option value="NORMAL">NORMAL</option>
                    <option value="HIGH">HIGH</option>
                    <option value="URGENT">URGENT</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Target Scope</label>
                  <select
                    value={formData.targetTowerId}
                    onChange={(e) => setFormData({ ...formData, targetTowerId: e.target.value })}
                    className="w-full px-2.5 py-2 text-xs rounded-xl border border-slate-200 bg-white"
                  >
                    <option value="">Township-Wide (All Towers)</option>
                    {towers.map((t) => (
                      <option key={t.id} value={t.id}>
                        {t.id} - {t.name}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              {/* Publish Date & Expiry Date */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Publish Date / Time</label>
                  <input
                    type="datetime-local"
                    value={formData.publishDate}
                    onChange={(e) => setFormData({ ...formData, publishDate: e.target.value })}
                    className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 bg-white"
                  />
                  <span className="text-[10px] text-slate-400 mt-0.5 block">Leave blank for immediate broadcast</span>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Expiry Date / Time</label>
                  <input
                    type="datetime-local"
                    value={formData.expiryDate}
                    onChange={(e) => setFormData({ ...formData, expiryDate: e.target.value })}
                    className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 bg-white"
                  />
                  <span className="text-[10px] text-slate-400 mt-0.5 block">Optional auto-archive deadline</span>
                </div>
              </div>

              {/* Description Body */}
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">Detailed Content</label>
                <textarea
                  rows={5}
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                  placeholder="Full text of notice, instructions, affected areas, and helplines..."
                  className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 bg-white leading-relaxed"
                  required
                />
              </div>

              {/* Pin Checkbox */}
              <div className="flex items-center gap-2 pt-1">
                <input
                  type="checkbox"
                  id="isPinned"
                  checked={formData.isPinned}
                  onChange={(e) => setFormData({ ...formData, isPinned: e.target.checked })}
                  className="rounded text-teal-600 focus:ring-teal-500"
                />
                <label htmlFor="isPinned" className="text-xs font-semibold text-slate-700 cursor-pointer">
                  Pin this notice to the top of the resident feed
                </label>
              </div>

              {/* Form Buttons */}
              <div className="pt-3 flex items-center justify-end gap-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-100 cursor-pointer"
                >
                  Cancel
                </button>

                <button
                  type="submit"
                  disabled={submitting}
                  className="px-5 py-2.5 rounded-xl bg-slate-900 hover:bg-slate-800 text-white text-xs font-semibold cursor-pointer shadow-xs disabled:opacity-50"
                >
                  {submitting ? 'Saving...' : editingNotice ? 'Update Notice' : 'Publish Announcement'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
