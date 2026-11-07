import { Info, Database } from 'lucide-react';

export default function DemoNoticeBanner({ moduleName = 'Dashboard' }) {
  return (
    <div className="bg-amber-50/80 border border-amber-200/80 rounded-xl p-3 sm:p-3.5 mb-6 flex flex-col sm:flex-row sm:items-center justify-between gap-2.5 text-xs">
      <div className="flex items-start sm:items-center gap-2.5">
        <div className="p-1 rounded-md bg-amber-100 text-amber-700 shrink-0">
          <Info className="w-4 h-4" />
        </div>
        <div>
          <span className="font-semibold text-amber-900">
            Preview Mode ({moduleName})
          </span>
          <span className="text-amber-800 ml-1.5 leading-relaxed">
            Displaying operational simulation data for 2,400 apartments across 28 towers in Pune. Production database persistence activates in Phase 4.
          </span>
        </div>
      </div>
      <div className="flex items-center gap-1.5 text-[11px] font-mono text-amber-900/80 bg-amber-100/70 px-2.5 py-1 rounded-lg shrink-0 self-start sm:self-auto">
        <Database className="w-3.5 h-3.5 text-amber-700" />
        <span>DB: PostgreSQL 16 (Ready)</span>
      </div>
    </div>
  );
}

