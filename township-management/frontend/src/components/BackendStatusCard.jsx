import { useState, useEffect } from 'react';
import {
  Server,
  Database,
  RefreshCw,
  CheckCircle2,
  AlertTriangle,
  XCircle,
  Cpu,
  Clock,
  Terminal,
} from 'lucide-react';
import { checkBackendHealth } from '../services/api';

export default function BackendStatusCard() {
  const [healthState, setHealthState] = useState({
    loading: true,
    result: null,
  });

  const runCheck = async () => {
    setHealthState((prev) => ({ ...prev, loading: true }));
    const result = await checkBackendHealth();
    setHealthState({
      loading: false,
      result,
    });
  };

  useEffect(() => {
    runCheck();
  }, []);

  const { loading, result } = healthState;

  const getStatusBadge = () => {
    if (loading) {
      return (
        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-amber-50 text-amber-700 border border-amber-200 animate-pulse">
          <RefreshCw className="w-3.5 h-3.5 animate-spin text-amber-600" />
          Probing Backend...
        </span>
      );
    }
    if (!result) return null;

    if (result.status === 'connected') {
      return (
        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200">
          <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
          Connected (All Systems UP)
        </span>
      );
    }

    if (result.status === 'degraded') {
      return (
        <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-amber-50 text-amber-700 border border-amber-200">
          <AlertTriangle className="w-3.5 h-3.5 text-amber-600" />
          Backend Active • Database Degraded
        </span>
      );
    }

    return (
      <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-rose-50 text-rose-700 border border-rose-200">
        <XCircle className="w-3.5 h-3.5 text-rose-600" />
        Backend Offline
      </span>
    );
  };

  return (
    <div className="bg-white border border-slate-200/90 rounded-xl p-5 shadow-sm">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-100">
        <div className="flex items-center gap-3">
          <div className="p-2.5 bg-teal-50 rounded-xl border border-teal-100 text-teal-600">
            <Server className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-sm font-bold text-slate-900 tracking-tight">Live Backend & Database Connection</h2>
            <p className="text-xs text-slate-400">
              Direct probe to Spring Boot health endpoint:{' '}
              <code className="text-teal-700 bg-slate-50 px-1.5 py-0.5 rounded text-[11px] font-mono border border-slate-200">
                {result?.url || 'http://localhost:8080/api/health'}
              </code>
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2.5 self-start sm:self-auto">
          {getStatusBadge()}
          <button
            onClick={runCheck}
            disabled={loading}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium bg-slate-50 hover:bg-slate-100 text-slate-700 border border-slate-200 transition disabled:opacity-50"
            title="Re-run health check"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin text-teal-600' : ''}`} />
            Refresh
          </button>
        </div>
      </div>

      {/* Details Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-4">
        {/* Metric 1: Backend Service */}
        <div className="bg-slate-50/70 border border-slate-200/70 rounded-xl p-4">
          <div className="flex items-center justify-between text-xs text-slate-500 mb-1">
            <span className="flex items-center gap-1.5 font-medium">
              <Server className="w-3.5 h-3.5 text-teal-600" />
              Spring Boot API
            </span>
            <span className="font-mono text-teal-700 font-semibold">{result?.latencyMs ? `${result.latencyMs} ms` : '—'}</span>
          </div>
          <div className="text-xs sm:text-sm font-bold text-slate-900 truncate">
            {result?.data?.service || (result?.status === 'offline' ? 'Unreachable' : 'Probing...')}
          </div>
          <div className="text-xs text-slate-400 mt-1">
            Status:{' '}
            <span className={result?.status === 'offline' ? 'text-rose-600 font-semibold' : 'text-emerald-700 font-semibold'}>
              {result?.data?.status || (result?.status === 'offline' ? 'DOWN' : 'PENDING')}
            </span>{' '}
            • Env: <span className="text-slate-600 font-mono">{result?.data?.environment || 'dev'}</span>
          </div>
        </div>

        {/* Metric 2: PostgreSQL Database */}
        <div className="bg-slate-50/70 border border-slate-200/70 rounded-xl p-4">
          <div className="flex items-center justify-between text-xs text-slate-500 mb-1">
            <span className="flex items-center gap-1.5 font-medium">
              <Database className="w-3.5 h-3.5 text-emerald-600" />
              PostgreSQL Engine
            </span>
            <span className="font-mono text-slate-500 text-[11px]">Port 5432</span>
          </div>
          <div className="text-xs sm:text-sm font-bold text-slate-900">
            {result?.data?.database?.status === 'CONNECTED' ? (
              <span className="text-emerald-700 flex items-center gap-1">
                <CheckCircle2 className="w-4 h-4 text-emerald-600" /> Connected
              </span>
            ) : result?.status === 'offline' ? (
              <span className="text-slate-400">Offline (Backend down)</span>
            ) : (
              <span className="text-amber-700 flex items-center gap-1">
                <AlertTriangle className="w-4 h-4 text-amber-600" /> Disconnected
              </span>
            )}
          </div>
          <div className="text-xs text-slate-400 mt-1 truncate">
            {result?.data?.database?.productName
              ? `${result.data.database.productName} ${result.data.database.productVersion || '16'}`
              : result?.data?.database?.suggestion || 'localhost:5432/township_db'}
          </div>
        </div>

        {/* Metric 3: System Runtime */}
        <div className="bg-slate-50/70 border border-slate-200/70 rounded-xl p-4">
          <div className="flex items-center justify-between text-xs text-slate-500 mb-1">
            <span className="flex items-center gap-1.5 font-medium">
              <Cpu className="w-3.5 h-3.5 text-sky-600" />
              Host Environment
            </span>
            <span className="flex items-center gap-1 text-[11px] text-slate-400">
              <Clock className="w-3 h-3" />
              {result?.timestamp ? new Date(result.timestamp).toLocaleTimeString() : '—'}
            </span>
          </div>
          <div className="text-xs sm:text-sm font-bold text-slate-900 font-mono">
            {result?.data?.system?.javaVersion ? `Java ${result.data.system.javaVersion}` : 'Java 17 (Adoptium)'}
          </div>
          <div className="text-xs text-slate-400 mt-1">
            OS: {result?.data?.system?.osName || 'Windows 11 x64'} • Heap: {result?.data?.system?.jvmTotalMemoryMB ? `${result.data.system.jvmTotalMemoryMB}MB` : '100MB'}
          </div>
        </div>
      </div>

      {/* Troubleshooting panel if offline */}
      {result?.status === 'offline' && (
        <div className="mt-4 p-3.5 rounded-xl bg-rose-50 border border-rose-200 text-xs text-rose-800">
          <div className="font-semibold flex items-center gap-1.5 text-rose-900 mb-1">
            <Terminal className="w-4 h-4" />
            Backend is offline. To launch Spring Boot service:
          </div>
          <pre className="mt-1.5 p-2 bg-slate-900 text-teal-300 rounded-lg font-mono text-[11px] overflow-x-auto">
            cd township-management/backend; .\mvnw.cmd spring-boot:run
          </pre>
        </div>
      )}

      {/* Raw Payload Inspection Accordion */}
      {result?.data && (
        <div className="mt-4 pt-3 border-t border-slate-100">
          <details className="text-xs text-slate-500 group">
            <summary className="cursor-pointer hover:text-slate-900 select-none flex items-center gap-1 font-mono text-[11px]">
              ▶ Inspect Raw /api/health Response JSON
            </summary>
            <pre className="mt-2 p-3 bg-slate-900 text-teal-300 rounded-lg font-mono text-[11px] overflow-x-auto">
              {JSON.stringify(result.data, null, 2)}
            </pre>
          </details>
        </div>
      )}
    </div>
  );
}
