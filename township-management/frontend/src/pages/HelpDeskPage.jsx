import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Headphones,
  Users,
  Clock,
  PhoneCall,
  ShieldCheck,
  CheckCircle2,
  AlertTriangle,
  UserCheck,
  Wrench,
  Radio,
  Search,
} from 'lucide-react';
import DemoNoticeBanner from '../components/DemoNoticeBanner';
import StatCard from '../components/StatCard';
import { facilityStaff } from '../data/mockData';

export default function HelpDeskPage() {
  const navigate = useNavigate();
  const [searchTerm, setSearchTerm] = useState('');

  const filteredStaff = facilityStaff.filter(
    (s) =>
      s.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      s.role.toLowerCase().includes(searchTerm.toLowerCase()) ||
      s.department.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="space-y-6">
      <DemoNoticeBanner moduleName="Facility Staff & Help Desk Operations" />

      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">
            Facility Help Desk & Operations
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 mt-0.5">
            Apex Facility Management Pune control room, on-duty technicians, and SLA dispatch roster.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={() => navigate('/notices')}
            className="inline-flex items-center gap-1.5 px-3.5 py-2 bg-teal-600 hover:bg-teal-700 text-white rounded-lg text-xs font-semibold shadow-sm transition"
          >
            <Radio className="w-4 h-4" />
            Control Room Broadcast (Notices)
          </button>
        </div>
      </div>

      {/* SLA & Response Metrics */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="Technicians On Duty"
          value="18 Active"
          subtext="Plumbers, Electricians, Lift AMC"
          icon={Users}
          colorScheme="teal"
          trend="up"
          trendLabel="Full Shift"
        />
        <StatCard
          title="SLA Compliance Rate"
          value="96.8%"
          subtext="Target >= 95% response"
          icon={ShieldCheck}
          colorScheme="emerald"
          trend="up"
          trendLabel="Benchmark Exceeded"
        />
        <StatCard
          title="Avg Emergency Response"
          value="24 Mins"
          subtext="Lifts & Main Plumbing lines"
          icon={Clock}
          colorScheme="indigo"
          trend="down"
          trendLabel="-6m this week"
        />
        <StatCard
          title="Active Work Orders"
          value="6 In Progress"
          subtext="Assigned across 28 towers"
          icon={Wrench}
          colorScheme="amber"
          trend="neutral"
          trendLabel="Normal Load"
        />
      </div>

      {/* Staff Roster & Duty Desk Table */}
      <div className="bg-white rounded-xl border border-slate-200/90 shadow-sm overflow-hidden">
        <div className="p-4 sm:p-5 border-b border-slate-100 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <span className="text-sm font-bold text-slate-900 tracking-tight">On-Duty Technicians & Supervisors</span>
            <span className="text-xs text-slate-400 ml-2">({filteredStaff.length} personnel listed)</span>
          </div>

          <div className="relative">
            <Search className="w-3.5 h-3.5 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Search technician, role or dept..."
              className="w-full sm:w-60 pl-8 pr-3 py-1.5 bg-slate-50 border border-slate-200 text-xs rounded-lg focus:outline-none focus:border-teal-500 focus:bg-white transition"
            />
          </div>
        </div>

        <div className="divide-y divide-slate-100">
          {filteredStaff.map((staff) => (
            <div key={staff.id} className="p-4 sm:p-5 hover:bg-slate-50/70 transition flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div className="flex items-start gap-3.5">
                <div className="w-10 h-10 rounded-full bg-slate-900 text-teal-300 font-bold text-xs flex items-center justify-center shrink-0">
                  {staff.name.split(' ').map((n) => n[0]).join('')}
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-semibold text-sm text-slate-900">{staff.name}</span>
                    <span className="font-mono text-[10px] text-slate-400 bg-slate-100 px-1.5 py-0.5 rounded">
                      {staff.id}
                    </span>
                  </div>
                  <div className="text-xs text-slate-600 font-medium">
                    {staff.role} • <span className="text-slate-400">{staff.department}</span>
                  </div>
                  <div className="text-xs text-slate-500 mt-1 flex items-center gap-2">
                    <span className="text-teal-700 font-medium">Task: {staff.currentTask}</span>
                    <span>•</span>
                    <span>Shift: {staff.shift}</span>
                  </div>
                </div>
              </div>

              <div className="flex items-center gap-3 shrink-0 self-end sm:self-center">
                <span
                  className={`px-2.5 py-1 rounded-full text-xs font-semibold ${
                    staff.status === 'On Duty'
                      ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                      : 'bg-amber-50 text-amber-700 border border-amber-200'
                  }`}
                >
                  {staff.status}
                </span>
                <a
                  href={`tel:${staff.phone}`}
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-semibold transition"
                >
                  <PhoneCall className="w-3.5 h-3.5 text-teal-600" />
                  {staff.phone}
                </a>
              </div>
            </div>
          ))}
        </div>

        <div className="p-4 bg-slate-50 border-t border-slate-100 flex flex-col sm:flex-row items-center justify-between text-xs text-slate-500 gap-2">
          <span>Facility Emergency Desk: +91 20 6789 0100 (24x7 Control Room)</span>
          <span>Pune Municipal Fire / Ambulance Dispatch Intercom: 101 / 108</span>
        </div>
      </div>
    </div>
  );
}

