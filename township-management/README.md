# Township Management System (Pune, India)

A production-style Gated Township Resident Management System built for facility management operations in Pune, India, serving **2,400 apartments across 28 residential towers**.

---

## 🏗️ Architecture & Tech Stack

| Layer | Technology | Version | Purpose |
| :--- | :--- | :--- | :--- |
| **Frontend** | React + Vite + Tailwind CSS | React 19, Vite 8, Tailwind v4 | Responsive Web Client & Committee Portal |
| **Router** | React Router | v7 | Client-side routing & deep navigation |
| **Backend** | Java + Spring Boot | Java 17 (Adoptium), Spring Boot 4 | Stateless REST API Microservice |
| **Security** | Spring Security + JJWT + BCrypt | Spring Security 6, JJWT 0.12.6 | Stateless JWT auth filter, BCrypt password hashing |
| **Data Access** | Spring Data JPA (Hibernate) | Hibernate 7 | ORM & Relational Entities (Users, Towers, Flats, Members) |
| **Database** | PostgreSQL | 16 / 17 | Multi-tenant ledger & operational database |
| **Build Tools** | Maven Wrapper & npm | Maven 3.10 (mvnw), npm 10.9 | Standardized build & test automation |

---

## 📁 Repository Structure

```
township-management/
├── frontend/                     # React 19 + Vite + Tailwind CSS + React Router
│   ├── src/
│   │   ├── components/           # Navbar, BackendStatusCard, TownshipOverviewCard
│   │   ├── pages/                # HomePage, ArchitecturePage
│   │   ├── services/             # api.js (Backend health check & latency timing)
│   │   ├── App.jsx               # Main layout and route definitions
│   │   ├── index.css             # Tailwind CSS imports & theme layer
│   │   └── main.jsx              # React DOM bootstrap with BrowserRouter
│   ├── .env                      # Local development environment configuration
│   ├── .env.example              # Environment variables template
│   ├── package.json              # Frontend npm dependencies and build scripts
│   └── vite.config.js            # Vite configuration with Tailwind & /api proxy
├── backend/                      # Java 17 + Spring Boot REST microservice
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/township/
│   │   │   │   ├── config/       # CorsConfig.java, SecurityConfig.java
│   │   │   │   ├── controller/   # HealthController.java (/api/health)
│   │   │   │   ├── dto/          # HealthResponseDto.java
│   │   │   │   └── TownshipBackendApplication.java
│   │   │   └── resources/
│   │   │       ├── application.yml        # Production-grade configuration template
│   │   │       └── application-dev.yml    # Development profile overrides
│   │   └── test/
│   │       └── java/com/township/
│   │           ├── controller/HealthControllerTest.java # Health endpoint test
│   │           └── TownshipBackendApplicationTests.java # Context loading test
│   ├── .env.example              # Backend environment template (DB, JWT, CORS)
│   ├── pom.xml                   # Maven dependencies and build plugins
│   ├── mvnw & mvnw.cmd           # Official Maven Wrapper (cross-platform)
│   └── .mvn/                     # Maven Wrapper binary and properties
├── docs/                         # Engineering and architecture documentation
│   ├── architecture.md           # High-level architecture and module roadmap
│   ├── api-spec.md               # API endpoints, response payloads, and CORS specs
│   └── database-schema.md        # PostgreSQL relational schema blueprint
├── sample-data/                  # Pune Township mock configuration
│   ├── township-config.json      # Township metadata, billing rates, facility contacts
│   └── towers-sample.json        # 28 Towers (A through AB) and unit distributions
├── README.md                     # This documentation file
└── .gitignore                    # Monorepo git ignore rules
```

---

## ⚙️ Environment Prerequisites Audit

During Phase 1 initialization, your system was inspected:

| Tool | Status | Details | Action Required |
| :--- | :--- | :--- | :--- |
| **Node.js** | ✅ Installed | `v22.15.0` | None |
| **npm** | ✅ Installed | `10.9.2` | None |
| **Java JDK** | ✅ Installed | `Eclipse Adoptium OpenJDK 17.0.16` | User `JAVA_HOME` configured |
| **Maven** | ✅ Ready via Wrapper | `mvnw.cmd` included in `backend/` | No global install required |
| **PostgreSQL**| ✅ Connected | PostgreSQL 16.15 on `localhost:5432/township_db` | Configured via environment variables |

---

## 📥 PostgreSQL Installation Instructions (Windows)

If you do not have PostgreSQL installed, install it using Windows Package Manager (`winget`):

```powershell
# Open Windows PowerShell as Administrator and run:
winget install PostgreSQL.PostgreSQL.16 --silent --accept-package-agreements --accept-source-agreements
```

Alternatively, download the official installer:
- **Download**: [https://www.enterprisedb.com/downloads/postgres-postgresql-downloads](https://www.enterprisedb.com/downloads/postgres-postgresql-downloads)

### Configure PostgreSQL for Local Development:
Once installed, create the database in `psql` or pgAdmin:
```sql
CREATE DATABASE township_db;
```

---

## 🚀 Running the Project (Windows PowerShell)

Open two PowerShell windows to run both services simultaneously.

### Terminal 1 — Start Spring Boot Backend

```powershell
# 1. Navigate to the backend directory
cd township-management\backend

# 2. Ensure JAVA_HOME is set in your session (if not already permanent)
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot"

# 3. (Optional) Run tests to verify compilation
.\mvnw.cmd test

# 4. Start the Spring Boot application
.\mvnw.cmd spring-boot:run
```

*The backend starts at `http://localhost:8080`.*  
*Health Check endpoint:* [`http://localhost:8080/api/health`](http://localhost:8080/api/health)

---

### Terminal 2 — Start Vite React Frontend

```powershell
# 1. Navigate to the frontend directory
cd township-management\frontend

# 2. Install dependencies (only required on first setup)
npm install

# 3. Start the Vite development server
npm run dev
```

*The frontend starts at `http://localhost:5173`.*  
*Open your browser and visit:* [`http://localhost:5173`](http://localhost:5173)

---

## 🧪 Verification & Build Checks

### Backend Test Suite
```powershell
cd township-management\backend
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot"
.\mvnw.cmd test
```
**Results (16 Tests Run, 0 Failures, 0 Errors)**:
- `TownshipBackendApplicationTests`: PASSED (ApplicationContext bootstrapped)
- `HealthControllerTest`: PASSED (Health endpoint structure and PostgreSQL 16 connection probe)
- `AuthControllerTest`: PASSED (Valid login email/phone, invalid password rejection 401, resident registration with flat association 201, unauthorized rejection 401, authorized me 200)
- `BillingControllerTest`: PASSED (3 sample billing calculations, partial payment balances, duplicate bill prevention, resident data isolation 403, unauthorized bill generation rejection 403, authorized idempotent monthly bill generation, public registration privilege escalation prevention)
- `BUILD SUCCESS`

### Frontend Production Build
```powershell
cd township-management\frontend
npm run build
```
**Results**:
- Client bundle built cleanly in ~380ms
- Zero TypeScript/JSX/CSS errors

---

## 💰 Phase 5: Maintenance Billing & Flat Ledger Specifications

### Billing Rules
- **Base Maintenance Rate**: INR 6.50 per square foot per month (according to project brief).
- **Monthly Bill Formula**: `flat_area_sq_ft * 6.50`
- **Financial Precision**: All amounts strictly stored as `BigDecimal` in Java and `NUMERIC(12, 2)` in PostgreSQL. No floating-point operations.
- **Audit Ledger**: Every billing generation, fee assessment, and verified payment appends an immutable entry to `flat_ledger_entries` with debit, credit, running balance, and unique transaction reference.
- **Idempotency**: Composite unique constraint `(flat_id, billing_month)` prevents duplicate billing.

### Seeded Sample Invoices (Idempotent)
| Flat & Tower | Resident Name | Area | Calculation | Paid Amount | Outstanding Balance | Status & Details |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **T07-1203** | Neeraj Kulkarni | 1,150 sq ft | `1150 * 6.50 = ₹7,475.00` | ₹7,475.00 | **₹0.00** | `PAID` (October 2026 Settled) |
| **T12-0408** | Swati Joshi | 980 sq ft | `980 * 6.50 = ₹6,370.00` | ₹0.00 | **₹6,370.00** | `OVERDUE` (47 days overdue) |
| **T21-1601** | Aamir Shaikh | 1,640 sq ft | `1640 * 6.50 = ₹10,660.00` | ₹5,000.00 | **₹5,660.00** | `PARTIALLY_PAID` (18 days overdue balance) |

---

## 👥 Seeded Test Resident Accounts

The database seeder idempotently provisions all **28 towers (Tower A Orchid through Tower AB Zinnia)** and core residents with BCrypt-hashed passwords:

| Resident Name | Flat / Tower | Role | Login Identifier (Email or Phone) | Password | Details |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Neeraj Kulkarni** | Tower G (Iris) #1203 | `RESIDENT` | `neeraj.kulkarni@grandvistapune.in` or `+919822012031` | `Resident@123` | 1,150 sq ft • Primary Owner |
| **Swati Joshi** | Tower L (Magnolia) #0408 | `RESIDENT` | `swati.joshi@grandvistapune.in` or `+919822004082` | `Resident@123` | 980 sq ft • Primary Owner |
| **Aamir Shaikh** | Tower U (Foxglove) #1601 | `RESIDENT` | `aamir.shaikh@grandvistapune.in` or `+919822016013` | `Resident@123` | 1,640 sq ft • Primary Owner |
| **Col. Rajiv Sharma (Retd.)** | Facility Operations HQ | `HELP_DESK` | `helpdesk@grandvistapune.in` or `+919822000100` | `HelpDesk@123` | Apex Facility Lead & Super Admin |

---

## 🔒 Security & CORS

- **CORS**: Configured safely in `CorsConfig.java` to whitelist `http://localhost:5173`, `http://localhost:3000`, and `http://127.0.0.1:5173`.
- **Stateless JWT**: `JwtAuthenticationFilter` validates `Bearer <token>` on every protected request via HMAC-SHA256.
- **BCrypt Hashing**: All passwords salted and hashed using BCrypt (`BCryptPasswordEncoder`). Plaintext passwords are never stored.
- **Relational Integrity**: Unique constraints enforced on `users.email`, `users.phone`, and composite `flats(tower_id, flat_number)` and `flat_members(flat_id, user_id)`.
- **Credentials Protection**: No secrets committed to version control. `DB_PASSWORD` and `JWT_SECRET` loaded via environment variables.

