# Deployment and Operations Guide

## Gated Township Resident Management System (Pune Grand Vista)

This guide provides step-by-step instructions for installing, configuring, running, and deploying the Gated Township Resident Management System across local development, on-premise servers, and containerized cloud environments.

> [!NOTE]
> **Live Deployment Status**: No public cloud live deployment URL exists yet. The application has been fully validated in local environments and containerized multi-container setups via Docker Compose (Backend: `http://localhost:8080`, Frontend: `http://localhost:5173`).

---

## 1. System Prerequisites

| Component | Minimum Version | Recommended | Purpose |
| :--- | :--- | :--- | :--- |
| **Operating System** | Windows 10/11, Ubuntu 22.04 LTS, or macOS 13+ | Linux x86_64 | Host environment |
| **Java Runtime** | Java 17 LTS (Eclipse Temurin / OpenJDK) | Java 17.0.16+ | Spring Boot backend execution |
| **Backend Framework** | Spring Boot 4.1.2 (Java 17 LTS) | Spring Boot 4.1.2 | Enterprise application backend & REST APIs |
| **Node.js** | Node.js 18.x LTS | Node.js 20.x LTS | React 19 / Vite 8 build & dependencies |
| **Package Manager** | npm 9.x | npm 10.x | Frontend package management |
| **Database** | PostgreSQL 15 | PostgreSQL 16 LTS | Relational storage & ACID double-entry ledger |
| **Containerization** | Docker Engine 24+ & Docker Compose 2.20+ | Latest stable | Multi-container deployment |

---

## 2. Environment Variables Specification

> [!IMPORTANT]
> **Security Notice**: Only environment variable **names** are specified here. Never commit, log, or share real credentials or production secrets. Copy `.env.example` to `.env` to configure your values.

### Backend Required Variables
- `DB_HOST`: Hostname or IP address of the PostgreSQL database server (e.g. `localhost` or Docker service name `postgres`).
- `DB_PORT`: Port number of PostgreSQL (default: `5432`).
- `DB_NAME`: Database catalog name (e.g. `township_db`).
- `DB_USER`: Database role/username with schema creation and DDL/DML permissions.
- `DB_PASSWORD`: Password for the designated database role.
- `PORT`: HTTP server listen port for the Spring Boot backend (default: `8080`).
- `JWT_SECRET`: Cryptographically secure HMAC-SHA512 key encoded in Base64 (minimum 512 bits / 64 bytes). JJWT signs with HS512 for keys >= 64 bytes and HS256 for 32-byte keys.
- `JWT_EXPIRATION_MS`: Access token time-to-live in milliseconds (e.g. `86400000` for 24 hours).
- `CORS_ALLOWED_ORIGINS`: Comma-separated list of permitted origin URLs (e.g. `http://localhost:5173,https://township.yourdomain.com`).
- `GEMINI_API_KEY`: *(Optional)* Google Gemini API key for semantic complaint classification. When omitted, the certified semantic rule engine runs automatically as an autonomous zero-dependency fallback.

### Frontend Required Variables
- `VITE_API_BASE_URL`: Base URL where the backend REST API is reachable (default: `http://localhost:8080`).

---

## 3. Database Setup & Migrations

### A. Initialize PostgreSQL Database
Log in to your PostgreSQL instance using `psql` or administrative tools:

```sql
CREATE DATABASE township_db;
CREATE USER township_admin WITH ENCRYPTED PASSWORD 'YOUR_STRONG_PASSWORD';
GRANT ALL PRIVILEGES ON DATABASE township_db TO township_admin;
\c township_db
GRANT ALL ON SCHEMA public TO township_admin;
```

### B. Database Migration Scripts
Database migrations are versioned under `backend/src/main/resources/db/migration/`:

1. `V1__init_core_entities.sql`: Baseline schema (`towers`, `flats`, `users`, `flat_members`).
2. `V2__maintenance_billing_and_ledger.sql`: Billing configurations, monthly maintenance bills, and immutable flat ledgers.
3. `V3__payments_and_bank_reconciliation.sql`: Payment transactions, bank batches, and reconciliation rules.
4. `V4__complaint_management.sql`: Help desk tickets, AI classification attributes, and complaint activity logs.
5. `V5__visitor_management_and_gate_security.sql`: Gate passes, QR tokens, visitor activities, and guard role constraint.
6. `V6__amenity_and_clubhouse_booking.sql`: Clubhouse amenities, operating slots, and booking capacities.
7. `V7__notices_and_announcements.sql`: Circular notices, tower target filters, and resident read receipts.
8. `V8__committee_analytics.sql`: Managing Committee role extension (`COMMITTEE` role constraint).

When running with Spring Boot in development, migrations are applied in sequence automatically. For manual execution:

```bash
# Apply migrations sequentially
psql -h localhost -U township_admin -d township_db -f backend/src/main/resources/db/migration/V1__init_core_entities.sql
psql -h localhost -U township_admin -d township_db -f backend/src/main/resources/db/migration/V2__maintenance_billing_and_ledger.sql
psql -h localhost -U township_admin -d township_db -f backend/src/main/resources/db/migration/V3__payments_and_bank_reconciliation.sql
psql -h localhost -U township_admin -d township_db -f backend/src/main/resources/db/migration/V4__complaint_management.sql
psql -h localhost -U township_admin -d township_db -f backend/src/main/resources/db/migration/V5__visitor_management_and_gate_security.sql
psql -h localhost -U township_admin -d township_db -f backend/src/main/resources/db/migration/V6__amenity_and_clubhouse_booking.sql
psql -h localhost -U township_admin -d township_db -f backend/src/main/resources/db/migration/V7__notices_and_announcements.sql
psql -h localhost -U township_admin -d township_db -f backend/src/main/resources/db/migration/V8__committee_analytics.sql
```

---

## 4. Local Step-by-Step Setup

### Step 1: Clone or Extract Repository
```bash
cd township-management
```

### Step 2: Configure Environment
Copy the environment template and provide your database credentials:
```bash
cp .env.example .env
```

### Step 3: Start the Backend Service
Using the included Maven Wrapper:

**Linux / macOS:**
```bash
cd backend
./mvnw clean package -DskipTests
java -jar target/township-backend-0.0.1-SNAPSHOT.jar
```

**Windows (PowerShell):**
```powershell
cd backend
.\mvnw.cmd clean package -DskipTests
java -jar target\township-backend-0.0.1-SNAPSHOT.jar
```

Verify backend health at `http://localhost:8080/api/health`.

### Step 4: Start the Frontend Application
In a separate terminal:

```bash
cd frontend
npm install
npm run dev
```

Open your browser at `http://localhost:5173`.

---

## 5. Production Container Deployment (Docker Compose)

For cloud virtual machines (AWS EC2, Google Compute Engine, DigitalOcean, or Azure VMs):

### Step 1: Prepare Environment File
Create a production `.env` file in the `township-management/` directory:
```bash
DB_NAME=township_prod
DB_USER=township_dbuser
DB_PASSWORD=<GENERATE_SECURE_PASSWORD>
DB_PORT=5432
JWT_SECRET=<GENERATE_CRYPTOGRAPHIC_BASE64_KEY>
JWT_EXPIRATION_MS=86400000
CORS_ALLOWED_ORIGINS=https://township.yourdomain.com
PORT=8080
```

### Step 2: Build and Launch Containers
```bash
docker-compose up -d --build
```

### Step 3: Monitor Startup
```bash
docker-compose logs -f backend
```

Once the database healthcheck passes, Spring Boot starts and binds to port 8080. Nginx hosts the optimized frontend SPA on port 80 and proxies `/api/` traffic directly to the backend container.

---

## 6. Pre-Configured Development Demo Accounts

The database seeder automatically provisions test accounts for role validation.

> [!WARNING]
> **PRODUCTION SECURITY WARNING — DEVELOPMENT-ONLY SEED CREDENTIALS**:
> All demo accounts, emails, and default passwords listed below are strictly development seeds intended for local testing, evaluation, and role verification. They must **NEVER** be exposed or used in any staging, production, or publicly accessible deployment.
> 
> Before deploying to production or exposing the system to external network traffic:
> 1. Disable the database seeder (`app.seeder.enabled=false` or remove development seed profiles).
> 2. Change all administrative and staff passwords to strong, cryptographically generated random strings.
> 3. Deactivate or delete development test accounts from PostgreSQL (`DELETE FROM users WHERE email LIKE '%@grandvistapune.in';`).

| Persona / Role | Full Name | Login Identifier / Email | Assigned Unit / Location | Default Password | Notes |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Resident (1)** | Neeraj Kulkarni | `neeraj.kulkarni@grandvistapune.in` | Unit T07-1203 (1,150 sq ft) | `Resident@123` | Primary owner in Tower T07 |
| **Resident (2)** | Swati Joshi | `swati.joshi@grandvistapune.in` | Unit T12-0408 (980 sq ft) | `Resident@123` | Primary owner in Tower T12 |
| **Resident (3)** | Aamir Shaikh | `aamir.shaikh@grandvistapune.in` | Unit T21-1601 (1,640 sq ft) | `Resident@123` | Primary owner in Tower T21 |
| **Help Desk** | Col. Rajiv Sharma (Retd.) | `helpdesk@grandvistapune.in` | Facility HQ Office | `HelpDesk@123` | Operations & maintenance staff |
| **Gate Guard** | Santosh Shinde | `guard@grandvistapune.in` | Gate 1 Main Cabin | `Guard@123` | Gate security terminal account |
| **Committee** | Dr. Vikram Sethi | `committee@grandvistapune.in` | MC Boardroom | `Committee@123` | Managing Committee analytics account |

> [!NOTE]
> Residents authenticate using their login email and password. Resident phone numbers have been omitted from all documentation to protect personal privacy and uphold society data governance rules.

---

## 7. Operational Health Checks & Troubleshooting

### Health Check Endpoint
```bash
curl -s http://localhost:8080/api/health
```

Expected JSON response:
```json
{
  "status": "UP",
  "service": "Gated Township Resident Management System",
  "version": "1.0.0-SNAPSHOT",
  "environment": "dev",
  "database": {
    "status": "CONNECTED",
    "productName": "PostgreSQL",
    "productVersion": "16.15"
  }
}
```

### Common Issues and Resolutions

1. **`FATAL: password authentication failed for user "postgres"`**:
   - Ensure the `DB_PASSWORD` in your `.env` or system environment matches your PostgreSQL instance password.
   - For local Windows installations where the password is empty or custom, verify by testing `psql -U postgres -h localhost`.

2. **`Port 8080 already in use`**:
   - Identify the conflicting process:
     - Linux/macOS: `lsof -i :8080`
     - Windows: `netstat -ano | findstr :8080`
   - Either terminate the existing process or override the port via `PORT=8081` in your environment.

3. **`CORS Policy Violation in Browser Console`**:
   - Ensure `CORS_ALLOWED_ORIGINS` includes your frontend URL (e.g. `http://localhost:5173` or your production domain).

4. **`HTTP 403 Forbidden on Admin or Committee Routes`**:
   - Verify the logged-in user role via `GET /api/auth/me`. Resident and Guard roles do not have access to Help Desk or Committee endpoints. Authenticate with `helpdesk@grandvistapune.in` or `committee@grandvistapune.in`.

5. **`Offline Gate Sync Queue`**:
   - If a gate tablet lost network connectivity during visitor check-ins, the actions are queued in browser `localStorage`. When the connection is restored, navigate to the Guard Portal and click **Sync Offline Actions** to flush the queue into the PostgreSQL ledger idempotently.
