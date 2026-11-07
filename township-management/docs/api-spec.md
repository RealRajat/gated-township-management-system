# API Specification

## Phase 1 Endpoints

### 1. Health Probe
- **Endpoint**: `GET /api/health`
- **Access**: Public (Permitted through Spring Security)
- **Description**: Probes service uptime, system runtime metadata, and tests live connectivity to PostgreSQL.
- **Success Response (200 OK)**:
```json
{
  "status": "UP",
  "service": "Gated Township Resident Management System",
  "version": "1.0.0-SNAPSHOT",
  "environment": "dev",
  "timestamp": "2026-11-07T17:05:47.235641600Z",
  "database": {
    "type": "PostgreSQL",
    "status": "CONNECTED",
    "productName": "PostgreSQL",
    "productVersion": "16.2"
  },
  "system": {
    "javaVersion": "17.0.16",
    "osName": "Windows 11",
    "jvmFreeMemoryMB": 59,
    "jvmTotalMemoryMB": 90
  }
}
```

- **Degraded Response (200 OK - DB Unreachable)**:
```json
{
  "status": "DEGRADED",
  "service": "Gated Township Resident Management System",
  "version": "1.0.0-SNAPSHOT",
  "environment": "dev",
  "timestamp": "2026-11-07T17:05:47.235641600Z",
  "database": {
    "type": "PostgreSQL",
    "status": "DISCONNECTED",
    "error": "Could not connect to PostgreSQL: Connection refused",
    "suggestion": "Ensure PostgreSQL is installed, running on port 5432, and database 'township_db' exists."
  },
  "system": {
    "javaVersion": "17.0.16",
    "osName": "Windows 11",
    "jvmFreeMemoryMB": 59,
    "jvmTotalMemoryMB": 90
  }
}
```

## CORS Policy
- **Allowed Origins**: `http://localhost:5173`, `http://localhost:3000`, `http://127.0.0.1:5173` (configurable via `CORS_ALLOWED_ORIGINS`).
- **Allowed Methods**: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`
- **Allowed Headers**: `Authorization`, `Content-Type`, `X-Requested-With`, `Accept`
- **Credentials**: `true`
- **Max Age**: 3600 seconds

## Upcoming Roadmap Endpoints (Phase 2+)
- `POST /api/auth/request-otp`
- `POST /api/auth/verify-otp`
- `GET /api/billing/invoices`
- `POST /api/billing/reconcile-csv`
- `POST /api/complaints`
- `POST /api/complaints/ai-classify`

