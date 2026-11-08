# Deployment and Reviewer Guide

## Gated Township Resident Management System

### Live evaluation links

- **Frontend:** https://gated-township-management.vercel.app
- **Backend API:** https://gated-township-api.onrender.com
- **Health endpoint:** https://gated-township-api.onrender.com/api/health
- **GitHub:** https://github.com/RealRajat/gated-township-management-system

This is a free-tier evaluation deployment backed by Neon PostgreSQL 16. It uses synthetic demo records only. Do not enter real resident, payment, visitor identity or security data. Render may cold-start after inactivity. Complaint and visitor photos are on the container filesystem and are not durable across restarts/redeploys because no persistent disk or object storage is configured.

## 1. Local prerequisites

- Git
- Java 17
- Node.js and npm
- PostgreSQL 16, or Docker Engine with Docker Compose
- Windows 10/11, Linux or macOS

## 2. Quick start with Docker Compose

Run these commands from the repository root:

~~~bash
git clone https://github.com/RealRajat/gated-township-management-system.git
cd gated-township-management-system
~~~

Copy the environment template:

**Linux/macOS**
~~~bash
cp .env.example .env
~~~

**Windows PowerShell**
~~~powershell
Copy-Item .env.example .env
~~~

Edit .env and supply local database and JWT values. The application requires a JWT secret; generate a fresh one rather than using a shared value. For example:

~~~bash
openssl rand -base64 64
~~~

For local demo data, keep APP_SEEDER_ENABLED enabled. Do not use that setting for a real production deployment.

Start the stack from the repository root:

~~~bash
docker compose -f township-management/docker-compose.yml up --build -d
~~~

Open:

- Frontend: http://localhost/
- Backend health: http://localhost:8080/api/health
- PostgreSQL: localhost:5432

Follow backend startup:

~~~bash
docker compose -f township-management/docker-compose.yml logs -f backend
~~~

The PostgreSQL image executes the numbered SQL bootstrap scripts from backend/src/main/resources/db/migration/ when it initializes a **new empty database volume**. These are plain SQL scripts, not Flyway migrations. The V1-V9 scripts create the schema expected by the JPA entities.

To stop the local stack:

~~~bash
docker compose -f township-management/docker-compose.yml down
~~~

Keep the named database volume when stopping. Do not remove volumes unless you intentionally want to delete the local demo database.

## 3. Run frontend and backend separately

### Backend

Create a local PostgreSQL database, then set the backend environment variables listed below. From the repository root:

~~~bash
cd township-management/backend
./mvnw spring-boot:run
~~~

On Windows PowerShell:

~~~powershell
cd township-management/backend
.\mvnw.cmd spring-boot:run
~~~

The default local backend port is 8080. The development profile uses Hibernate schema update for local convenience; production uses schema validation.

### Frontend

In a second terminal:

~~~bash
cd township-management/frontend
npm ci
npm run dev
~~~

Open http://localhost:5173. The Vite development proxy sends /api requests to http://localhost:8080. The optional VITE_API_BASE_URL can be set when the frontend must call a different API origin.

## 4. Tests and build checks

From township-management/backend:

~~~bash
./mvnw test
~~~

From township-management/frontend:

~~~bash
npm ci
npm run build
npm run lint
~~~

The repository includes ten backend test classes covering authentication, billing, complaints, visitors, amenities, committee analytics, notices, payments/reconciliation, health and application startup. The Render Dockerfile builds with tests skipped, so run the test command separately; a successful deployment is not a substitute for a test run.

## 5. Cloud deployment topology

| Component | Platform | Configuration |
|---|---|---|
| React/Vite frontend | Vercel | Root directory township-management/frontend; build command npm run build; output dist |
| Spring Boot API | Render | Docker service built from township-management/backend/Dockerfile; free plan, Singapore region |
| PostgreSQL | Neon | PostgreSQL 16, Singapore region; credentials stored in Render environment settings |

The Vercel project is connected to the public GitHub repository. VITE_API_BASE_URL points the built frontend to the Render API. Render is connected to the same repository's main branch and automatically deploys new commits. The backend receives the Neon database connection through Render environment variables; credentials are not stored in this repository.

### Environment variable names

Only names are listed here; never commit values or paste secrets into source control.

**Backend**
- SPRING_PROFILES_ACTIVE
- APP_SEEDER_ENABLED
- PORT
- DB_HOST
- DB_PORT
- DB_NAME
- DB_USER
- DB_PASSWORD
- SPRING_DATASOURCE_URL (optional full JDBC URL override; used for hosted Neon connectivity)
- JWT_SECRET
- JWT_EXPIRATION_MS
- CORS_ALLOWED_ORIGINS
- GEMINI_API_KEY (optional; when omitted/unavailable, the local rule-based classifier is used)

**Frontend**
- VITE_API_BASE_URL

The evaluation database was seeded before review. APP_SEEDER_ENABLED is disabled on Render so future production restarts do not re-run the demo seeder; existing reviewer records remain in the database. Local Docker Compose defaults the seeder on for a fresh demo database.

## 6. Reviewer demo logins

Open the live frontend and select **Sign In**. Use the email address and password below (email login is recommended).

| Role | Login email | Password |
|---|---|---|
| Resident | neeraj.kulkarni@grandvistapune.in | Resident@123 |
| Resident | swati.joshi@grandvistapune.in | Resident@123 |
| Resident | aamir.shaikh@grandvistapune.in | Resident@123 |
| Help Desk | helpdesk@grandvistapune.in | HelpDesk@123 |
| Guard | guard@grandvistapune.in | Guard@123 |
| Committee | committee@grandvistapune.in | Committee@123 |

These are intentionally published **evaluation-only** credentials for synthetic test accounts. They are not real residents or production credentials. Please avoid reusing these passwords elsewhere. Before any real production pilot, replace/delete the demo accounts, rotate all credentials, and keep demo seeding disabled.

## 7. Operational boundaries

- Payments are simulated/test-only; no real money is charged.
- The live environment does not have a Gemini API key configured. It uses the deterministic keyword/rule-based classifier; critical safety keyword matches take precedence.
- No SMS/WhatsApp/OTP delivery is configured.
- Physical boom barriers, access-control hardware and CCTV are not integrated.
- Gate offline actions are a limited browser-local queue with manual sync, not full offline operation.
- Uploaded photos are not persisted to durable object storage on the free Render service.
- Use synthetic data only in this public evaluation environment.
