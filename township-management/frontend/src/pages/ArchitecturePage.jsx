import {
  Layers,
  Database,
  Lock,
  Cpu,
  Globe,
  CheckCircle2,
  Terminal,
  Server,
  ArrowRight,
  ShieldCheck,
  UserCheck,
  Key,
} from 'lucide-react';
import BackendStatusCard from '../components/BackendStatusCard';
import DemoNoticeBanner from '../components/DemoNoticeBanner';
import { useAuth } from '../context/AuthContext';

const stackPillars = [
  {
    title: 'Frontend Client',
    tech: 'React 19 + Vite + Tailwind CSS v4 + React Router v7',
    role: 'Responsive single page enterprise SaaS application with navy sidebar, teal accents, Recharts analytics, and modular components.',
    target: 'Vercel CDN',
    icon: Globe,
    badge: 'Phase 3 & 4 (Active)',
  },
  {
    title: 'Backend API Service',
    tech: 'Spring Boot 4 (Java 17) + Spring Web + Spring Security 6',
    role: 'Stateless REST API microservice with CORS policy, input validation, BCrypt password hashing, and secure endpoints.',
    target: 'Render Web Service',
    icon: Cpu,
    badge: 'Active (Port 8080)',
  },
  {
    title: 'Persistence Layer',
    tech: 'PostgreSQL 16.15 + Spring Data JPA (Hibernate 7)',
    role: 'Relational data store managing resident ledgers, 28 tower hierarchies, flat members, payment audit records, and ticketing history.',
    target: 'PostgreSQL 16 (localhost:5432/township_db)',
    icon: Database,
    badge: 'Connected',
  },
  {
    title: 'Security & Auth',
    tech: 'Spring Security 6 + JJWT (HMAC-SHA256) + BCrypt',
    role: 'Stateless JWT Bearer authentication, role-based access control (RESIDENT & HELP_DESK), and idempotent seeder for 28 towers.',
    target: 'Spring Security FilterChain',
    icon: Lock,
    badge: 'Phase 4 (Active)',
  },
];

const databaseEntities = [
  {
    table: 'towers',
    fields: 'id (PK, VARCHAR 10), name (VARCHAR 100), total_floors, total_flats',
    constraint: '28 Towers seeded (Tower A Orchid through Tower AB Zinnia)',
  },
  {
    table: 'flats',
    fields: 'id (PK, BIGSERIAL), tower_id (FK), flat_number, floor_number, area_sq_ft, occupancy_status',
    constraint: 'UNIQUE (tower_id, flat_number)',
  },
  {
    table: 'users',
    fields: 'id (PK, BIGSERIAL), full_name, email, phone, password_hash, role, is_active, created_at',
    constraint: 'UNIQUE (email), UNIQUE (phone), BCrypt hashed',
  },
  {
    table: 'flat_members',
    fields: 'id (PK, BIGSERIAL), flat_id (FK), user_id (FK), member_type, is_primary, created_at',
    constraint: 'UNIQUE (flat_id, user_id), Foreign Keys with CASCADE/RESTRICT',
  },
];

export default function ArchitecturePage() {
  const { user, token, isAuthenticated, isHelpDesk } = useAuth();
  const primaryFlat = user?.flats && user.flats.length > 0 ? user.flats[0] : null;

  return (
    <div className="space-y-6">
      <DemoNoticeBanner moduleName="System Architecture & Backend Health" />

      {/* Page Title */}
      <div>
        <h1 className="text-2xl font-bold text-slate-900 tracking-tight">
          System Architecture & Technical Specifications
        </h1>
        <p className="text-xs sm:text-sm text-slate-500 mt-0.5">
          Detailed architectural breakdown and live API & PostgreSQL health probe for Pune Gated Township Resident Management System.
        </p>
      </div>

      {/* Live Backend & Database Connection Status Card */}
      <BackendStatusCard />

      {/* Active JWT Session Card */}
      {isAuthenticated && user && (
        <div className="bg-white border border-teal-200 rounded-xl p-5 shadow-sm">
          <div className="flex items-center justify-between pb-3 border-b border-slate-100">
            <div className="flex items-center gap-2">
              <div className="p-1.5 rounded-lg bg-teal-50 text-teal-600">
                <Key className="w-4 h-4" />
              </div>
              <h3 className="font-bold text-slate-900 text-sm">Active JWT Authentication Session</h3>
            </div>
            <span className="text-[10px] font-mono px-2 py-0.5 rounded-full font-semibold bg-emerald-100 text-emerald-800">
              Authenticated • {user.role}
            </span>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mt-4 text-xs">
            <div className="bg-slate-50 p-3 rounded-lg border border-slate-100">
              <span className="text-slate-400 block text-[11px]">Logged-in User</span>
              <span className="font-semibold text-slate-800">{user.fullName}</span>
              <span className="text-[10px] text-slate-500 block truncate">{user.email}</span>
            </div>

            <div className="bg-slate-50 p-3 rounded-lg border border-slate-100">
              <span className="text-slate-400 block text-[11px]">Registered Mobile</span>
              <span className="font-semibold text-slate-800 font-mono">{user.phone}</span>
              <span className="text-[10px] text-emerald-600 block">Verified format</span>
            </div>

            <div className="bg-slate-50 p-3 rounded-lg border border-slate-100">
              <span className="text-slate-400 block text-[11px]">Assigned Unit</span>
              <span className="font-semibold text-slate-800">
                {primaryFlat ? `${primaryFlat.towerName} • #${primaryFlat.flatNumber}` : 'Facility Ops (HQ)'}
              </span>
              <span className="text-[10px] text-slate-500 block">
                {primaryFlat ? `${primaryFlat.areaSqFt} sq ft • ${primaryFlat.memberType}` : 'Management Team'}
              </span>
            </div>

            <div className="bg-slate-50 p-3 rounded-lg border border-slate-100">
              <span className="text-slate-400 block text-[11px]">JWT Bearer Token</span>
              <span className="font-mono text-[10px] text-teal-700 block truncate" title={token}>
                {token ? `${token.substring(0, 24)}...` : 'None'}
              </span>
              <span className="text-[10px] text-slate-400 block">HMAC-SHA256 • 24h Expiry</span>
            </div>
          </div>
        </div>
      )}

      {/* Stack Pillars Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {stackPillars.map((pillar, idx) => {
          const Icon = pillar.icon;
          return (
            <div
              key={idx}
              className="bg-white border border-slate-200/90 rounded-xl p-5 shadow-sm flex flex-col justify-between"
            >
              <div>
                <div className="flex items-center justify-between mb-3">
                  <div className="flex items-center gap-3">
                    <div className="p-2 rounded-xl bg-teal-50 border border-teal-100 text-teal-600">
                      <Icon className="w-5 h-5" />
                    </div>
                    <div>
                      <h3 className="font-bold text-slate-900 text-sm">{pillar.title}</h3>
                      <div className="text-xs text-slate-400 font-mono">{pillar.tech}</div>
                    </div>
                  </div>
                  <span className="text-[10px] font-mono px-2 py-0.5 rounded font-semibold bg-slate-100 text-slate-700 border border-slate-200">
                    {pillar.badge}
                  </span>
                </div>
                <p className="text-xs text-slate-600 leading-relaxed mt-2">
                  {pillar.role}
                </p>
              </div>
              <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between text-xs">
                <span className="text-slate-400">Deployment Target:</span>
                <span className="text-teal-700 font-semibold">{pillar.target}</span>
              </div>
            </div>
          );
        })}
      </div>

      {/* PostgreSQL Database Schema Card */}
      <div className="bg-white border border-slate-200/90 rounded-xl p-5 shadow-sm">
        <h3 className="font-bold text-slate-900 text-sm mb-3 flex items-center gap-2">
          <Database className="w-4 h-4 text-teal-600" />
          PostgreSQL 16 Relational Schema & Unique Constraints
        </h3>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs border-collapse">
            <thead>
              <tr className="border-b border-slate-200 text-slate-500 font-medium">
                <th className="py-2.5 px-3">Table Name</th>
                <th className="py-2.5 px-3">Primary & Foreign Keys / Columns</th>
                <th className="py-2.5 px-3">Constraints & Integrity Rules</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {databaseEntities.map((ent, idx) => (
                <tr key={idx} className="hover:bg-slate-50">
                  <td className="py-2.5 px-3 font-mono font-semibold text-teal-800">{ent.table}</td>
                  <td className="py-2.5 px-3 text-slate-700 font-mono text-[11px]">{ent.fields}</td>
                  <td className="py-2.5 px-3 text-slate-600">{ent.constraint}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Architecture Roadmap Checklist */}
      <div className="bg-white border border-slate-200/90 rounded-xl p-5 shadow-sm">
        <h3 className="font-bold text-slate-900 text-sm mb-3 flex items-center gap-2">
          <Terminal className="w-4 h-4 text-teal-600" />
          Technical Progress & Verification Checklist
        </h3>
        <ul className="space-y-2 text-xs text-slate-600">
          <li className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span>Phase 1: Clean monorepo structure initialized with Vite React frontend & Spring Boot backend.</span>
          </li>
          <li className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span>Phase 2: Local PostgreSQL 16 database connected and verified with active HikariCP connection pool on <code className="text-teal-700 font-mono">localhost:5432/township_db</code>.</span>
          </li>
          <li className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span>Phase 3: Polished SaaS dashboard UI implemented across 9 pages with clean white background, navy sidebar, and subtle teal accents.</span>
          </li>
          <li className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span>Phase 4: Real JWT authentication, BCrypt password hashing, Spring Security FilterChain, and PostgreSQL entities (<code className="text-teal-700 font-mono">users</code>, <code className="text-teal-700 font-mono">towers</code>, <code className="text-teal-700 font-mono">flats</code>, <code className="text-teal-700 font-mono">flat_members</code>).</span>
          </li>
          <li className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span>Phase 4: Idempotent database seeder with all 28 towers and sample residents (Neeraj Kulkarni, Swati Joshi, Aamir Shaikh, Col. Rajiv Sharma).</span>
          </li>
        </ul>
      </div>
    </div>
  );
}
