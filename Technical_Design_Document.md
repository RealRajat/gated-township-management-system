# Technical Design Document
## Gated Township Resident Management System

**GitHub repository:** https://github.com/RealRajat/gated-township-management-system  
**Live application:** https://gated-township-management.vercel.app  
**Live REST API:** https://gated-township-api.onrender.com  
**API health endpoint:** https://gated-township-api.onrender.com/api/health  
**Database:** Neon PostgreSQL 16 (Singapore region)  
**Document date:** 8 November 2026  
**Version:** 1.1 - Evaluation deployment

---

## 1. Executive summary

This project is a responsive, role-based web application for a gated township. It brings three resident-facing operational flows into one product:

1. **Maintenance:** residents can review maintenance bills and ledger entries and make test-mode payments.
2. **Complaints:** residents can submit and track complaints; Help Desk staff can review, assign and update them. A classification assistant suggests a category and priority, while mandatory safety rules take precedence.
3. **Visitors:** residents can pre-approve visitors, and guards can validate passes and record entry and exit. Walk-in requests follow a resident-approval flow.

The application includes four roles: Resident, Help Desk, Guard and Committee. Supporting modules include bank-statement reconciliation, amenities and bookings, notices and committee analytics.

The public evaluation deployment has a React frontend on Vercel, a Spring Boot API on Render and PostgreSQL on Neon. It uses synthetic demonstration records only. It is not a production service for real residents, payments, visitor identity records or security operations.

## 2. Scope and what can be evaluated

### Core flows

| Flow | Resident experience | Staff experience |
|---|---|---|
| Maintenance | Sign in, view bills and ledger, make a test payment and inspect the receipt | Help Desk can review billing and reconciliation records |
| Complaints | Create a complaint, see classification suggestions and follow status/activity | Help Desk can review, assign and update the complaint |
| Visitors | Create a pre-approved pass and request approval for a walk-in visitor | Guard can look up a pass and record check-in/check-out |

### Supporting functionality

The repository also contains bank CSV reconciliation, amenity booking with capacity checks, notices/read receipts, and committee analytics/exports. These are supporting modules; the three flows above are the primary product slice for this brief.

### Demonstration data

The live Neon database was checked after seeding and contains 28 tower records, 3 sample flats, 6 demo users, 3 maintenance bills, 3 complaints, 4 visitor records, 5 amenities, 4 amenity bookings and 5 notices. The interface uses the approximate township label “28 Towers & 2,400 Units”; this is a scenario label, not a claim that 2,400 flat records are loaded. The hosted database contains only a small set of representative synthetic flats.

## 3. Architecture and technology choices

### Hosted architecture

| Layer | Implementation | Responsibility |
|---|---|---|
| Browser | React 19, React Router 7, Tailwind CSS 4 | Responsive role-specific UI |
| Frontend hosting | Vercel (Vite build) | Static assets and HTTPS delivery |
| API | Java 17, Spring Boot 4.1.2, Spring Security, Spring Data JPA | Authentication, validation, business rules and REST endpoints |
| Backend hosting | Render Docker web service | Runs the Spring Boot API |
| Database | Neon PostgreSQL 16 | Persistent relational records |
| Optional classifier provider | Google Gemini API | Contextual complaint suggestions when a key is configured |

**Hosted request path:** Browser -> Vercel React app -> HTTPS REST requests -> Render Spring Boot API -> Neon PostgreSQL over SSL.

The frontend reads its API origin from **VITE_API_BASE_URL**. The deployed value points to the Render API. The repository also contains a Docker Compose deployment: Nginx serves the frontend and proxies /api/ to Spring Boot, while PostgreSQL runs as a separate container with a persistent local volume. That Nginx path is for the Compose topology; it is not in the hosted Vercel request path.

### Technology rationale

- **React + Vite:** component-based UI and a static production build suitable for CDN hosting.
- **Spring Boot + Spring Security:** server-side validation, service-layer rules and role-based authorization.
- **PostgreSQL:** relational links between users, flats, bills, complaints, visitors and audit/activity records.
- **JPA/Hibernate:** entity mapping and persistence; production startup validates the schema.
- **JWT + BCrypt:** stateless API authentication and password hashing.
- **Docker:** repeatable backend packaging and local multi-container execution.

### Database bootstrap

The repository contains nine numbered PostgreSQL SQL bootstrap scripts (V1 through V9). They are plain SQL files, not Flyway migrations; the backend Maven configuration does not include Flyway. On a new Docker Compose database volume, the PostgreSQL image executes the scripts in /docker-entrypoint-initdb.d. The managed Neon database was initialized from the same scripts. Production uses Hibernate schema validation rather than automatic schema mutation.

## 4. Data model

| Entity group | Main records | Purpose |
|---|---|---|
| Township structure | towers, flats, flat_members | Unit inventory and resident-to-flat relationships |
| Identity | users | Resident, Help Desk, Guard and Committee accounts |
| Maintenance | maintenance_bills, billing_configurations, flat_ledger_entries | Bill balances and a transaction history |
| Payments | payments, bank_statement_batches, bank_transactions | Test payments and bank reconciliation |
| Complaints | complaints, complaint_activities | Ticket lifecycle, classification, assignment and activity history |
| Visitors | visitors, visitor_activities | Pass details and gate-entry audit history |
| Shared facilities | amenities, amenity_bookings | Facility capacity and booking slots |
| Communications | notices, notice_reads | Announcements and read acknowledgements |

The main relationships are: a tower contains flats; a flat can have linked members; bills and ledger entries belong to flats; complaints are filed by users and can be linked to a flat; visitors are associated with a host flat; and amenity bookings link a flat to an amenity.

## 5. Main end-to-end flows

### 5.1 Sign-in and authorization

1. A user signs in with a registered email or mobile number and password.
2. The API validates the credentials and returns a signed JWT.
3. The frontend stores the token in browser local storage and attaches it to authenticated API requests.
4. Backend authorization rules enforce the user's role and, for residents, the flat relationship. Hiding a screen in the frontend is not treated as the security boundary.
5. Public registration creates a Resident account; privileged roles are provisioned separately.

### 5.2 Maintenance and test payment

1. A resident opens the maintenance area and reviews the bills and ledger linked to their flat.
2. The resident selects a bill and submits a full or partial test payment.
3. The backend checks the bill balance and rejects invalid or excessive amounts.
4. The payment is recorded and the bill/ledger state is updated.
5. The UI presents the resulting payment/receipt record.

**Payment boundary:** this is a simulated/test flow. It does not charge money and is not connected to a payment processor.

### 5.3 Complaint and staff workflow

1. A resident submits a title and description.
2. The classification service returns a suggested category, priority, confidence value and explanation.
3. The complaint is saved with its status and activity history.
4. Help Desk staff review the ticket, assign/update it and maintain the activity trail.
5. The resident can follow the ticket and provide a rating when the workflow permits.

### 5.4 Visitor and gate workflow

1. A resident creates a visitor pass with the visit details and validity window.
2. The guard looks up the pass at the gate and records check-in/check-out.
3. A walk-in visitor can be sent through the resident-approval path before entry is confirmed.
4. Gate actions are recorded in visitor activity history.
5. The limited offline queue stores pending gate actions in the browser's local storage and requires a later manual sync. It is not a full offline application; clearing browser storage can remove unsynced actions.

## 6. Complaint classification and AI/automation

The classification pipeline is deliberately layered:

1. **Safety override first:** SafetyKeywordRuleEngine checks the title and description for defined fire/smoke, gas-leak, trapped-lift and electrical-shock/exposed-wire patterns. A match forces CRITICAL priority before the general classifier runs.
2. **Gemini path (optional):** when GEMINI_API_KEY is configured and the provider is available, the Gemini-backed service can provide contextual suggestions.
3. **Rule fallback:** when Gemini is not configured or is unavailable, RuleBasedClassificationEngine classifies using local keyword rules. It returns a heuristic confidence value (currently 60 for OTHER, 85 for a recognized category); this is not a statistically calibrated probability.
4. **Human review:** suggestions are intended to help staff triage, not replace Help Desk judgment. A category/priority can be reviewed as part of the staff workflow. A rule miss or false positive remains possible.

The live evaluation environment does not have a Gemini API key configured; it uses the local rule-based fallback. The mandatory safety override takes precedence over either path. This is bounded automation, not a claim of perfect AI accuracy or a substitute for emergency services and on-site safety procedures.

## 7. UI and design decisions

- **Sign-in:** a focused login card keeps the first action clear and displays validation/error feedback next to the form.
- **Resident dashboard:** surfaces role-relevant summaries and shortcuts to bills, complaints and visitors instead of exposing staff-only navigation.
- **Maintenance:** groups bill status, amounts and payment/ledger history so a resident can understand what is due and what has been recorded.
- **Complaints:** uses clear status, priority and activity information to make the ticket lifecycle visible to both residents and staff.
- **Visitor/gate screens:** prioritise pass lookup and entry/exit actions for a guard's operational context.
- **Staff screens:** separate Help Desk operational tasks and Committee reporting from the resident's self-service views.
- **Responsive layout:** navigation and content grids adapt to smaller screens; forms include input validation and user-visible errors.

The visual system uses a deep navy base, teal accents, slate text and restrained status colours. This gives primary actions and urgent states a consistent hierarchy without turning every element into an alert.

## 8. Security, privacy and operational controls

- Passwords are stored as BCrypt hashes.
- API endpoints use JWT authentication and role-based access checks.
- Resident resources are checked against the resident's flat membership on the backend.
- Request DTO validation and service-level checks protect core inputs and state changes.
- Production error responses suppress stack traces and internal exception details.
- Real secrets are supplied through environment variables, not committed configuration values.
- DataSeeder is configurable. The evaluation database was seeded before review; Render has APP_SEEDER_ENABLED disabled so later production restarts do not re-run demo seeding. Local Compose defaults the seeder on for a fresh demo database.
- The hosted demo credentials are intentionally included in deployment.md for the evaluator. They are synthetic test credentials, not production credentials.
- Complaint and visitor photos are written to the container filesystem. The free Render service has no persistent disk/object storage configured, so uploaded files can disappear after a restart or redeploy.

## 9. Assumptions and constraints

The supplied submission brief identifies the product as a gated-township resident app for maintenance, complaints and visitors, but does not provide detailed tariff, township inventory, staffing policy or service-level numbers. The following values are therefore **explicit demonstration assumptions**, not verified client policies:

| Assumption | Demonstration choice | How to change it |
|---|---|---|
| Township identity and scale | “Pune Grand Vista”, 28 towers and an approximate 2,400-unit label | Replace the sample configuration and seed data with the client's real inventory |
| Tower layout | 15 floors and a notional 86 units per tower in seed configuration | Import the client's actual tower/flat schedule |
| Maintenance tariff | INR 6.50 per square foot per month in demo billing configuration | Confirm the society's approved tariff and billing rules |
| Roles | Resident, Help Desk, Guard and Committee | Map to the client's actual job titles and permissions |
| Payment behavior | Test-only simulated full/partial payment | Integrate a real payment provider only after merchant approval and reconciliation design |
| AI provider | Gemini is optional; deterministic local fallback is active when it is absent | Configure a managed AI key and define review/monitoring policy |
| Visitor approval | Resident pre-approval and resident approval for walk-ins | Align with the client's actual gate protocol and retention rules |
| Single society | One township per deployment | Add tenant isolation before serving multiple societies |

## 10. Tests and verification

The repository contains ten backend test classes covering application context, authentication, billing, amenity booking, committee analytics, complaints, health, notices, payments/reconciliation and visitors. Run them from township-management/backend:

~~~bash
./mvnw test
~~~

The frontend production build can be checked from township-management/frontend:

~~~bash
npm ci
npm run build
npm run lint
~~~

The Render Docker image build currently uses -DskipTests; therefore a successful cloud deploy should not be interpreted as evidence that the test suite passed. Run the test command above separately and review any environment-dependent failures.

## 11. What was cut and what comes next

### Deliberately not integrated in this evaluation build

- Real-money payment processing and bank connectivity.
- SMS/WhatsApp/OTP delivery.
- Physical gate barriers, access controllers and CCTV hardware.
- Durable object storage for uploaded photos.
- A full multi-device offline sync engine.
- Multi-society tenancy and enterprise operational monitoring.

### Next steps before a production pilot

1. Confirm the actual maintenance rules, tariff, due dates and complaint SLAs with the client.
2. Replace demo users and seed records with controlled onboarding; rotate credentials and keep the seeder disabled.
3. Add persistent, access-controlled object storage and an explicit retention/deletion policy for photos.
4. Add a real payment provider only after reconciliation, refunds and audit requirements are agreed.
5. Add SMS/OTP or notification delivery, physical gate integration and operational monitoring.
6. Run the full test suite in CI, perform security testing and load-test realistic township volumes.
