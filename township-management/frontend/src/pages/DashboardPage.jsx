import { useState } from 'react';
import {
  Building2,
  Receipt,
  CheckCircle2,
  AlertCircle,
  Clock,
  ArrowUpRight,
  TrendingUp,
  FileSpreadsheet,
  Headphones,
  Users,
  CreditCard,
  ChevronRight,
  Download,
  Filter,
} from 'lucide-react';
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  Legend,
  CartesianGrid,
  AreaChart,
  Area,
} from 'recharts';
import StatCard from '../components/StatCard';
import DemoNoticeBanner from '../components/DemoNoticeBanner';
import { useAuth } from '../context/AuthContext';
import {
  townshipSummary,
  collectionTrend,
  recentPayments,
  recentComplaints,
} from '../data/mockData';
import { Link } from 'react-router-dom';

export default function DashboardPage() {
  const [chartType, setChartType] = useState('bar');
  const { user, isHelpDesk } = useAuth();
  const primaryFlat = user?.flats && user.flats.length > 0 ? user.flats[0] : null;

  return (
    <div className="space-y-6">
      {/* Demo Notice Disclaimer */}
      <DemoNoticeBanner moduleName="Main Overview" />

      {/* User Welcome Banner */}
      {user && (
        <div className="bg-gradient-to-r from-[#0c1322] to-[#16233b] text-white rounded-2xl p-4 sm:p-5 shadow-sm flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div>
            <div className="flex items-center gap-2">
              <span className="text-xs font-semibold uppercase tracking-wider text-teal-400">
                {isHelpDesk ? 'Facility Management Command' : 'Resident Portal'}
              </span>
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-400"></span>
              <span className="text-[11px] text-emerald-400 font-medium">Session Active (JWT)</span>
            </div>
            <h2 className="text-lg sm:text-xl font-bold tracking-tight text-white mt-1">
              Welcome back, {user.fullName}
            </h2>
            <p className="text-xs text-slate-300 mt-0.5">
              {isHelpDesk ? (
                'Overseeing 28 towers, facility tickets, vendor maintenance, and bank reconciliation.'
              ) : primaryFlat ? (
                `Assigned to ${primaryFlat.towerName} • Unit #${primaryFlat.flatNumber} (${primaryFlat.areaSqFt} sq ft • ${primaryFlat.memberType})`
              ) : (
                'Resident Member • Pune Grand Vista'
              )}
            </p>
          </div>
          <div className="flex items-center gap-2 self-start sm:self-auto">
            <span className="text-xs font-mono font-semibold px-2.5 py-1 rounded-full bg-slate-800 text-teal-300 border border-slate-700">
              Role: {user.role}
            </span>
          </div>
        </div>
      )}

      {/* Page Title & Quick Actions */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-2">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 tracking-tight">
            Township Operations Dashboard
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 mt-0.5">
            Real-time facility management overview for 28 residential towers & 2,400 apartments in Pune.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Link
            to="/maintenance"
            className="inline-flex items-center gap-1.5 px-3.5 py-2 bg-teal-600 hover:bg-teal-700 text-white rounded-lg text-xs font-semibold shadow-sm transition"
          >
            <Receipt className="w-4 h-4" />
            Generate Invoices
          </Link>
          <Link
            to="/bank-reconciliation"
            className="inline-flex items-center gap-1.5 px-3.5 py-2 bg-white hover:bg-slate-50 text-slate-700 border border-slate-200 rounded-lg text-xs font-semibold shadow-sm transition"
          >
            <FileSpreadsheet className="w-4 h-4 text-teal-600" />
            Reconcile CSV
          </Link>
        </div>
      </div>

      {/* Key Metric Stat Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4">
        <StatCard
          title="Total Apartments"
          value="2,400"
          subtext="2,112 Occupied (88%)"
          icon={Building2}
          colorScheme="teal"
          trend="neutral"
          trendLabel="28 Towers"
        />
        <StatCard
          title="Total Towers"
          value="28"
          subtext="Towers A through AB"
          icon={Building2}
          colorScheme="sky"
          trend="neutral"
          trendLabel="15 Floors Avg"
        />
        <StatCard
          title="Monthly Billed"
          value="₹86.40 L"
          subtext="Current Q1-2026 Cycle"
          icon={Receipt}
          colorScheme="indigo"
          trend="up"
          trendLabel="+1.6%"
        />
        <StatCard
          title="Total Collection"
          value="₹78.20 L"
          subtext="90.5% Collected this month"
          icon={CheckCircle2}
          colorScheme="emerald"
          trend="up"
          trendLabel="90.5%"
        />
        <StatCard
          title="Outstanding Dues"
          value="₹8.20 L"
          subtext="288 Units pending dues"
          icon={AlertCircle}
          colorScheme="amber"
          trend="down"
          trendLabel="9.5%"
        />
        <StatCard
          title="Open Complaints"
          value="14"
          subtext="2 Critical escalations"
          icon={Clock}
          colorScheme="rose"
          trend="down"
          trendLabel="SLA 96.8%"
        />
      </div>

      {/* Main Analytics Chart & Quick Distribution */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Monthly Collection Trend (Recharts) */}
        <div className="lg:col-span-2 bg-white rounded-xl border border-slate-200/90 p-5 shadow-sm">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-4 mb-4 border-b border-slate-100">
            <div>
              <h2 className="text-sm font-bold text-slate-900 tracking-tight">
                Maintenance Billing vs Actual Collection Trend
              </h2>
              <p className="text-xs text-slate-400 mt-0.5">
                Amounts displayed in Lakhs INR (₹ Lakhs) over the last 6 billing cycles
              </p>
            </div>
            <div className="flex items-center gap-1 bg-slate-100 p-0.5 rounded-lg self-start sm:self-auto">
              <button
                onClick={() => setChartType('bar')}
                className={`px-2.5 py-1 text-xs font-medium rounded-md transition ${
                  chartType === 'bar'
                    ? 'bg-white text-slate-900 shadow-sm'
                    : 'text-slate-500 hover:text-slate-900'
                }`}
              >
                Bar Chart
              </button>
              <button
                onClick={() => setChartType('area')}
                className={`px-2.5 py-1 text-xs font-medium rounded-md transition ${
                  chartType === 'area'
                    ? 'bg-white text-slate-900 shadow-sm'
                    : 'text-slate-500 hover:text-slate-900'
                }`}
              >
                Area Trend
              </button>
            </div>
          </div>

          <div className="h-72 w-full">
            <ResponsiveContainer width="100%" height="100%">
              {chartType === 'bar' ? (
                <BarChart data={collectionTrend} margin={{ top: 10, right: 10, left: -10, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                  <XAxis dataKey="month" stroke="#94a3b8" fontSize={11} tickLine={false} />
                  <YAxis stroke="#94a3b8" fontSize={11} tickLine={false} unit=" L" />
                  <Tooltip
                    formatter={(value) => [`₹${value} Lakhs`, '']}
                    contentStyle={{ backgroundColor: '#ffffff', borderColor: '#e2e8f0', borderRadius: '8px', fontSize: '12px' }}
                  />
                  <Legend wrapperStyle={{ fontSize: '12px', paddingTop: '10px' }} />
                  <Bar dataKey="billed" name="Billed (₹ Lakhs)" fill="#94a3b8" radius={[4, 4, 0, 0]} />
                  <Bar dataKey="collected" name="Collected (₹ Lakhs)" fill="#0d9488" radius={[4, 4, 0, 0]} />
                </BarChart>
              ) : (
                <AreaChart data={collectionTrend} margin={{ top: 10, right: 10, left: -10, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                  <XAxis dataKey="month" stroke="#94a3b8" fontSize={11} tickLine={false} />
                  <YAxis stroke="#94a3b8" fontSize={11} tickLine={false} unit=" L" />
                  <Tooltip
                    formatter={(value) => [`₹${value} Lakhs`, '']}
                    contentStyle={{ backgroundColor: '#ffffff', borderColor: '#e2e8f0', borderRadius: '8px', fontSize: '12px' }}
                  />
                  <Legend wrapperStyle={{ fontSize: '12px', paddingTop: '10px' }} />
                  <Area type="monotone" dataKey="billed" name="Billed (₹ Lakhs)" stroke="#64748b" fill="#f1f5f9" strokeWidth={2} />
                  <Area type="monotone" dataKey="collected" name="Collected (₹ Lakhs)" stroke="#0d9488" fill="#ccfbf1" strokeWidth={2} />
                </AreaChart>
              )}
            </ResponsiveContainer>
          </div>
        </div>

        {/* Quick Action Matrix & Facility Highlights */}
        <div className="bg-white rounded-xl border border-slate-200/90 p-5 shadow-sm flex flex-col justify-between">
          <div>
            <h2 className="text-sm font-bold text-slate-900 tracking-tight mb-1">
              Township Quick Actions
            </h2>
            <p className="text-xs text-slate-400 mb-4">
              Operations shortcuts for facility desk & committee
            </p>

            <div className="space-y-2.5">
              <Link
                to="/maintenance"
                className="flex items-center justify-between p-3 rounded-xl border border-slate-100 hover:border-teal-200 hover:bg-teal-50/40 transition group"
              >
                <div className="flex items-center gap-3">
                  <div className="p-2 rounded-lg bg-teal-50 text-teal-600 group-hover:scale-105 transition">
                    <Receipt className="w-4 h-4" />
                  </div>
                  <div>
                    <div className="text-xs font-semibold text-slate-800">28 Towers Maintenance</div>
                    <div className="text-[11px] text-slate-400">Quarterly calculation & rate audit</div>
                  </div>
                </div>
                <ChevronRight className="w-4 h-4 text-slate-300 group-hover:text-teal-600 transition" />
              </Link>

              <Link
                to="/bank-reconciliation"
                className="flex items-center justify-between p-3 rounded-xl border border-slate-100 hover:border-sky-200 hover:bg-sky-50/40 transition group"
              >
                <div className="flex items-center gap-3">
                  <div className="p-2 rounded-lg bg-sky-50 text-sky-600 group-hover:scale-105 transition">
                    <FileSpreadsheet className="w-4 h-4" />
                  </div>
                  <div>
                    <div className="text-xs font-semibold text-slate-800">Bank CSV Reconciliation</div>
                    <div className="text-[11px] text-slate-400">Match NEFT/IMPS statement entries</div>
                  </div>
                </div>
                <ChevronRight className="w-4 h-4 text-slate-300 group-hover:text-sky-600 transition" />
              </Link>

              <Link
                to="/complaints"
                className="flex items-center justify-between p-3 rounded-xl border border-slate-100 hover:border-violet-200 hover:bg-violet-50/40 transition group"
              >
                <div className="flex items-center gap-3">
                  <div className="p-2 rounded-lg bg-violet-50 text-violet-600 group-hover:scale-105 transition">
                    <AlertCircle className="w-4 h-4" />
                  </div>
                  <div>
                    <div className="text-xs font-semibold text-slate-800">AI Complaint Routing</div>
                    <div className="text-[11px] text-slate-400">Gemini priority & triage classifier</div>
                  </div>
                </div>
                <ChevronRight className="w-4 h-4 text-slate-300 group-hover:text-violet-600 transition" />
              </Link>

              <Link
                to="/helpdesk"
                className="flex items-center justify-between p-3 rounded-xl border border-slate-100 hover:border-amber-200 hover:bg-amber-50/40 transition group"
              >
                <div className="flex items-center gap-3">
                  <div className="p-2 rounded-lg bg-amber-50 text-amber-600 group-hover:scale-105 transition">
                    <Headphones className="w-4 h-4" />
                  </div>
                  <div>
                    <div className="text-xs font-semibold text-slate-800">Facility Operations Desk</div>
                    <div className="text-[11px] text-slate-400">Technicians, SLA & duty roster</div>
                  </div>
                </div>
                <ChevronRight className="w-4 h-4 text-slate-300 group-hover:text-amber-600 transition" />
              </Link>
            </div>
          </div>

          <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
            <span>SLA Compliance:</span>
            <span className="font-semibold text-teal-700">96.8% on-time</span>
          </div>
        </div>
      </div>

      {/* Two-Column Tables Section: Recent Payments & Recent Complaints */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Table 1: Recent Payment Activity */}
        <div className="bg-white rounded-xl border border-slate-200/90 shadow-sm overflow-hidden flex flex-col justify-between">
          <div className="p-5 border-b border-slate-100 flex items-center justify-between">
            <div>
              <h2 className="text-sm font-bold text-slate-900 tracking-tight">Recent Payment Activity</h2>
              <p className="text-xs text-slate-400 mt-0.5">Live resident collections via UPI, NEFT & Net Banking</p>
            </div>
            <Link to="/payments" className="text-xs font-semibold text-teal-600 hover:text-teal-700 flex items-center gap-1">
              View All <ArrowUpRight className="w-3.5 h-3.5" />
            </Link>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 text-slate-500 font-semibold uppercase tracking-wider text-[10px] border-b border-slate-100">
                <tr>
                  <th className="px-4 py-3">Resident / Flat</th>
                  <th className="px-3 py-3">Tower</th>
                  <th className="px-3 py-3">Mode</th>
                  <th className="px-3 py-3 text-right">Amount</th>
                  <th className="px-4 py-3 text-center">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {recentPayments.slice(0, 5).map((txn) => (
                  <tr key={txn.id} className="hover:bg-slate-50/70 transition">
                    <td className="px-4 py-3">
                      <div className="font-medium text-slate-900">{txn.residentName}</div>
                      <div className="text-[11px] text-slate-400 font-mono">Unit {txn.unit}</div>
                    </td>
                    <td className="px-3 py-3 text-slate-600 text-[11px] truncate max-w-[120px]">{txn.tower}</td>
                    <td className="px-3 py-3">
                      <span className="px-2 py-0.5 rounded text-[10px] font-medium bg-slate-100 text-slate-700 border border-slate-200">
                        {txn.mode}
                      </span>
                    </td>
                    <td className="px-3 py-3 text-right font-semibold text-slate-900 font-mono">
                      ₹{txn.amount.toLocaleString('en-IN')}
                    </td>
                    <td className="px-4 py-3 text-center">
                      <span
                        className={`inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-semibold ${
                          txn.status === 'Success'
                            ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                            : txn.status === 'Processing'
                            ? 'bg-amber-50 text-amber-700 border border-amber-200'
                            : 'bg-rose-50 text-rose-700 border border-rose-200'
                        }`}
                      >
                        {txn.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="p-3 bg-slate-50 border-t border-slate-100 text-center text-xs text-slate-400">
            Total 24 transactions recorded today across 28 towers
          </div>
        </div>

        {/* Table 2: Recent Complaints & Help Desk Triage */}
        <div className="bg-white rounded-xl border border-slate-200/90 shadow-sm overflow-hidden flex flex-col justify-between">
          <div className="p-5 border-b border-slate-100 flex items-center justify-between">
            <div>
              <h2 className="text-sm font-bold text-slate-900 tracking-tight">Recent Complaints & SLA</h2>
              <p className="text-xs text-slate-400 mt-0.5">Facility management ticketing & AI severity tags</p>
            </div>
            <Link to="/complaints" className="text-xs font-semibold text-teal-600 hover:text-teal-700 flex items-center gap-1">
              View All <ArrowUpRight className="w-3.5 h-3.5" />
            </Link>
          </div>

          <div className="divide-y divide-slate-100">
            {recentComplaints.slice(0, 4).map((ticket) => (
              <div key={ticket.id} className="p-4 hover:bg-slate-50/70 transition flex items-start justify-between gap-3">
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <span className="font-mono text-[11px] font-bold text-slate-700 bg-slate-100 px-1.5 py-0.5 rounded border border-slate-200">
                      {ticket.id}
                    </span>
                    <span className="text-xs font-semibold text-slate-900">
                      Unit {ticket.unit} ({ticket.tower.split(' ')[0]} {ticket.tower.split(' ')[1]})
                    </span>
                    <span
                      className={`text-[10px] font-semibold px-2 py-0.5 rounded-full ${
                        ticket.priority === 'Critical'
                          ? 'bg-rose-50 text-rose-700 border border-rose-200'
                          : ticket.priority === 'High'
                          ? 'bg-amber-50 text-amber-700 border border-amber-200'
                          : 'bg-slate-100 text-slate-700 border border-slate-200'
                      }`}
                    >
                      {ticket.priority}
                    </span>
                  </div>
                  <div className="text-xs text-slate-700 font-medium leading-relaxed">
                    {ticket.title}
                  </div>
                  <div className="flex items-center gap-2 text-[11px] text-slate-400">
                    <span className="text-teal-700 font-medium">Assigned: {ticket.assignedTo}</span>
                    <span>•</span>
                    <span>{ticket.reportedTime}</span>
                  </div>
                </div>

                <div className="shrink-0 text-right">
                  <span
                    className={`inline-block px-2 py-0.5 rounded text-[10px] font-medium ${
                      ticket.status === 'In Progress'
                        ? 'bg-sky-50 text-sky-700 border border-sky-200'
                        : ticket.status === 'Open'
                        ? 'bg-amber-50 text-amber-700 border border-amber-200'
                        : 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                    }`}
                  >
                    {ticket.status}
                  </span>
                </div>
              </div>
            ))}
          </div>

          <div className="p-3 bg-slate-50 border-t border-slate-100 text-center text-xs text-slate-400">
            Average ticket turnaround time: 3.8 hours in Pune zone
          </div>
        </div>
      </div>
    </div>
  );
}

