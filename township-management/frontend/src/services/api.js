/**
 * Township Management System - API Client Service
 * Phase 4: Full Authentication & JWT Integration
 */

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export function getAuthToken() {
  return localStorage.getItem('township_jwt');
}

export function setAuthToken(token) {
  if (token) {
    localStorage.setItem('township_jwt', token);
  } else {
    localStorage.removeItem('township_jwt');
  }
}

export function removeAuthToken() {
  localStorage.removeItem('township_jwt');
}

/**
 * Standard fetch wrapper with auth header injection and json error handling
 */
async function request(endpoint, options = {}) {
  const url = `${API_BASE_URL}${endpoint}`;
  const headers = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    ...options.headers,
  };

  const token = getAuthToken();
  if (token && !headers['Authorization']) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(url, {
    ...options,
    headers,
  });

  const contentType = response.headers.get('content-type');
  const isJson = contentType && contentType.includes('application/json');
  const data = isJson ? await response.json() : await response.text();

  if (!response.ok) {
    const errorMessage =
      (typeof data === 'object' && (data.message || data.error)) ||
      (typeof data === 'string' && data) ||
      `HTTP error ${response.status}: ${response.statusText}`;

    const error = new Error(errorMessage);
    error.status = response.status;
    error.data = data;
    throw error;
  }

  return data;
}

/**
 * Fetch wrapper for multipart/form-data requests (e.g. Bank CSV statements)
 */
async function requestMultipart(endpoint, formData) {
  const url = `${API_BASE_URL}${endpoint}`;
  const headers = {
    'Accept': 'application/json',
  };

  const token = getAuthToken();
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const response = await fetch(url, {
    method: 'POST',
    headers,
    body: formData,
  });

  const contentType = response.headers.get('content-type');
  const isJson = contentType && contentType.includes('application/json');
  const data = isJson ? await response.json() : await response.text();

  if (!response.ok) {
    const errorMessage =
      (typeof data === 'object' && (data.message || data.error)) ||
      (typeof data === 'string' && data) ||
      `HTTP error ${response.status}: ${response.statusText}`;

    const error = new Error(errorMessage);
    error.status = response.status;
    error.data = data;
    throw error;
  }

  return data;
}

/**
 * Probe backend health
 */
export async function checkBackendHealth() {
  const startTime = performance.now();
  const targetUrl = `${API_BASE_URL}/api/health`;

  try {
    const response = await fetch(targetUrl, {
      method: 'GET',
      headers: { 'Accept': 'application/json' },
    });

    const elapsedMs = Math.round(performance.now() - startTime);

    if (!response.ok) {
      return {
        status: 'error',
        httpStatus: response.status,
        url: targetUrl,
        latencyMs: elapsedMs,
        error: `Server responded with HTTP ${response.status} ${response.statusText}`,
        timestamp: new Date().toISOString(),
      };
    }

    const data = await response.json();
    const isDegraded = data.status === 'DEGRADED' || data.database?.status !== 'CONNECTED';

    return {
      status: isDegraded ? 'degraded' : 'connected',
      httpStatus: response.status,
      url: targetUrl,
      latencyMs: elapsedMs,
      data,
      timestamp: new Date().toISOString(),
    };
  } catch (err) {
    const elapsedMs = Math.round(performance.now() - startTime);
    return {
      status: 'offline',
      httpStatus: 0,
      url: targetUrl,
      latencyMs: elapsedMs,
      error: err.message || 'Failed to fetch from backend',
      timestamp: new Date().toISOString(),
    };
  }
}

/**
 * Authentication API
 */
export const authApi = {
  login: async (identifier, password) => {
    return request('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ identifier, password }),
    });
  },

  register: async (registerData) => {
    return request('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify(registerData),
    });
  },

  getMe: async () => {
    return request('/api/auth/me', {
      method: 'GET',
    });
  },
};

/**
 * Township Core Entities API
 */
export const townshipApi = {
  getTowers: async () => {
    return request('/api/towers', {
      method: 'GET',
    });
  },

  getFlatsByTower: async (towerId) => {
    return request(`/api/towers/${encodeURIComponent(towerId)}/flats`, {
      method: 'GET',
    });
  },
};

/**
 * Phase 5: Maintenance Billing & Flat Ledger API
 */
export const billingApi = {
  getMyBills: async () => {
    return request('/api/billing/my-bills', {
      method: 'GET',
    });
  },

  getBillById: async (billId) => {
    return request(`/api/billing/bills/${encodeURIComponent(billId)}`, {
      method: 'GET',
    });
  },

  getFlatLedger: async (flatId) => {
    return request(`/api/billing/ledger/${encodeURIComponent(flatId)}`, {
      method: 'GET',
    });
  },

  getAdminOverview: async () => {
    return request('/api/billing/admin/overview', {
      method: 'GET',
    });
  },

  getAdminBills: async ({ billingMonth, status, towerId } = {}) => {
    const params = new URLSearchParams();
    if (billingMonth) params.append('billingMonth', billingMonth);
    if (status && status !== 'ALL') params.append('status', status);
    if (towerId && towerId !== 'ALL') params.append('towerId', towerId);
    const query = params.toString() ? `?${params.toString()}` : '';
    return request(`/api/billing/admin/bills${query}`, {
      method: 'GET',
    });
  },

  generateMonthlyBills: async (generateData) => {
    return request('/api/billing/admin/generate-monthly', {
      method: 'POST',
      body: JSON.stringify(generateData),
    });
  },

  getBillingConfig: async () => {
    return request('/api/billing/admin/config', {
      method: 'GET',
    });
  },

  updateBillingConfig: async (configData) => {
    return request('/api/billing/admin/config', {
      method: 'PUT',
      body: JSON.stringify(configData),
    });
  },
};

/**
 * Phase 6: Test Mode Payment Gateway API
 */
export const paymentApi = {
  testPay: async (paymentData) => {
    return request('/api/payments/test-pay', {
      method: 'POST',
      body: JSON.stringify(paymentData),
    });
  },

  getMyHistory: async () => {
    return request('/api/payments/my-history', {
      method: 'GET',
    });
  },

  getReceipt: async (paymentId) => {
    return request(`/api/payments/receipt/${encodeURIComponent(paymentId)}`, {
      method: 'GET',
    });
  },

  getAllPayments: async ({ status, page, size } = {}) => {
    const params = new URLSearchParams();
    if (status && status !== 'ALL') params.append('status', status);
    if (page !== undefined) params.append('page', page);
    if (size !== undefined) params.append('size', size);
    const query = params.toString() ? `?${params.toString()}` : '';
    return request(`/api/payments/admin/all${query}`, {
      method: 'GET',
    });
  },
};

/**
 * Phase 6: Bank CSV Reconciliation API
 */
export const reconciliationApi = {
  uploadStatement: async (formData) => {
    return requestMultipart('/api/reconciliation/upload', formData);
  },

  getTransactions: async ({ status, batchId } = {}) => {
    const params = new URLSearchParams();
    if (status && status !== 'ALL') params.append('status', status);
    if (batchId) params.append('batchId', batchId);
    const query = params.toString() ? `?${params.toString()}` : '';
    return request(`/api/reconciliation/transactions${query}`, {
      method: 'GET',
    });
  },

  approveOrReject: async (actionData) => {
    return request('/api/reconciliation/approve', {
      method: 'POST',
      body: JSON.stringify(actionData),
    });
  },

  getBatches: async () => {
    return request('/api/reconciliation/batches', {
      method: 'GET',
    });
  },
};

/**
 * Phase 7: AI-Powered Complaint Management API
 */
export const complaintApi = {
  previewClassification: async (previewData) => {
    return request('/api/complaints/preview-classify', {
      method: 'POST',
      body: JSON.stringify(previewData),
    });
  },

  createComplaint: async (data, hasPhoto = false) => {
    if (hasPhoto && data instanceof FormData) {
      return requestMultipart('/api/complaints', data);
    }
    return request('/api/complaints', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  getMyComplaints: async () => {
    return request('/api/complaints/my-complaints', {
      method: 'GET',
    });
  },

  getComplaintById: async (id) => {
    return request(`/api/complaints/${encodeURIComponent(id)}`, {
      method: 'GET',
    });
  },

  getPhotoBlobUrl: async (complaintId) => {
    const token = getAuthToken();
    const headers = token ? { Authorization: `Bearer ${token}` } : {};
    const res = await fetch(`${API_BASE_URL}/api/complaints/${encodeURIComponent(complaintId)}/photo`, { headers });
    if (!res.ok) throw new Error('Failed to fetch photo');
    const blob = await res.blob();
    return URL.createObjectURL(blob);
  },

  rateComplaint: async (id, ratingData) => {
    return request(`/api/complaints/${encodeURIComponent(id)}/rate`, {
      method: 'POST',
      body: JSON.stringify(ratingData),
    });
  },

  // Help Desk Endpoints
  getAllComplaints: async ({ status, category, priority, towerId, overdueOnly } = {}) => {
    const params = new URLSearchParams();
    if (status && status !== 'ALL') params.append('status', status);
    if (category && category !== 'ALL') params.append('category', category);
    if (priority && priority !== 'ALL') params.append('priority', priority);
    if (towerId && towerId !== 'ALL') params.append('towerId', towerId);
    if (overdueOnly) params.append('overdueOnly', 'true');
    const query = params.toString() ? `?${params.toString()}` : '';
    return request(`/api/complaints/admin/all${query}`, {
      method: 'GET',
    });
  },

  getOverview: async () => {
    return request('/api/complaints/admin/overview', {
      method: 'GET',
    });
  },

  assignComplaint: async (id, assignData) => {
    return request(`/api/complaints/${encodeURIComponent(id)}/assign`, {
      method: 'PUT',
      body: JSON.stringify(assignData),
    });
  },

  updateStatus: async (id, statusData) => {
    return request(`/api/complaints/${encodeURIComponent(id)}/status`, {
      method: 'PUT',
      body: JSON.stringify(statusData),
    });
  },

  addInternalNote: async (id, noteData) => {
    return request(`/api/complaints/${encodeURIComponent(id)}/internal-note`, {
      method: 'POST',
      body: JSON.stringify(noteData),
    });
  },

  linkDuplicate: async (id, linkData) => {
    return request(`/api/complaints/${encodeURIComponent(id)}/link-duplicate`, {
      method: 'POST',
      body: JSON.stringify(linkData),
    });
  },
};

/**
 * Phase 8: Visitor Management and Gate Security API
 */
export const visitorApi = {
  preApproveVisitor: async (data) => {
    return request('/api/visitors/pre-approve', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  getMyVisitors: async (status) => {
    const query = status && status !== 'ALL' ? `?status=${encodeURIComponent(status)}` : '';
    return request(`/api/visitors/my-visitors${query}`, {
      method: 'GET',
    });
  },

  getMyPendingApprovals: async () => {
    return request('/api/visitors/my-pending-approvals', {
      method: 'GET',
    });
  },

  processApprovalDecision: async (id, decisionData) => {
    return request(`/api/visitors/${encodeURIComponent(id)}/decision`, {
      method: 'PUT',
      body: JSON.stringify(decisionData),
    });
  },

  revokePass: async (id) => {
    return request(`/api/visitors/${encodeURIComponent(id)}/revoke`, {
      method: 'PUT',
    });
  },

  getVisitorById: async (id) => {
    return request(`/api/visitors/${encodeURIComponent(id)}`, {
      method: 'GET',
    });
  },

  getVisitorPhotoBlobUrl: async (id) => {
    const token = getAuthToken();
    const headers = token ? { Authorization: `Bearer ${token}` } : {};
    const res = await fetch(`${API_BASE_URL}/api/visitors/${encodeURIComponent(id)}/photo`, { headers });
    if (!res.ok) throw new Error('Failed to fetch visitor photo');
    const blob = await res.blob();
    return URL.createObjectURL(blob);
  },

  gateLookup: async (q) => {
    return request(`/api/visitors/gate/lookup?q=${encodeURIComponent(q)}`, {
      method: 'GET',
    });
  },

  gateWalkIn: async (data, hasPhoto = false) => {
    if (hasPhoto && data instanceof FormData) {
      return requestMultipart('/api/visitors/gate/walk-in', data);
    }
    return request('/api/visitors/gate/walk-in', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  gateCheckIn: async (data, hasPhoto = false) => {
    if (hasPhoto && data instanceof FormData) {
      return requestMultipart('/api/visitors/gate/check-in', data);
    }
    return request('/api/visitors/gate/check-in', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  gateCheckOut: async (id, data = {}) => {
    return request(`/api/visitors/gate/check-out/${encodeURIComponent(id)}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },

  syncOfflineEvents: async (events) => {
    return request('/api/visitors/gate/sync', {
      method: 'POST',
      body: JSON.stringify({ events }),
    });
  },

  getActivePassesForCache: async () => {
    return request('/api/visitors/gate/active-passes', {
      method: 'GET',
    });
  },

  getGateOverview: async () => {
    return request('/api/visitors/gate/overview', {
      method: 'GET',
    });
  },

  getAllVisitors: async ({ status, flatId, towerId } = {}) => {
    const params = new URLSearchParams();
    if (status && status !== 'ALL') params.append('status', status);
    if (flatId) params.append('flatId', flatId);
    if (towerId && towerId !== 'ALL') params.append('towerId', towerId);
    const query = params.toString() ? `?${params.toString()}` : '';
    return request(`/api/visitors/admin/all${query}`, {
      method: 'GET',
    });
  },

  cleanupPhotos: async () => {
    return request('/api/visitors/admin/cleanup-photos', {
      method: 'POST',
    });
  },
};

/**
 * Phase 9: Amenity and Clubhouse Booking APIs
 */
export const amenityApi = {
  getActiveAmenities: async () => {
    return request('/api/amenities', { method: 'GET' });
  },

  getAmenityById: async (id) => {
    return request(`/api/amenities/${encodeURIComponent(id)}`, { method: 'GET' });
  },

  getAvailableSlots: async (amenityId, date) => {
    const query = date ? `?date=${encodeURIComponent(date)}` : '';
    return request(`/api/amenities/${encodeURIComponent(amenityId)}/slots${query}`, { method: 'GET' });
  },

  createBooking: async (data) => {
    return request('/api/amenities/bookings', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  getMyBookings: async () => {
    return request('/api/amenities/my-bookings', { method: 'GET' });
  },

  getBookingByReference: async (reference) => {
    return request(`/api/amenities/bookings/${encodeURIComponent(reference)}`, { method: 'GET' });
  },

  cancelBooking: async (id, data = {}) => {
    return request(`/api/amenities/bookings/${encodeURIComponent(id)}/cancel`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },

  getAdminOverview: async () => {
    return request('/api/amenities/admin/overview', { method: 'GET' });
  },

  getAllAmenitiesAdmin: async () => {
    return request('/api/amenities/admin/all', { method: 'GET' });
  },

  createAmenity: async (data) => {
    return request('/api/amenities/admin', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  updateAmenity: async (id, data) => {
    return request(`/api/amenities/admin/${encodeURIComponent(id)}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },

  toggleAmenityStatus: async (id, data) => {
    return request(`/api/amenities/admin/${encodeURIComponent(id)}/toggle-status`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },

  getAllBookingsAdmin: async ({ date, amenityId, status, flatId } = {}) => {
    const params = new URLSearchParams();
    if (date) params.append('date', date);
    if (amenityId && amenityId !== 'ALL') params.append('amenityId', amenityId);
    if (status && status !== 'ALL') params.append('status', status);
    if (flatId) params.append('flatId', flatId);
    const query = params.toString() ? `?${params.toString()}` : '';
    return request(`/api/amenities/admin/bookings${query}`, { method: 'GET' });
  },

  adminCancelBooking: async (id, data = {}) => {
    return request(`/api/amenities/admin/bookings/${encodeURIComponent(id)}/cancel`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },
};

/**
 * Phase 10: Notices and Announcements APIs
 */
export const noticeApi = {
  getResidentNotices: async ({ includeExpired = false, category } = {}) => {
    const params = new URLSearchParams();
    if (includeExpired) params.append('includeExpired', 'true');
    if (category && category !== 'ALL') params.append('category', category);
    const query = params.toString() ? `?${params.toString()}` : '';
    return request(`/api/notices${query}`, { method: 'GET' });
  },

  getNoticeById: async (id) => {
    return request(`/api/notices/${encodeURIComponent(id)}`, { method: 'GET' });
  },

  markAsRead: async (id) => {
    return request(`/api/notices/${encodeURIComponent(id)}/read`, { method: 'POST' });
  },

  markAllAsRead: async () => {
    return request('/api/notices/read-all', { method: 'POST' });
  },

  getKpis: async () => {
    return request('/api/notices/kpis', { method: 'GET' });
  },

  getAllNoticesAdmin: async ({ status, category, priority, towerId } = {}) => {
    const params = new URLSearchParams();
    if (status && status !== 'ALL') params.append('status', status);
    if (category && category !== 'ALL') params.append('category', category);
    if (priority && priority !== 'ALL') params.append('priority', priority);
    if (towerId && towerId !== 'ALL') params.append('towerId', towerId);
    const query = params.toString() ? `?${params.toString()}` : '';
    return request(`/api/notices/admin/all${query}`, { method: 'GET' });
  },

  createNotice: async (data) => {
    return request('/api/notices/admin', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  updateNotice: async (id, data) => {
    return request(`/api/notices/admin/${encodeURIComponent(id)}`, {
      method: 'PUT',
      body: JSON.stringify(data),
    });
  },

  togglePin: async (id) => {
    return request(`/api/notices/admin/${encodeURIComponent(id)}/pin`, {
      method: 'PUT',
    });
  },

  archiveNotice: async (id) => {
    return request(`/api/notices/admin/${encodeURIComponent(id)}/archive`, {
      method: 'PUT',
    });
  },

  deleteNotice: async (id) => {
    return request(`/api/notices/admin/${encodeURIComponent(id)}`, {
      method: 'DELETE',
    });
  },
};

/**
 * Blob fetcher for binary / CSV file downloads
 */
export async function fetchBlob(endpoint) {
  const url = `${API_BASE_URL}${endpoint}`;
  const headers = {};
  const token = getAuthToken();
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  const response = await fetch(url, { headers });
  if (!response.ok) {
    throw new Error(`Failed to download report: HTTP ${response.status}`);
  }
  return await response.blob();
}

/**
 * Browser download trigger for Blob
 */
export function downloadBlob(blob, filename) {
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  window.URL.revokeObjectURL(url);
}

/**
 * Committee Analytics and Reporting API
 */
export const committeeApi = {
  getDashboard: async ({ fromDate, toDate, towerId } = {}) => {
    const params = new URLSearchParams();
    if (fromDate) params.append('fromDate', fromDate);
    if (toDate) params.append('toDate', toDate);
    if (towerId && towerId !== 'ALL') params.append('towerId', towerId);
    const query = params.toString() ? `?${params.toString()}` : '';
    return request(`/api/committee/dashboard${query}`, { method: 'GET' });
  },

  getFinancialKpis: async ({ fromDate, toDate, towerId } = {}) => {
    const params = new URLSearchParams();
    if (fromDate) params.append('fromDate', fromDate);
    if (toDate) params.append('toDate', toDate);
    if (towerId && towerId !== 'ALL') params.append('towerId', towerId);
    const query = params.toString() ? `?${params.toString()}` : '';
    return request(`/api/committee/financial${query}`, { method: 'GET' });
  },

  getCollectionTrends: async (towerId) => {
    const query = towerId && towerId !== 'ALL' ? `?towerId=${encodeURIComponent(towerId)}` : '';
    return request(`/api/committee/trends${query}`, { method: 'GET' });
  },

  getDuesAgeing: async (towerId) => {
    const query = towerId && towerId !== 'ALL' ? `?towerId=${encodeURIComponent(towerId)}` : '';
    return request(`/api/committee/dues-ageing${query}`, { method: 'GET' });
  },

  getTowerDues: async () => {
    return request('/api/committee/tower-dues', { method: 'GET' });
  },

  getDefaulters: async (towerId) => {
    const query = towerId && towerId !== 'ALL' ? `?towerId=${encodeURIComponent(towerId)}` : '';
    return request(`/api/committee/defaulters${query}`, { method: 'GET' });
  },

  getComplaintAnalytics: async ({ fromDate, toDate, towerId } = {}) => {
    const params = new URLSearchParams();
    if (fromDate) params.append('fromDate', fromDate);
    if (toDate) params.append('toDate', toDate);
    if (towerId && towerId !== 'ALL') params.append('towerId', towerId);
    const query = params.toString() ? `?${params.toString()}` : '';
    return request(`/api/committee/complaints${query}`, { method: 'GET' });
  },

  getOperationalAnalytics: async ({ fromDate, toDate, towerId } = {}) => {
    const params = new URLSearchParams();
    if (fromDate) params.append('fromDate', fromDate);
    if (toDate) params.append('toDate', toDate);
    if (towerId && towerId !== 'ALL') params.append('towerId', towerId);
    const query = params.toString() ? `?${params.toString()}` : '';
    return request(`/api/committee/operational${query}`, { method: 'GET' });
  },

  downloadFinancialCsv: async ({ fromDate, toDate, towerId } = {}) => {
    const params = new URLSearchParams();
    if (fromDate) params.append('fromDate', fromDate);
    if (toDate) params.append('toDate', toDate);
    if (towerId && towerId !== 'ALL') params.append('towerId', towerId);
    const query = params.toString() ? `?${params.toString()}` : '';
    return fetchBlob(`/api/committee/reports/financial/csv${query}`);
  },

  downloadDefaultersCsv: async (towerId) => {
    const query = towerId && towerId !== 'ALL' ? `?towerId=${encodeURIComponent(towerId)}` : '';
    return fetchBlob(`/api/committee/reports/defaulters/csv${query}`);
  },

  downloadComplaintsCsv: async ({ fromDate, toDate, towerId } = {}) => {
    const params = new URLSearchParams();
    if (fromDate) params.append('fromDate', fromDate);
    if (toDate) params.append('toDate', toDate);
    if (towerId && towerId !== 'ALL') params.append('towerId', towerId);
    const query = params.toString() ? `?${params.toString()}` : '';
    return fetchBlob(`/api/committee/reports/complaints/csv${query}`);
  },

  downloadOperationalCsv: async ({ fromDate, toDate, towerId } = {}) => {
    const params = new URLSearchParams();
    if (fromDate) params.append('fromDate', fromDate);
    if (toDate) params.append('toDate', toDate);
    if (towerId && towerId !== 'ALL') params.append('towerId', towerId);
    const query = params.toString() ? `?${params.toString()}` : '';
    return fetchBlob(`/api/committee/reports/operational/csv${query}`);
  },
};
