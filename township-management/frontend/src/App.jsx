import { useState } from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import Sidebar from './components/Sidebar';
import Header from './components/Header';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import DashboardPage from './pages/DashboardPage';
import MaintenancePage from './pages/MaintenancePage';
import PaymentsPage from './pages/PaymentsPage';
import BankReconciliationPage from './pages/BankReconciliationPage';
import ComplaintsPage from './pages/ComplaintsPage';
import VisitorsPage from './pages/VisitorsPage';
import AmenitiesPage from './pages/AmenitiesPage';
import NoticesPage from './pages/NoticesPage';
import HelpDeskPage from './pages/HelpDeskPage';
import CommitteePage from './pages/CommitteePage';
import ArchitecturePage from './pages/ArchitecturePage';
import SettingsPage from './pages/SettingsPage';

function DashboardLayout() {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col font-sans antialiased text-slate-800">
      {/* Sidebar Navigation */}
      <Sidebar isOpen={sidebarOpen} onClose={() => setSidebarOpen(false)} />

      {/* Main Content Area (Shifted by 72 (18rem) on desktop) */}
      <div className="lg:pl-72 flex flex-col flex-1 min-h-screen transition-all duration-300">
        {/* Top Header */}
        <Header onToggleSidebar={() => setSidebarOpen(!sidebarOpen)} />

        {/* Page Content */}
        <main className="flex-1 p-4 sm:p-6 lg:p-8 max-w-7xl mx-auto w-full">
          <Routes>
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/maintenance" element={<MaintenancePage />} />
            <Route path="/payments" element={<PaymentsPage />} />
            <Route path="/bank-reconciliation" element={<BankReconciliationPage />} />
            <Route path="/complaints" element={<ComplaintsPage />} />
            <Route path="/visitors" element={<VisitorsPage />} />
            <Route path="/amenities" element={<AmenitiesPage />} />
            <Route path="/notices" element={<NoticesPage />} />
            <Route path="/helpdesk" element={<HelpDeskPage />} />
            <Route path="/committee" element={<CommitteePage />} />
            <Route path="/architecture" element={<ArchitecturePage />} />
            <Route path="/settings" element={<SettingsPage />} />
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </main>

        {/* Global Footer */}
        <footer className="border-t border-slate-200 bg-white py-4 px-6 text-center text-xs text-slate-400">
          <p>
            Pune Grand Vista Gated Community • Apex Facility Management Services Pvt. Ltd. • 28 Towers & 2,400 Units
          </p>
        </footer>
      </div>
    </div>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        {/* Public Authentication Routes */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />

        {/* Protected Township Routes */}
        <Route
          path="/*"
          element={
            <ProtectedRoute>
              <DashboardLayout />
            </ProtectedRoute>
          }
        />
      </Routes>
    </AuthProvider>
  );
}
