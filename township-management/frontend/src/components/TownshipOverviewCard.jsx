import {
  Building2,
  Receipt,
  FileSpreadsheet,
  Bot,
  Headphones,
  BarChart3,
  ShieldCheck,
  Check,
} from 'lucide-react';

const townshipStats = [
  { label: 'Total Towers', value: '28 Towers' },
  { label: 'Total Apartments', value: '2,400 Units' },
  { label: 'Location', value: 'Pune, Maharashtra' },
  { label: 'Architecture Phase', value: 'Phase 1: Initialized' },
];

const roadmapModules = [
  {
    title: 'Resident Maintenance Billing',
    desc: 'Automated quarterly/monthly billing calculations and test payment gateways.',
    icon: Receipt,
    color: 'text-emerald-400',
    phase: 'Phase 2-3',
  },
  {
    title: 'Bank CSV Reconciliation',
    desc: 'Parse and match NEFT/IMPS/UPI bank statement dumps against resident balances.',
    icon: FileSpreadsheet,
    color: 'text-sky-400',
    phase: 'Phase 3',
  },
  {
    title: 'AI Complaint Classification',
    desc: 'Gemini API integration for automated ticketing priority, category & routing.',
    icon: Bot,
    color: 'text-violet-400',
    phase: 'Phase 4',
  },
  {
    title: 'Help Desk Staff Operations',
    desc: 'Field technician task assignment, ticket SLA tracker, and status transitions.',
    icon: Headphones,
    color: 'text-amber-400',
    phase: 'Phase 4',
  },
  {
    title: 'Committee Analytics Dashboard',
    desc: 'Real-time collection vs pending dues, resolution turnaround time, and occupancy trends.',
    icon: BarChart3,
    color: 'text-pink-400',
    phase: 'Phase 5',
  },
  {
    title: 'Visitor & Amenity Access',
    desc: 'Security gatepass verification, club house and sports court booking slots.',
    icon: ShieldCheck,
    color: 'text-teal-400',
    phase: 'Phase 6 (Optional)',
  },
];

export default function TownshipOverviewCard() {
  return (
    <div className="space-y-6">
      {/* Quick Stats Grid */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        {townshipStats.map((stat, idx) => (
          <div
            key={idx}
            className="bg-slate-900 border border-slate-800 rounded-xl p-4 shadow-sm"
          >
            <div className="text-xs text-slate-400">{stat.label}</div>
            <div className="text-base sm:text-lg font-bold text-slate-100 mt-1">
              {stat.value}
            </div>
          </div>
        ))}
      </div>

      {/* Roadmap & Capabilities Module Matrix */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl">
        <div className="flex items-center justify-between pb-4 border-b border-slate-800 mb-6">
          <div className="flex items-center gap-2">
            <Building2 className="w-5 h-5 text-indigo-400" />
            <h3 className="font-semibold text-white">
              Township Operational Modules (Production Scope)
            </h3>
          </div>
          <span className="text-xs text-slate-400">
            Phase 1 Scaffolding Complete
          </span>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {roadmapModules.map((module, i) => {
            const Icon = module.icon;
            return (
              <div
                key={i}
                className="bg-slate-950/60 border border-slate-800/80 rounded-xl p-4 flex flex-col justify-between hover:border-slate-700 transition"
              >
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <div className={`p-2 rounded-lg bg-slate-900 border border-slate-800 ${module.color}`}>
                      <Icon className="w-4 h-4" />
                    </div>
                    <span className="text-[11px] font-mono px-2 py-0.5 rounded bg-slate-800 text-slate-300">
                      {module.phase}
                    </span>
                  </div>
                  <h4 className="text-sm font-semibold text-slate-200">
                    {module.title}
                  </h4>
                  <p className="text-xs text-slate-400 mt-1.5 leading-relaxed">
                    {module.desc}
                  </p>
                </div>
                <div className="mt-3 pt-2 border-t border-slate-900 text-[11px] text-slate-400 flex items-center gap-1">
                  <Check className="w-3 h-3 text-slate-400" />
                  Planned for subsequent phase
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}

