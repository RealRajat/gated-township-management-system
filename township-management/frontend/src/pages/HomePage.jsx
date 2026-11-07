import BackendStatusCard from '../components/BackendStatusCard';
import TownshipOverviewCard from '../components/TownshipOverviewCard';

export default function HomePage() {
  return (
    <div className="space-y-8">
      {/* Hero Welcome banner */}
      <div className="bg-gradient-to-r from-indigo-950/70 via-slate-900 to-slate-900 border border-indigo-900/40 rounded-2xl p-6 sm:p-8">
        <div className="max-w-3xl">
          <span className="text-xs font-semibold uppercase tracking-wider text-indigo-400 bg-indigo-950 px-2.5 py-1 rounded-md border border-indigo-800">
            Phase 1 • Initialization & Environment Verification
          </span>
          <h1 className="text-3xl sm:text-4xl font-extrabold text-white mt-3 tracking-tight">
            Township Management System
          </h1>
          <p className="text-sm sm:text-base text-slate-300 mt-2 leading-relaxed">
            Enterprise resident facility management platform designed for 28 residential towers and 2,400 apartments in Pune, India.
            Phase 1 establishes the decoupled React + Vite frontend and Spring Boot 4 + PostgreSQL backend skeleton with active health probe monitoring.
          </p>
        </div>
      </div>

      {/* Backend Live Status Card */}
      <BackendStatusCard />

      {/* Township Project Context & Architecture Modules */}
      <TownshipOverviewCard />
    </div>
  );
}

