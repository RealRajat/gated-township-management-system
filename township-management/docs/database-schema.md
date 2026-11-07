# Database Schema Blueprint (PostgreSQL)

## Architecture Overview
The database layer uses PostgreSQL 16/17 with Spring Data JPA. All monetary ledger entries are modeled with `DECIMAL(12,2)` precision to prevent floating-point calculation discrepancies in Indian Rupee transactions.

## Target Tables (Phase 2+)

### 1. `towers`
- `id` (VARCHAR(10), PRIMARY KEY): e.g., 'T01' to 'T28'
- `name` (VARCHAR(100)): e.g., 'Tower A (Orchid)'
- `total_floors` (INT): default 15
- `total_units` (INT): default 86
- `created_at` (TIMESTAMP)

### 2. `units`
- `id` (BIGSERIAL, PRIMARY KEY)
- `tower_id` (VARCHAR(10), FK -> `towers.id`)
- `unit_number` (VARCHAR(20)): e.g., 'A-101'
- `unit_type` (VARCHAR(20)): e.g., '2BHK_STD', '3BHK_LUX'
- `super_built_up_sqft` (DECIMAL(8,2))
- `carpet_sqft` (DECIMAL(8,2))
- `floor_number` (INT)
- `occupancy_status` (VARCHAR(20)): 'OWNER_OCCUPIED', 'TENANT_OCCUPIED', 'VACANT'

### 3. `users`
- `id` (BIGSERIAL, PRIMARY KEY)
- `full_name` (VARCHAR(150))
- `phone_number` (VARCHAR(20), UNIQUE): +91 Indian format
- `email` (VARCHAR(150), UNIQUE)
- `role` (VARCHAR(50)): 'SUPER_ADMIN', 'FACILITY_MANAGER', 'HELP_DESK', 'TOWER_COMMITTEE', 'RESIDENT'
- `active` (BOOLEAN)
- `created_at` (TIMESTAMP)

### 4. `maintenance_invoices`
- `id` (BIGSERIAL, PRIMARY KEY)
- `invoice_number` (VARCHAR(50), UNIQUE)
- `unit_id` (BIGINT, FK -> `units.id`)
- `billing_quarter` (VARCHAR(10)): e.g., 'Q1-2026'
- `base_charge_inr` (DECIMAL(12,2))
- `sinking_fund_inr` (DECIMAL(12,2))
- `gst_amount_inr` (DECIMAL(12,2))
- `total_amount_inr` (DECIMAL(12,2))
- `due_date` (DATE)
- `payment_status` (VARCHAR(30)): 'PENDING', 'PAID', 'OVERDUE'

### 5. `bank_transactions` (CSV Bank Statements)
- `id` (BIGSERIAL, PRIMARY KEY)
- `transaction_reference` (VARCHAR(100), UNIQUE): UTR / Bank Reference No
- `transaction_date` (TIMESTAMP)
- `amount_inr` (DECIMAL(12,2))
- `narration` (TEXT)
- `reconciliation_status` (VARCHAR(30)): 'MATCHED', 'UNMATCHED', 'MANUAL_REVIEW'
- `matched_invoice_id` (BIGINT, FK -> `maintenance_invoices.id`, NULLABLE)

### 6. `complaints`
- `id` (BIGSERIAL, PRIMARY KEY)
- `ticket_number` (VARCHAR(50), UNIQUE)
- `resident_id` (BIGINT, FK -> `users.id`)
- `unit_id` (BIGINT, FK -> `units.id`)
- `title` (VARCHAR(255))
- `description` (TEXT)
- `ai_category` (VARCHAR(100)): 'Plumbing', 'Electrical', 'Elevator', 'Security', etc.
- `ai_priority` (VARCHAR(30)): 'CRITICAL', 'HIGH', 'MEDIUM', 'LOW'
- `ai_suggested_team` (VARCHAR(100))
- `assigned_staff_id` (BIGINT, FK -> `users.id`, NULLABLE)
- `status` (VARCHAR(50)): 'OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'
- `created_at` (TIMESTAMP)
- `resolved_at` (TIMESTAMP, NULLABLE)

