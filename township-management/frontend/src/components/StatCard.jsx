import { TrendingUp, TrendingDown, Minus } from 'lucide-react';

export default function StatCard({
  title,
  value,
  subtext,
  icon: Icon,
  trend,
  trendLabel,
  colorScheme = 'teal', // teal, sky, indigo, amber, emerald, rose
}) {
  const colorMap = {
    teal: {
      iconBg: 'bg-teal-50 text-teal-600 border-teal-100',
      badge: 'text-teal-700 bg-teal-50 border-teal-200',
    },
    emerald: {
      iconBg: 'bg-emerald-50 text-emerald-600 border-emerald-100',
      badge: 'text-emerald-700 bg-emerald-50 border-emerald-200',
    },
    sky: {
      iconBg: 'bg-sky-50 text-sky-600 border-sky-100',
      badge: 'text-sky-700 bg-sky-50 border-sky-200',
    },
    indigo: {
      iconBg: 'bg-indigo-50 text-indigo-600 border-indigo-100',
      badge: 'text-indigo-700 bg-indigo-50 border-indigo-200',
    },
    amber: {
      iconBg: 'bg-amber-50 text-amber-600 border-amber-100',
      badge: 'text-amber-700 bg-amber-50 border-amber-200',
    },
    rose: {
      iconBg: 'bg-rose-50 text-rose-600 border-rose-100',
      badge: 'text-rose-700 bg-rose-50 border-rose-200',
    },
  };

  const scheme = colorMap[colorScheme] || colorMap.teal;

  return (
    <div className="bg-white rounded-xl border border-slate-200/90 p-5 shadow-sm hover:shadow-md transition duration-200">
      <div className="flex items-center justify-between mb-3">
        <span className="text-xs font-medium text-slate-500 uppercase tracking-wider">
          {title}
        </span>
        {Icon && (
          <div className={`p-2 rounded-xl border ${scheme.iconBg}`}>
            <Icon className="w-4 h-4" />
          </div>
        )}
      </div>

      <div className="flex items-baseline justify-between gap-2">
        <div className="text-2xl font-bold text-slate-900 tracking-tight">
          {value}
        </div>
        {trend && (
          <div className={`flex items-center gap-1 text-[11px] font-semibold px-2 py-0.5 rounded-full border ${scheme.badge}`}>
            {trend === 'up' && <TrendingUp className="w-3 h-3" />}
            {trend === 'down' && <TrendingDown className="w-3 h-3" />}
            {trend === 'neutral' && <Minus className="w-3 h-3" />}
            <span>{trendLabel}</span>
          </div>
        )}
      </div>

      {subtext && (
        <p className="text-xs text-slate-400 mt-2 truncate">
          {subtext}
        </p>
      )}
    </div>
  );
}

