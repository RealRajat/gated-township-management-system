import { useState, useEffect } from 'react';
import {
  Sparkles,
  Calendar,
  Clock,
  MapPin,
  Users,
  CheckCircle2,
  XCircle,
  AlertCircle,
  Plus,
  Edit2,
  Power,
  Info,
  RefreshCw,
  Search,
  Filter,
  X,
  Copy,
  Check,
  Dumbbell,
  FileText,
  ShieldAlert,
  ChevronRight,
  ShieldCheck,
  Building2,
  Ban,
  Activity,
  Layers,
} from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { amenityApi } from '../services/api';

const CATEGORIES = [
  { id: 'ALL', label: 'All Amenities' },
  { id: 'FITNESS', label: 'Fitness & Health' },
  { id: 'SPORTS', label: 'Sports Complex' },
  { id: 'LEISURE', label: 'Clubhouse & Leisure' },
  { id: 'EVENTS', label: 'Banquet & Events' },
];

export default function AmenitiesPage() {
  const { user, isHelpDesk } = useAuth();

  // Primary active tab: 'explore', 'my-bookings', 'admin'
  const [activeTab, setActiveTab] = useState('explore');
  const [selectedCategory, setSelectedCategory] = useState('ALL');

  // Amenity catalog
  const [amenities, setAmenities] = useState([]);
  const [loadingAmenities, setLoadingAmenities] = useState(true);
  const [amenitiesError, setAmenitiesError] = useState(null);

  // Selected amenity for slot viewing & booking
  const [selectedAmenity, setSelectedAmenity] = useState(null);
  const [selectedDate, setSelectedDate] = useState(() => {
    const d = new Date();
    d.setDate(d.getDate() + 1); // default tomorrow
    return d.toISOString().split('T')[0];
  });
  const [slots, setSlots] = useState([]);
  const [loadingSlots, setLoadingSlots] = useState(false);

  // Booking modal
  const [bookingSlot, setBookingSlot] = useState(null);
  const [bookingAttendeeCount, setBookingAttendeeCount] = useState(1);
  const [bookingPurpose, setBookingPurpose] = useState('');
  const [bookingFlatId, setBookingFlatId] = useState(() => {
    return user?.flats && user.flats.length > 0 ? user.flats[0].id : 1;
  });
  const [submittingBooking, setSubmittingBooking] = useState(false);
  const [bookingSuccessData, setBookingSuccessData] = useState(null);
  const [bookingError, setBookingError] = useState(null);

  // Guidelines / Rules modal
  const [viewingRulesAmenity, setViewingRulesAmenity] = useState(null);

  // My bookings tab
  const [myBookings, setMyBookings] = useState([]);
  const [loadingMyBookings, setLoadingMyBookings] = useState(false);
  const [bookingFilterStatus, setBookingFilterStatus] = useState('ALL');
  const [cancellingBooking, setCancellingBooking] = useState(null);
  const [cancellationReason, setCancellationReason] = useState('');
  const [submittingCancel, setSubmittingCancel] = useState(false);

  // Admin tab states
  const [adminOverview, setAdminOverview] = useState(null);
  const [adminAmenities, setAdminAmenities] = useState([]);
  const [adminBookings, setAdminBookings] = useState([]);
  const [loadingAdmin, setLoadingAdmin] = useState(false);
  const [adminBookingDateFilter, setAdminBookingDateFilter] = useState('');
  const [adminAmenityFilter, setAdminAmenityFilter] = useState('ALL');
  const [adminStatusFilter, setAdminStatusFilter] = useState('ALL');

  // Admin Create/Edit Amenity modal
  const [showCreateAmenityModal, setShowCreateAmenityModal] = useState(false);
  const [editingAmenity, setEditingAmenity] = useState(null);
  const [amenityFormData, setAmenityFormData] = useState({
    name: '',
    code: '',
    category: 'FITNESS',
    description: '',
    location: '',
    openTime: '06:00',
    closeTime: '22:00',
    slotDurationMinutes: 60,
    slotCapacity: 10,
    maxActiveBookingsPerFlat: 2,
    cancelDeadlineHours: 2,
    rulesAndGuidelines: '',
  });

  // Copied reference toast
  const [copiedRef, setCopiedRef] = useState(null);

  // Load amenities on mount
  useEffect(() => {
    fetchActiveAmenities();
    if (activeTab === 'my-bookings') {
      fetchMyBookings();
    } else if (activeTab === 'admin' && isHelpDesk) {
      fetchAdminData();
    }
  }, [activeTab]);

  // Load slots when selected amenity or date changes
  useEffect(() => {
    if (selectedAmenity) {
      fetchSlots(selectedAmenity.id, selectedDate);
    }
  }, [selectedAmenity, selectedDate]);

  const fetchActiveAmenities = async () => {
    try {
      setLoadingAmenities(true);
      setAmenitiesError(null);
      const data = await amenityApi.getActiveAmenities();
      setAmenities(data);
      if (data.length > 0 && !selectedAmenity) {
        setSelectedAmenity(data[0]);
      }
    } catch (err) {
      setAmenitiesError(err.message || 'Failed to load amenities catalog');
    } finally {
      setLoadingAmenities(false);
    }
  };

  const fetchSlots = async (amenityId, date) => {
    try {
      setLoadingSlots(true);
      const data = await amenityApi.getAvailableSlots(amenityId, date);
      setSlots(data);
    } catch (err) {
      console.error('Failed to load slots:', err);
      setSlots([]);
    } finally {
      setLoadingSlots(false);
    }
  };

  const fetchMyBookings = async () => {
    try {
      setLoadingMyBookings(true);
      const data = await amenityApi.getMyBookings();
      setMyBookings(data);
    } catch (err) {
      console.error('Failed to load my bookings:', err);
    } finally {
      setLoadingMyBookings(false);
    }
  };

  const fetchAdminData = async () => {
    try {
      setLoadingAdmin(true);
      const [overviewData, amenitiesList, bookingsList] = await Promise.all([
        amenityApi.getAdminOverview(),
        amenityApi.getAllAmenitiesAdmin(),
        amenityApi.getAllBookingsAdmin({
          date: adminBookingDateFilter || undefined,
          amenityId: adminAmenityFilter !== 'ALL' ? adminAmenityFilter : undefined,
          status: adminStatusFilter !== 'ALL' ? adminStatusFilter : undefined,
        }),
      ]);
      setAdminOverview(overviewData);
      setAdminAmenities(amenitiesList);
      setAdminBookings(bookingsList);
    } catch (err) {
      console.error('Failed to load admin overview:', err);
    } finally {
      setLoadingAdmin(false);
    }
  };

  const handleOpenBookingModal = (slot) => {
    setBookingSlot(slot);
    setBookingAttendeeCount(1);
    setBookingPurpose('');
    setBookingError(null);
  };

  const handleConfirmBooking = async (e) => {
    e.preventDefault();
    if (!selectedAmenity || !bookingSlot) return;

    try {
      setSubmittingBooking(true);
      setBookingError(null);

      const payload = {
        amenityId: selectedAmenity.id,
        flatId: bookingFlatId,
        bookingDate: selectedDate,
        startTime: bookingSlot.startTime,
        attendeeCount: parseInt(bookingAttendeeCount, 10),
        purpose: bookingPurpose.trim() || undefined,
      };

      const res = await amenityApi.createBooking(payload);
      setBookingSuccessData(res);
      setBookingSlot(null);
      // Refresh slots immediately
      fetchSlots(selectedAmenity.id, selectedDate);
    } catch (err) {
      setBookingError(err.message || 'Failed to complete booking');
    } finally {
      setSubmittingBooking(false);
    }
  };

  const handleCancelBooking = async () => {
    if (!cancellingBooking) return;
    try {
      setSubmittingCancel(true);
      if (activeTab === 'admin') {
        await amenityApi.adminCancelBooking(cancellingBooking.id, { reason: cancellationReason });
        fetchAdminData();
      } else {
        await amenityApi.cancelBooking(cancellingBooking.id, { reason: cancellationReason });
        fetchMyBookings();
      }
      setCancellingBooking(null);
      setCancellationReason('');
    } catch (err) {
      alert('Failed to cancel booking: ' + err.message);
    } finally {
      setSubmittingCancel(false);
    }
  };

  const handleToggleAmenityStatus = async (amenity) => {
    const willBeActive = !amenity.isActive;
    let reason = null;
    if (!willBeActive) {
      reason = window.prompt(
        `Enter maintenance reason for disabling "${amenity.name}":`,
        'Scheduled facility maintenance'
      );
      if (reason === null) return; // user cancelled prompt
    }

    try {
      await amenityApi.toggleAmenityStatus(amenity.id, {
        isActive: willBeActive,
        maintenanceReason: reason,
      });
      fetchAdminData();
      fetchActiveAmenities();
    } catch (err) {
      alert('Failed to update status: ' + err.message);
    }
  };

  const handleSaveAmenity = async (e) => {
    e.preventDefault();
    try {
      if (editingAmenity) {
        await amenityApi.updateAmenity(editingAmenity.id, amenityFormData);
      } else {
        await amenityApi.createAmenity(amenityFormData);
      }
      setShowCreateAmenityModal(false);
      setEditingAmenity(null);
      fetchAdminData();
      fetchActiveAmenities();
    } catch (err) {
      alert('Error saving amenity: ' + err.message);
    }
  };

  const handleCopyReference = (ref) => {
    navigator.clipboard.writeText(ref);
    setCopiedRef(ref);
    setTimeout(() => setCopiedRef(null), 2500);
  };

  // Helper date buttons for quick selection
  const quickDates = Array.from({ length: 7 }).map((_, idx) => {
    const d = new Date();
    d.setDate(d.getDate() + idx);
    const dateStr = d.toISOString().split('T')[0];
    const dayLabel = idx === 0 ? 'Today' : idx === 1 ? 'Tomorrow' : d.toLocaleDateString('en-US', { weekday: 'short' });
    const formatted = d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
    return { dateStr, dayLabel, formatted };
  });

  const filteredAmenities = amenities.filter(
    (a) => selectedCategory === 'ALL' || a.category === selectedCategory
  );

  const filteredMyBookings = myBookings.filter(
    (b) => bookingFilterStatus === 'ALL' || b.status === bookingFilterStatus
  );

  return (
    <div className="space-y-6">
      {/* Top Header / Context banner */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white p-6 rounded-2xl border border-slate-200/80 shadow-xs">
        <div>
          <div className="flex items-center gap-2.5">
            <span className="p-2 rounded-xl bg-teal-50 text-teal-600 border border-teal-200/60">
              <Sparkles className="w-5 h-5" />
            </span>
            <h1 className="text-2xl font-bold text-slate-900 tracking-tight">
              Amenity & Clubhouse Bookings
            </h1>
          </div>
          <p className="text-sm text-slate-500 mt-1">
            Reserve world-class township recreation facilities, view real-time slot availability, and manage your bookings.
          </p>
        </div>

        {/* Global tab navigation */}
        <div className="flex items-center gap-1.5 p-1 bg-slate-100 rounded-xl border border-slate-200 self-start md:self-auto">
          <button
            onClick={() => setActiveTab('explore')}
            className={`px-4 py-2 rounded-lg text-xs font-semibold transition cursor-pointer flex items-center gap-2 ${
              activeTab === 'explore'
                ? 'bg-white text-slate-900 shadow-xs border border-slate-200/60'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            <Calendar className="w-4 h-4 text-teal-600" />
            Browse & Book
          </button>

          <button
            onClick={() => setActiveTab('my-bookings')}
            className={`px-4 py-2 rounded-lg text-xs font-semibold transition cursor-pointer flex items-center gap-2 ${
              activeTab === 'my-bookings'
                ? 'bg-white text-slate-900 shadow-xs border border-slate-200/60'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            <Clock className="w-4 h-4 text-emerald-600" />
            My Bookings
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
              <ShieldCheck className="w-4 h-4 text-indigo-600" />
              Help Desk Admin
            </button>
          )}
        </div>
      </div>

      {/* ==================================================================== */}
      {/* TAB 1: BROWSE & BOOK SLOTS                                           */}
      {/* ==================================================================== */}
      {activeTab === 'explore' && (
        <div className="space-y-6">
          {/* Category Filter Pills */}
          <div className="flex items-center gap-2 overflow-x-auto pb-1">
            {CATEGORIES.map((cat) => (
              <button
                key={cat.id}
                onClick={() => setSelectedCategory(cat.id)}
                className={`px-3.5 py-1.5 rounded-full text-xs font-medium whitespace-nowrap transition cursor-pointer ${
                  selectedCategory === cat.id
                    ? 'bg-slate-900 text-white shadow-xs'
                    : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-50'
                }`}
              >
                {cat.label}
              </button>
            ))}
          </div>

          {/* Loading / Error States */}
          {loadingAmenities && (
            <div className="bg-white p-12 rounded-2xl border border-slate-200 flex flex-col items-center justify-center text-slate-500">
              <RefreshCw className="w-8 h-8 animate-spin text-teal-600 mb-3" />
              <p className="text-sm font-medium">Loading township amenities...</p>
            </div>
          )}

          {amenitiesError && (
            <div className="p-4 rounded-xl bg-red-50 border border-red-200 text-red-700 text-sm flex items-center gap-3">
              <AlertCircle className="w-5 h-5 shrink-0" />
              <div>{amenitiesError}</div>
            </div>
          )}

          {/* Amenities Grid */}
          {!loadingAmenities && !amenitiesError && (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
              {filteredAmenities.map((amenity) => {
                const isSelected = selectedAmenity?.id === amenity.id;
                return (
                  <div
                    key={amenity.id}
                    onClick={() => setSelectedAmenity(amenity)}
                    className={`bg-white rounded-2xl border p-5 transition cursor-pointer flex flex-col justify-between hover:shadow-md ${
                      isSelected
                        ? 'border-teal-500 ring-2 ring-teal-500/10 shadow-xs'
                        : 'border-slate-200/80 hover:border-slate-300'
                    }`}
                  >
                    <div>
                      {/* Top Badges */}
                      <div className="flex items-center justify-between gap-2 mb-3">
                        <span className="text-[11px] font-semibold tracking-wide uppercase px-2.5 py-0.5 rounded-full bg-slate-100 text-slate-700 border border-slate-200">
                          {amenity.category}
                        </span>
                        {amenity.isActive ? (
                          <span className="inline-flex items-center gap-1.5 text-[11px] font-medium text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200">
                            <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
                            Operational
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 text-[11px] font-medium text-amber-700 bg-amber-50 px-2 py-0.5 rounded-full border border-amber-200">
                            <Ban className="w-3 h-3" />
                            Maintenance
                          </span>
                        )}
                      </div>

                      {/* Title & Description */}
                      <h3 className="text-base font-bold text-slate-900 group-hover:text-teal-600 transition">
                        {amenity.name}
                      </h3>
                      <p className="text-xs text-slate-500 mt-1 line-clamp-2 leading-relaxed">
                        {amenity.description || 'Modern society amenity facility.'}
                      </p>

                      {/* Location & Metadata */}
                      <div className="mt-4 pt-3 border-t border-slate-100 space-y-2 text-xs text-slate-600">
                        <div className="flex items-center gap-2">
                          <MapPin className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                          <span className="truncate">{amenity.location || 'Central Amenity Complex'}</span>
                        </div>
                        <div className="flex items-center gap-2">
                          <Clock className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                          <span>
                            {amenity.openTime?.slice(0, 5)} - {amenity.closeTime?.slice(0, 5)} ({amenity.slotDurationMinutes} min slots)
                          </span>
                        </div>
                        <div className="flex items-center gap-2">
                          <Users className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                          <span>Max Capacity: {amenity.slotCapacity} attendees / slot</span>
                        </div>
                      </div>
                    </div>

                    {/* Bottom Actions */}
                    <div className="mt-5 pt-3 border-t border-slate-100 flex items-center justify-between gap-2">
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          setViewingRulesAmenity(amenity);
                        }}
                        className="text-xs font-semibold text-slate-500 hover:text-slate-800 flex items-center gap-1 cursor-pointer"
                      >
                        <Info className="w-3.5 h-3.5" />
                        Rules
                      </button>

                      <button
                        type="button"
                        onClick={() => setSelectedAmenity(amenity)}
                        className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition cursor-pointer flex items-center gap-1.5 ${
                          isSelected
                            ? 'bg-teal-600 text-white shadow-xs'
                            : 'bg-slate-100 text-slate-800 hover:bg-slate-200'
                        }`}
                      >
                        {isSelected ? 'Viewing Slots' : 'Select Slots'}
                        <ChevronRight className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          {/* Real-time Slot Availability Panel */}
          {selectedAmenity && (
            <div className="bg-white rounded-2xl border border-slate-200/80 p-6 shadow-xs space-y-6">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-100">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-bold uppercase tracking-wider text-teal-600 bg-teal-50 px-2 py-0.5 rounded-md border border-teal-200/50">
                      Step 2: Choose Slot
                    </span>
                    <h2 className="text-lg font-bold text-slate-900">
                      {selectedAmenity.name} Slots
                    </h2>
                  </div>
                  <p className="text-xs text-slate-500 mt-0.5">
                    Operating Hours: {selectedAmenity.openTime?.slice(0, 5)} to {selectedAmenity.closeTime?.slice(0, 5)} • {selectedAmenity.slotDurationMinutes} min per slot
                  </p>
                </div>

                {/* Custom Date Input */}
                <div className="flex items-center gap-2 self-start sm:self-auto">
                  <span className="text-xs text-slate-400 font-medium">Select Date:</span>
                  <input
                    type="date"
                    min={new Date().toISOString().split('T')[0]}
                    value={selectedDate}
                    onChange={(e) => setSelectedDate(e.target.value)}
                    className="px-3 py-1.5 text-xs font-medium rounded-lg border border-slate-200 bg-slate-50 text-slate-800 focus:outline-hidden focus:ring-2 focus:ring-teal-500"
                  />
                </div>
              </div>

              {/* Quick Date Selector Pills */}
              <div className="flex items-center gap-2 overflow-x-auto pb-1">
                {quickDates.map((item) => (
                  <button
                    key={item.dateStr}
                    type="button"
                    onClick={() => setSelectedDate(item.dateStr)}
                    className={`px-3.5 py-2 rounded-xl text-center min-w-[5.5rem] transition cursor-pointer border ${
                      selectedDate === item.dateStr
                        ? 'bg-[#0c1322] text-white border-[#0c1322] shadow-xs'
                        : 'bg-white text-slate-700 border-slate-200 hover:bg-slate-50'
                    }`}
                  >
                    <div className="text-[11px] font-semibold">{item.dayLabel}</div>
                    <div className="text-[11px] text-slate-400">{item.formatted}</div>
                  </button>
                ))}
              </div>

              {/* Slot Availability Grid */}
              {loadingSlots ? (
                <div className="py-12 flex flex-col items-center justify-center text-slate-400">
                  <RefreshCw className="w-6 h-6 animate-spin text-teal-600 mb-2" />
                  <p className="text-xs">Checking real-time slot capacities...</p>
                </div>
              ) : slots.length === 0 ? (
                <div className="py-10 text-center text-slate-400 text-xs">
                  No slots configured for this facility or hours.
                </div>
              ) : (
                <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3">
                  {slots.map((slot, idx) => {
                    return (
                      <div
                        key={idx}
                        className={`p-3 rounded-xl border flex flex-col justify-between transition ${
                          slot.isBookedByMyFlat
                            ? 'bg-amber-50/70 border-amber-300 ring-1 ring-amber-300'
                            : slot.isAvailable
                            ? 'bg-white border-slate-200 hover:border-teal-500 hover:shadow-xs'
                            : 'bg-slate-50 border-slate-200 opacity-60'
                        }`}
                      >
                        <div>
                          <div className="text-xs font-bold text-slate-800">
                            {slot.startTime?.slice(0, 5)} - {slot.endTime?.slice(0, 5)}
                          </div>
                          <div className="text-[11px] text-slate-500 mt-1">
                            {slot.isBookedByMyFlat ? (
                              <span className="font-semibold text-amber-700">Booked by You</span>
                            ) : slot.isPast ? (
                              <span className="text-slate-400">Past slot</span>
                            ) : slot.availableSpots > 0 ? (
                              <span className="font-medium text-emerald-600">
                                {slot.availableSpots} of {slot.totalCapacity} spots left
                              </span>
                            ) : (
                              <span className="font-medium text-red-500">Full</span>
                            )}
                          </div>
                        </div>

                        <div className="mt-3">
                          {slot.isAvailable ? (
                            <button
                              type="button"
                              onClick={() => handleOpenBookingModal(slot)}
                              className="w-full py-1 px-2 rounded-lg bg-teal-600 hover:bg-teal-700 text-white text-[11px] font-semibold transition cursor-pointer text-center"
                            >
                              Book Slot
                            </button>
                          ) : (
                            <div className="text-[10px] text-center font-medium py-1 px-2 rounded-lg bg-slate-100 text-slate-400">
                              {slot.isBookedByMyFlat ? 'Reserved' : slot.isPast ? 'Expired' : 'Unavailable'}
                            </div>
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {/* ==================================================================== */}
      {/* TAB 2: RESIDENT'S MY BOOKINGS                                        */}
      {/* ==================================================================== */}
      {activeTab === 'my-bookings' && (
        <div className="space-y-6">
          {/* Status Filters */}
          <div className="flex items-center justify-between gap-4">
            <div className="flex items-center gap-2">
              {['ALL', 'CONFIRMED', 'COMPLETED', 'CANCELLED'].map((st) => (
                <button
                  key={st}
                  onClick={() => setBookingFilterStatus(st)}
                  className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition cursor-pointer ${
                    bookingFilterStatus === st
                      ? 'bg-slate-900 text-white shadow-xs'
                      : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-50'
                  }`}
                >
                  {st}
                </button>
              ))}
            </div>

            <button
              onClick={fetchMyBookings}
              className="px-3 py-1.5 rounded-lg border border-slate-200 bg-white text-slate-600 hover:bg-slate-50 text-xs font-medium flex items-center gap-1.5 cursor-pointer"
            >
              <RefreshCw className="w-3.5 h-3.5" />
              Refresh
            </button>
          </div>

          {loadingMyBookings ? (
            <div className="bg-white p-12 rounded-2xl border border-slate-200 flex flex-col items-center justify-center text-slate-400">
              <RefreshCw className="w-7 h-7 animate-spin text-teal-600 mb-2" />
              <p className="text-xs">Retrieving your booking records...</p>
            </div>
          ) : filteredMyBookings.length === 0 ? (
            <div className="bg-white p-12 rounded-2xl border border-slate-200 text-center">
              <Calendar className="w-10 h-10 text-slate-300 mx-auto mb-2" />
              <h4 className="text-sm font-bold text-slate-800">No Bookings Found</h4>
              <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
                You have not booked any amenities matching this filter. Switch to the Explore tab to reserve a session.
              </p>
              <button
                onClick={() => setActiveTab('explore')}
                className="mt-4 px-4 py-2 rounded-xl bg-teal-600 hover:bg-teal-700 text-white text-xs font-semibold cursor-pointer"
              >
                Browse Amenities
              </button>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {filteredMyBookings.map((b) => (
                <div
                  key={b.id}
                  className="bg-white rounded-2xl border border-slate-200 p-5 shadow-xs flex flex-col justify-between"
                >
                  <div>
                    {/* Top Reference & Status */}
                    <div className="flex items-center justify-between gap-2 mb-3">
                      <div className="flex items-center gap-2">
                        <span className="font-mono text-xs font-bold text-slate-900 bg-slate-100 px-2 py-0.5 rounded-md border border-slate-200 flex items-center gap-1.5">
                          {b.bookingReference}
                          <button
                            type="button"
                            onClick={() => handleCopyReference(b.bookingReference)}
                            className="text-slate-400 hover:text-slate-700 cursor-pointer"
                            title="Copy reference"
                          >
                            {copiedRef === b.bookingReference ? (
                              <Check className="w-3.5 h-3.5 text-emerald-600" />
                            ) : (
                              <Copy className="w-3.5 h-3.5" />
                            )}
                          </button>
                        </span>
                        <span className="text-[11px] font-semibold text-slate-600">
                          {b.flatNumber}
                        </span>
                      </div>

                      <span
                        className={`text-[11px] font-semibold px-2.5 py-0.5 rounded-full border ${
                          b.status === 'CONFIRMED'
                            ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                            : b.status === 'COMPLETED'
                            ? 'bg-slate-100 text-slate-700 border-slate-200'
                            : 'bg-red-50 text-red-700 border-red-200'
                        }`}
                      >
                        {b.status}
                      </span>
                    </div>

                    <h4 className="text-base font-bold text-slate-900">{b.amenityName}</h4>
                    <p className="text-xs text-slate-500 flex items-center gap-1 mt-0.5">
                      <MapPin className="w-3 h-3" />
                      {b.amenityLocation || 'Society Premises'}
                    </p>

                    {/* Schedule info box */}
                    <div className="mt-3.5 p-3 rounded-xl bg-slate-50 border border-slate-100 space-y-1.5 text-xs text-slate-700">
                      <div className="flex items-center justify-between">
                        <span className="text-slate-400 font-medium">Date:</span>
                        <span className="font-semibold text-slate-900">{b.bookingDate}</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-slate-400 font-medium">Time Slot:</span>
                        <span className="font-semibold text-slate-900">{b.slotLabel}</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-slate-400 font-medium">Attendees:</span>
                        <span className="font-semibold text-slate-900">{b.attendeeCount} Person(s)</span>
                      </div>
                      {b.purpose && (
                        <div className="flex items-center justify-between pt-1 border-t border-slate-200/60">
                          <span className="text-slate-400 font-medium">Purpose:</span>
                          <span className="font-medium text-slate-800 truncate max-w-[200px]">{b.purpose}</span>
                        </div>
                      )}
                    </div>

                    {b.cancellationReason && (
                      <div className="mt-2 text-xs text-red-600 bg-red-50/50 p-2 rounded-lg border border-red-100">
                        Reason: {b.cancellationReason}
                      </div>
                    )}
                  </div>

                  {/* Cancel Button */}
                  <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between">
                    <span className="text-[11px] text-slate-400">
                      {b.status === 'CONFIRMED' && b.canCancel && 'Eligible for self-cancellation'}
                      {b.status === 'CONFIRMED' && !b.canCancel && 'Cancellation deadline passed'}
                    </span>

                    {b.status === 'CONFIRMED' && b.canCancel && (
                      <button
                        type="button"
                        onClick={() => {
                          setCancellingBooking(b);
                          setCancellationReason('');
                        }}
                        className="px-3 py-1.5 rounded-lg border border-red-200 text-red-600 hover:bg-red-50 text-xs font-semibold transition cursor-pointer"
                      >
                        Cancel Booking
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* ==================================================================== */}
      {/* TAB 3: HELP DESK ADMINISTRATION                                      */}
      {/* ==================================================================== */}
      {activeTab === 'admin' && isHelpDesk && (
        <div className="space-y-6">
          {/* KPI Stat Cards */}
          {adminOverview && (
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-xs">
                <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Total Facilities</span>
                <div className="text-2xl font-bold text-slate-900 mt-1">{adminOverview.totalAmenities}</div>
                <span className="text-[11px] text-teal-600 mt-1 block">Master Amenity Registry</span>
              </div>

              <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-xs">
                <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Active & Open</span>
                <div className="text-2xl font-bold text-emerald-600 mt-1">{adminOverview.activeAmenities}</div>
                <span className="text-[11px] text-slate-500 mt-1 block">Currently operational</span>
              </div>

              <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-xs">
                <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Bookings Today</span>
                <div className="text-2xl font-bold text-slate-900 mt-1">{adminOverview.bookingsToday}</div>
                <span className="text-[11px] text-slate-500 mt-1 block">Active facility slots</span>
              </div>

              <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-xs">
                <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Upcoming (7 Days)</span>
                <div className="text-2xl font-bold text-indigo-600 mt-1">{adminOverview.upcomingBookingsWeek}</div>
                <span className="text-[11px] text-slate-500 mt-1 block">Scheduled reservations</span>
              </div>
            </div>
          )}

          {/* Amenity Master Catalog Management */}
          <div className="bg-white rounded-2xl border border-slate-200 shadow-xs overflow-hidden">
            <div className="p-5 border-b border-slate-100 flex items-center justify-between">
              <div>
                <h3 className="text-base font-bold text-slate-900">Amenity Master Catalog</h3>
                <p className="text-xs text-slate-500 mt-0.5">Configure hours, capacities, slot durations, and maintenance states.</p>
              </div>

              <button
                type="button"
                onClick={() => {
                  setEditingAmenity(null);
                  setAmenityFormData({
                    name: '',
                    code: '',
                    category: 'FITNESS',
                    description: '',
                    location: '',
                    openTime: '06:00',
                    closeTime: '22:00',
                    slotDurationMinutes: 60,
                    slotCapacity: 10,
                    maxActiveBookingsPerFlat: 2,
                    cancelDeadlineHours: 2,
                    rulesAndGuidelines: '',
                  });
                  setShowCreateAmenityModal(true);
                }}
                className="px-3.5 py-2 rounded-xl bg-slate-900 hover:bg-slate-800 text-white text-xs font-semibold flex items-center gap-1.5 cursor-pointer shadow-xs"
              >
                <Plus className="w-4 h-4" />
                Add Facility
              </button>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs text-slate-600">
                <thead className="bg-slate-50 text-slate-400 font-semibold uppercase tracking-wider border-b border-slate-100">
                  <tr>
                    <th className="px-5 py-3.5">Facility / Code</th>
                    <th className="px-4 py-3.5">Category</th>
                    <th className="px-4 py-3.5">Operating Hours</th>
                    <th className="px-4 py-3.5">Capacity / Slot</th>
                    <th className="px-4 py-3.5">Status</th>
                    <th className="px-5 py-3.5 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {adminAmenities.map((a) => (
                    <tr key={a.id} className="hover:bg-slate-50/60 transition">
                      <td className="px-5 py-4">
                        <div className="font-bold text-slate-900">{a.name}</div>
                        <div className="font-mono text-[11px] text-slate-400">{a.code}</div>
                      </td>
                      <td className="px-4 py-4">
                        <span className="px-2 py-0.5 rounded-md bg-slate-100 text-slate-700 font-medium text-[11px]">
                          {a.category}
                        </span>
                      </td>
                      <td className="px-4 py-4">
                        {a.openTime?.slice(0, 5)} - {a.closeTime?.slice(0, 5)} ({a.slotDurationMinutes}m)
                      </td>
                      <td className="px-4 py-4">
                        {a.slotCapacity} attendees (Max {a.maxActiveBookingsPerFlat} / flat)
                      </td>
                      <td className="px-4 py-4">
                        {a.isActive ? (
                          <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200">
                            Active
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-amber-700 bg-amber-50 px-2 py-0.5 rounded-full border border-amber-200">
                            Maintenance
                          </span>
                        )}
                      </td>
                      <td className="px-5 py-4 text-right space-x-2">
                        <button
                          type="button"
                          onClick={() => {
                            setEditingAmenity(a);
                            setAmenityFormData({
                              name: a.name,
                              code: a.code,
                              category: a.category,
                              description: a.description || '',
                              location: a.location || '',
                              openTime: a.openTime?.slice(0, 5) || '06:00',
                              closeTime: a.closeTime?.slice(0, 5) || '22:00',
                              slotDurationMinutes: a.slotDurationMinutes || 60,
                              slotCapacity: a.slotCapacity || 10,
                              maxActiveBookingsPerFlat: a.maxActiveBookingsPerFlat || 2,
                              cancelDeadlineHours: a.cancelDeadlineHours || 2,
                              rulesAndGuidelines: a.rulesAndGuidelines || '',
                            });
                            setShowCreateAmenityModal(true);
                          }}
                          className="p-1.5 rounded-lg border border-slate-200 text-slate-600 hover:bg-slate-100 cursor-pointer"
                          title="Edit facility configuration"
                        >
                          <Edit2 className="w-3.5 h-3.5" />
                        </button>

                        <button
                          type="button"
                          onClick={() => handleToggleAmenityStatus(a)}
                          className={`p-1.5 rounded-lg border cursor-pointer ${
                            a.isActive
                              ? 'border-amber-200 text-amber-600 hover:bg-amber-50'
                              : 'border-emerald-200 text-emerald-600 hover:bg-emerald-50'
                          }`}
                          title={a.isActive ? 'Disable for maintenance' : 'Enable facility'}
                        >
                          <Power className="w-3.5 h-3.5" />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Township All Bookings Master Log */}
          <div className="bg-white rounded-2xl border border-slate-200 shadow-xs overflow-hidden">
            <div className="p-5 border-b border-slate-100 flex flex-col md:flex-row md:items-center justify-between gap-4">
              <div>
                <h3 className="text-base font-bold text-slate-900">Master Bookings Log</h3>
                <p className="text-xs text-slate-500 mt-0.5">Comprehensive township reservations across all 28 towers.</p>
              </div>

              {/* Filters */}
              <div className="flex flex-wrap items-center gap-2">
                <input
                  type="date"
                  value={adminBookingDateFilter}
                  onChange={(e) => setAdminBookingDateFilter(e.target.value)}
                  className="px-2.5 py-1.5 text-xs rounded-lg border border-slate-200 bg-slate-50 text-slate-800"
                />

                <select
                  value={adminAmenityFilter}
                  onChange={(e) => setAdminAmenityFilter(e.target.value)}
                  className="px-2.5 py-1.5 text-xs rounded-lg border border-slate-200 bg-slate-50 text-slate-800"
                >
                  <option value="ALL">All Facilities</option>
                  {adminAmenities.map((a) => (
                    <option key={a.id} value={a.id}>
                      {a.name}
                    </option>
                  ))}
                </select>

                <select
                  value={adminStatusFilter}
                  onChange={(e) => setAdminStatusFilter(e.target.value)}
                  className="px-2.5 py-1.5 text-xs rounded-lg border border-slate-200 bg-slate-50 text-slate-800"
                >
                  <option value="ALL">All Statuses</option>
                  <option value="CONFIRMED">CONFIRMED</option>
                  <option value="COMPLETED">COMPLETED</option>
                  <option value="CANCELLED">CANCELLED</option>
                </select>

                <button
                  type="button"
                  onClick={fetchAdminData}
                  className="px-3 py-1.5 rounded-lg bg-teal-600 text-white text-xs font-semibold cursor-pointer"
                >
                  Filter
                </button>
              </div>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs text-slate-600">
                <thead className="bg-slate-50 text-slate-400 font-semibold uppercase tracking-wider border-b border-slate-100">
                  <tr>
                    <th className="px-5 py-3.5">Reference / Resident</th>
                    <th className="px-4 py-3.5">Flat Number</th>
                    <th className="px-4 py-3.5">Amenity</th>
                    <th className="px-4 py-3.5">Date & Slot</th>
                    <th className="px-4 py-3.5">Attendees</th>
                    <th className="px-4 py-3.5">Status</th>
                    <th className="px-5 py-3.5 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {adminBookings.length === 0 ? (
                    <tr>
                      <td colSpan={7} className="px-5 py-8 text-center text-slate-400">
                        No bookings matching criteria.
                      </td>
                    </tr>
                  ) : (
                    adminBookings.map((b) => (
                      <tr key={b.id} className="hover:bg-slate-50/60 transition">
                        <td className="px-5 py-3.5">
                          <div className="font-mono font-bold text-slate-900">{b.bookingReference}</div>
                          <div className="text-[11px] text-slate-500">{b.residentName}</div>
                        </td>
                        <td className="px-4 py-3.5 font-semibold text-slate-900">{b.flatNumber}</td>
                        <td className="px-4 py-3.5 font-medium text-slate-800">{b.amenityName}</td>
                        <td className="px-4 py-3.5">
                          <div className="font-medium text-slate-900">{b.bookingDate}</div>
                          <div className="text-[11px] text-slate-400">{b.slotLabel}</div>
                        </td>
                        <td className="px-4 py-3.5">{b.attendeeCount} Person(s)</td>
                        <td className="px-4 py-3.5">
                          <span
                            className={`px-2 py-0.5 rounded-full text-[10px] font-bold border ${
                              b.status === 'CONFIRMED'
                                ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
                                : b.status === 'COMPLETED'
                                ? 'bg-slate-100 text-slate-700 border-slate-200'
                                : 'bg-red-50 text-red-700 border-red-200'
                            }`}
                          >
                            {b.status}
                          </span>
                        </td>
                        <td className="px-5 py-3.5 text-right">
                          {b.status === 'CONFIRMED' && (
                            <button
                              type="button"
                              onClick={() => {
                                setCancellingBooking(b);
                                setCancellationReason('');
                              }}
                              className="px-2.5 py-1 rounded-md border border-red-200 text-red-600 hover:bg-red-50 text-[11px] font-semibold cursor-pointer"
                            >
                              Admin Cancel
                            </button>
                          )}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* ==================================================================== */}
      {/* MODAL 1: BOOKING CONFIRMATION FORM                                   */}
      {/* ==================================================================== */}
      {bookingSlot && selectedAmenity && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-lg w-full p-6 shadow-2xl border border-slate-200 relative animate-in fade-in zoom-in-95 duration-200">
            <button
              onClick={() => setBookingSlot(null)}
              className="absolute top-5 right-5 text-slate-400 hover:text-slate-600 p-1.5 rounded-lg hover:bg-slate-100 transition cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>

            <div className="flex items-center gap-3 mb-5">
              <div className="p-3 rounded-2xl bg-teal-50 text-teal-600 border border-teal-200/50">
                <Calendar className="w-6 h-6" />
              </div>
              <div>
                <h3 className="text-lg font-bold text-slate-900">Confirm Slot Reservation</h3>
                <p className="text-xs text-slate-500">{selectedAmenity.name}</p>
              </div>
            </div>

            {bookingError && (
              <div className="mb-4 p-3 rounded-xl bg-red-50 border border-red-200 text-red-700 text-xs flex items-center gap-2">
                <AlertCircle className="w-4 h-4 shrink-0" />
                <span>{bookingError}</span>
              </div>
            )}

            <form onSubmit={handleConfirmBooking} className="space-y-4">
              {/* Selected Timing Recap */}
              <div className="p-3.5 rounded-2xl bg-slate-50 border border-slate-100 text-xs space-y-1.5">
                <div className="flex justify-between">
                  <span className="text-slate-400 font-medium">Date:</span>
                  <span className="font-semibold text-slate-900">{selectedDate}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400 font-medium">Slot Window:</span>
                  <span className="font-semibold text-teal-700">{bookingSlot.slotLabel}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-400 font-medium">Available Spots:</span>
                  <span className="font-semibold text-slate-900">
                    {bookingSlot.availableSpots} of {bookingSlot.totalCapacity}
                  </span>
                </div>
              </div>

              {/* Flat Selector */}
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Apartment Flat:
                </label>
                {user?.flats && user.flats.length > 0 ? (
                  <select
                    value={bookingFlatId}
                    onChange={(e) => setBookingFlatId(Number(e.target.value))}
                    className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 bg-white text-slate-800 focus:ring-2 focus:ring-teal-500"
                  >
                    {user.flats.map((f) => (
                      <option key={f.id} value={f.id}>
                        {f.towerId ? `${f.towerId}-${f.flatNumber}` : f.flatNumber} (Area: {f.carpetAreaSqFt} sq.ft)
                      </option>
                    ))}
                  </select>
                ) : (
                  <input
                    type="number"
                    value={bookingFlatId}
                    onChange={(e) => setBookingFlatId(Number(e.target.value))}
                    className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 bg-white text-slate-800"
                    placeholder="Enter Flat ID"
                    required
                  />
                )}
              </div>

              {/* Attendee Count */}
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Number of Attendees (Max: {Math.min(bookingSlot.availableSpots, selectedAmenity.slotCapacity)}):
                </label>
                <input
                  type="number"
                  min={1}
                  max={Math.min(bookingSlot.availableSpots, selectedAmenity.slotCapacity)}
                  value={bookingAttendeeCount}
                  onChange={(e) => setBookingAttendeeCount(e.target.value)}
                  className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 bg-white text-slate-800 focus:ring-2 focus:ring-teal-500"
                  required
                />
              </div>

              {/* Purpose / Note */}
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Purpose / Notes (Optional):
                </label>
                <input
                  type="text"
                  value={bookingPurpose}
                  onChange={(e) => setBookingPurpose(e.target.value)}
                  placeholder="e.g. Morning cardio workout, Badminton practice, Birthday party"
                  className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 bg-white text-slate-800 focus:ring-2 focus:ring-teal-500"
                />
              </div>

              {/* Cancellation policy reminder */}
              <div className="p-3 rounded-xl bg-teal-50/60 border border-teal-100 text-[11px] text-teal-800 flex items-start gap-2">
                <Info className="w-4 h-4 shrink-0 text-teal-600 mt-0.5" />
                <span>
                  Free cancellation is permitted up to <strong>{selectedAmenity.cancelDeadlineHours} hours</strong> before the slot begins. Please adhere to society guidelines.
                </span>
              </div>

              {/* Form buttons */}
              <div className="pt-2 flex items-center justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setBookingSlot(null)}
                  className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-100 transition cursor-pointer"
                >
                  Cancel
                </button>

                <button
                  type="submit"
                  disabled={submittingBooking}
                  className="px-5 py-2.5 rounded-xl bg-teal-600 hover:bg-teal-700 text-white text-xs font-semibold shadow-xs transition flex items-center gap-1.5 cursor-pointer disabled:opacity-50"
                >
                  {submittingBooking && <RefreshCw className="w-3.5 h-3.5 animate-spin" />}
                  {submittingBooking ? 'Securing Slot...' : 'Confirm & Reserve Slot'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ==================================================================== */}
      {/* MODAL 2: BOOKING SUCCESS CONFIRMATION MODAL                          */}
      {/* ==================================================================== */}
      {bookingSuccessData && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl border border-slate-200 text-center animate-in fade-in zoom-in-95 duration-200">
            <div className="w-14 h-14 rounded-2xl bg-emerald-50 text-emerald-600 border border-emerald-200 flex items-center justify-center mx-auto mb-4">
              <CheckCircle2 className="w-8 h-8" />
            </div>

            <h3 className="text-xl font-bold text-slate-900">Slot Confirmed!</h3>
            <p className="text-xs text-slate-500 mt-1">
              Your reservation has been successfully registered in the township system.
            </p>

            {/* Reference box */}
            <div className="my-5 p-4 rounded-2xl bg-slate-50 border border-slate-200 flex items-center justify-between">
              <div>
                <span className="text-[10px] font-semibold text-slate-400 uppercase tracking-wider block text-left">
                  Booking Reference
                </span>
                <span className="font-mono text-base font-bold text-slate-900">
                  {bookingSuccessData.bookingReference}
                </span>
              </div>
              <button
                type="button"
                onClick={() => handleCopyReference(bookingSuccessData.bookingReference)}
                className="px-3 py-1.5 rounded-lg border border-slate-200 bg-white text-slate-700 hover:bg-slate-50 text-xs font-semibold flex items-center gap-1 cursor-pointer"
              >
                {copiedRef === bookingSuccessData.bookingReference ? (
                  <>
                    <Check className="w-3.5 h-3.5 text-emerald-600" />
                    Copied
                  </>
                ) : (
                  <>
                    <Copy className="w-3.5 h-3.5" />
                    Copy
                  </>
                )}
              </button>
            </div>

            {/* Details recap */}
            <div className="text-xs text-slate-600 text-left space-y-2 p-3 bg-slate-50/50 rounded-xl border border-slate-100">
              <div className="flex justify-between">
                <span className="text-slate-400">Amenity:</span>
                <span className="font-semibold text-slate-900">{bookingSuccessData.amenityName}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Date:</span>
                <span className="font-semibold text-slate-900">{bookingSuccessData.bookingDate}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Slot:</span>
                <span className="font-semibold text-teal-700">{bookingSuccessData.slotLabel}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Flat:</span>
                <span className="font-semibold text-slate-900">{bookingSuccessData.flatNumber}</span>
              </div>
            </div>

            <div className="mt-6 flex items-center gap-3">
              <button
                type="button"
                onClick={() => {
                  setBookingSuccessData(null);
                  setActiveTab('my-bookings');
                }}
                className="flex-1 py-2.5 rounded-xl border border-slate-200 hover:bg-slate-50 text-slate-800 text-xs font-semibold cursor-pointer"
              >
                View My Bookings
              </button>
              <button
                type="button"
                onClick={() => setBookingSuccessData(null)}
                className="flex-1 py-2.5 rounded-xl bg-teal-600 hover:bg-teal-700 text-white text-xs font-semibold cursor-pointer shadow-xs"
              >
                Done
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ==================================================================== */}
      {/* MODAL 3: GUIDELINES & RULES MODAL                                    */}
      {/* ==================================================================== */}
      {viewingRulesAmenity && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-lg w-full p-6 shadow-2xl border border-slate-200 relative animate-in fade-in zoom-in-95 duration-200">
            <button
              onClick={() => setViewingRulesAmenity(null)}
              className="absolute top-5 right-5 text-slate-400 hover:text-slate-600 p-1.5 rounded-lg hover:bg-slate-100 transition cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>

            <div className="flex items-center gap-3 mb-4">
              <div className="p-3 rounded-2xl bg-indigo-50 text-indigo-600 border border-indigo-200/50">
                <FileText className="w-6 h-6" />
              </div>
              <div>
                <h3 className="text-lg font-bold text-slate-900">Guidelines & Rules</h3>
                <p className="text-xs text-slate-500">{viewingRulesAmenity.name}</p>
              </div>
            </div>

            <div className="bg-slate-50 p-4 rounded-2xl border border-slate-100 text-xs text-slate-700 space-y-2 whitespace-pre-line leading-relaxed max-h-80 overflow-y-auto">
              {viewingRulesAmenity.rulesAndGuidelines ||
                '1. Standard township etiquette and cleanliness rules apply.\n2. Please carry valid resident identification if requested by clubhouse staff.\n3. Return sports equipment after completion.'}
            </div>

            <div className="mt-5 flex justify-end">
              <button
                type="button"
                onClick={() => setViewingRulesAmenity(null)}
                className="px-5 py-2 rounded-xl bg-slate-900 hover:bg-slate-800 text-white text-xs font-semibold cursor-pointer"
              >
                I Understand
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ==================================================================== */}
      {/* MODAL 4: CANCELLATION MODAL                                          */}
      {/* ==================================================================== */}
      {cancellingBooking && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl border border-slate-200 relative animate-in fade-in zoom-in-95 duration-200">
            <button
              onClick={() => setCancellingBooking(null)}
              className="absolute top-5 right-5 text-slate-400 hover:text-slate-600 p-1.5 rounded-lg hover:bg-slate-100 transition cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>

            <div className="flex items-center gap-3 mb-4">
              <div className="p-3 rounded-2xl bg-red-50 text-red-600 border border-red-200/50">
                <AlertCircle className="w-6 h-6" />
              </div>
              <div>
                <h3 className="text-lg font-bold text-slate-900">Cancel Booking?</h3>
                <p className="text-xs text-slate-500 font-mono">{cancellingBooking.bookingReference}</p>
              </div>
            </div>

            <p className="text-xs text-slate-600 mb-4">
              Are you sure you want to cancel your session for <strong>{cancellingBooking.amenityName}</strong> on{' '}
              <strong>{cancellingBooking.bookingDate} ({cancellingBooking.slotLabel})</strong>? The reserved spots will be released back to the township pool.
            </p>

            <div className="mb-4">
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Reason for Cancellation (Optional):
              </label>
              <input
                type="text"
                value={cancellationReason}
                onChange={(e) => setCancellationReason(e.target.value)}
                placeholder="e.g. Schedule conflict, unwell, travel"
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 bg-white text-slate-800 focus:ring-2 focus:ring-red-500"
              />
            </div>

            <div className="flex items-center justify-end gap-3">
              <button
                type="button"
                onClick={() => setCancellingBooking(null)}
                className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-100 transition cursor-pointer"
              >
                Keep Booking
              </button>

              <button
                type="button"
                disabled={submittingCancel}
                onClick={handleCancelBooking}
                className="px-5 py-2.5 rounded-xl bg-red-600 hover:bg-red-700 text-white text-xs font-semibold shadow-xs transition flex items-center gap-1.5 cursor-pointer disabled:opacity-50"
              >
                {submittingCancel && <RefreshCw className="w-3.5 h-3.5 animate-spin" />}
                Confirm Cancellation
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ==================================================================== */}
      {/* MODAL 5: HELP DESK CREATE/EDIT AMENITY MODAL                         */}
      {/* ==================================================================== */}
      {showCreateAmenityModal && isHelpDesk && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-xl w-full p-6 shadow-2xl border border-slate-200 relative animate-in fade-in zoom-in-95 duration-200 max-h-[90vh] overflow-y-auto">
            <button
              onClick={() => setShowCreateAmenityModal(false)}
              className="absolute top-5 right-5 text-slate-400 hover:text-slate-600 p-1.5 rounded-lg hover:bg-slate-100 transition cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>

            <div className="flex items-center gap-3 mb-5">
              <div className="p-3 rounded-2xl bg-indigo-50 text-indigo-600 border border-indigo-200/50">
                <Building2 className="w-6 h-6" />
              </div>
              <div>
                <h3 className="text-lg font-bold text-slate-900">
                  {editingAmenity ? 'Edit Facility Configuration' : 'Create New Facility'}
                </h3>
                <p className="text-xs text-slate-500">Master amenity parameters and operating policies</p>
              </div>
            </div>

            <form onSubmit={handleSaveAmenity} className="space-y-4">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Facility Name</label>
                  <input
                    type="text"
                    value={amenityFormData.name}
                    onChange={(e) => setAmenityFormData({ ...amenityFormData, name: e.target.value })}
                    placeholder="e.g. Squash Court 1"
                    className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 bg-white"
                    required
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Facility Code</label>
                  <input
                    type="text"
                    value={amenityFormData.code}
                    disabled={!!editingAmenity}
                    onChange={(e) => setAmenityFormData({ ...amenityFormData, code: e.target.value.toUpperCase() })}
                    placeholder="e.g. SQUASH_1"
                    className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 bg-white disabled:bg-slate-100 font-mono"
                    required
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Category</label>
                  <select
                    value={amenityFormData.category}
                    onChange={(e) => setAmenityFormData({ ...amenityFormData, category: e.target.value })}
                    className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 bg-white"
                  >
                    <option value="FITNESS">FITNESS</option>
                    <option value="SPORTS">SPORTS</option>
                    <option value="LEISURE">LEISURE</option>
                    <option value="EVENTS">EVENTS</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Location</label>
                  <input
                    type="text"
                    value={amenityFormData.location}
                    onChange={(e) => setAmenityFormData({ ...amenityFormData, location: e.target.value })}
                    placeholder="e.g. Sports Complex, Level 2"
                    className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 bg-white"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Open Time</label>
                  <input
                    type="time"
                    value={amenityFormData.openTime}
                    onChange={(e) => setAmenityFormData({ ...amenityFormData, openTime: e.target.value })}
                    className="w-full px-2.5 py-1.5 text-xs rounded-xl border border-slate-200 bg-white"
                    required
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Close Time</label>
                  <input
                    type="time"
                    value={amenityFormData.closeTime}
                    onChange={(e) => setAmenityFormData({ ...amenityFormData, closeTime: e.target.value })}
                    className="w-full px-2.5 py-1.5 text-xs rounded-xl border border-slate-200 bg-white"
                    required
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Duration (Min)</label>
                  <input
                    type="number"
                    min={15}
                    step={15}
                    value={amenityFormData.slotDurationMinutes}
                    onChange={(e) =>
                      setAmenityFormData({ ...amenityFormData, slotDurationMinutes: parseInt(e.target.value, 10) })
                    }
                    className="w-full px-2.5 py-1.5 text-xs rounded-xl border border-slate-200 bg-white"
                    required
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Capacity / Slot</label>
                  <input
                    type="number"
                    min={1}
                    value={amenityFormData.slotCapacity}
                    onChange={(e) =>
                      setAmenityFormData({ ...amenityFormData, slotCapacity: parseInt(e.target.value, 10) })
                    }
                    className="w-full px-2.5 py-1.5 text-xs rounded-xl border border-slate-200 bg-white"
                    required
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Max Active per Flat</label>
                  <input
                    type="number"
                    min={1}
                    value={amenityFormData.maxActiveBookingsPerFlat}
                    onChange={(e) =>
                      setAmenityFormData({
                        ...amenityFormData,
                        maxActiveBookingsPerFlat: parseInt(e.target.value, 10),
                      })
                    }
                    className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 bg-white"
                    required
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Cancel Deadline (Hrs)</label>
                  <input
                    type="number"
                    min={0}
                    value={amenityFormData.cancelDeadlineHours}
                    onChange={(e) =>
                      setAmenityFormData({
                        ...amenityFormData,
                        cancelDeadlineHours: parseInt(e.target.value, 10),
                      })
                    }
                    className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 bg-white"
                    required
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">Description</label>
                <textarea
                  rows={2}
                  value={amenityFormData.description}
                  onChange={(e) => setAmenityFormData({ ...amenityFormData, description: e.target.value })}
                  placeholder="Facility specifications, equipment, and highlights..."
                  className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 bg-white"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">Rules and Guidelines</label>
                <textarea
                  rows={3}
                  value={amenityFormData.rulesAndGuidelines}
                  onChange={(e) => setAmenityFormData({ ...amenityFormData, rulesAndGuidelines: e.target.value })}
                  placeholder="Numbered guidelines, required dress code, noise policies..."
                  className="w-full px-3 py-2 text-xs rounded-xl border border-slate-200 bg-white font-mono text-[11px]"
                />
              </div>

              <div className="pt-3 flex items-center justify-end gap-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setShowCreateAmenityModal(false)}
                  className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 hover:bg-slate-100 cursor-pointer"
                >
                  Cancel
                </button>

                <button
                  type="submit"
                  className="px-5 py-2.5 rounded-xl bg-slate-900 hover:bg-slate-800 text-white text-xs font-semibold cursor-pointer shadow-xs"
                >
                  {editingAmenity ? 'Save Changes' : 'Create Facility'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

