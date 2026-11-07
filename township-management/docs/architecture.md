# Township Management System — Architecture Overview

## Project Scope
- **Scale**: 2,400 residential apartments distributed across 28 multi-story towers.
- **Location**: Pune, Maharashtra, India.
- **Client**: Facility Management Company overseeing residential operations, security, amenities, and billing.

## Core Pillars
1. **Frontend**: React 19 + Vite 8 + Tailwind CSS v4 + React Router v7 + Recharts.
2. **Backend**: Spring Boot 4 + Java 17 (Eclipse Adoptium OpenJDK 17) + Spring Web + Spring Security + Spring Data JPA + Hibernate.
3. **Database**: PostgreSQL 16/17 (Relational, ACID-compliant for financial ledger entries).
4. **Security**: Stateless JWT token authentication with mock OTP phone verification for Indian mobile numbers (+91).
5. **AI Module (Phase 4)**: Google Gemini API for auto-categorization and severity classification of resident help desk tickets.
6. **Deployments**: Vercel for the React frontend, Render for the Spring Boot backend service and managed PostgreSQL.

## Monorepo Layout
```
township-management/
├── frontend/             # Vite + React 19 + Tailwind CSS + React Router
│   ├── src/
│   │   ├── components/   # Navbar, Status Cards, Overview Cards
│   │   ├── pages/        # HomePage, ArchitecturePage
│   │   ├── services/     # api.js client
│   │   ├── App.jsx
│   │   └── main.jsx
│   ├── .env.example
│   ├── package.json
│   └── vite.config.js
├── backend/              # Spring Boot 4 + Java 17 + Maven
│   ├── src/main/java/com/township/
│   │   ├── config/       # CorsConfig, SecurityConfig
│   │   ├── controller/   # HealthController (/api/health)
│   │   ├── dto/          # HealthResponseDto
│   │   └── TownshipBackendApplication.java
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── application-dev.yml
│   ├── .env.example
│   ├── pom.xml
│   ├── mvnw & mvnw.cmd
│   └── .mvn/
├── docs/                 # System architecture, API specification, DB schema
│   ├── architecture.md
│   ├── api-spec.md
│   └── database-schema.md
├── sample-data/          # Township & 28-tower mock configuration
│   ├── township-config.json
│   └── towers-sample.json
├── README.md             # Developer setup, PowerShell commands, and environment guide
└── .gitignore
```

