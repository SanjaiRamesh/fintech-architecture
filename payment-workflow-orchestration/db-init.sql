-- Run this once against your local PostgreSQL instance before starting the services.
-- psql -U postgres -f db-init.sql

-- Shared service account
CREATE USER payment_user WITH PASSWORD 'payment_pass';

-- One database per service (database-per-service pattern)
CREATE DATABASE payment_orchestrator_db OWNER payment_user;
CREATE DATABASE risk_fraud_db           OWNER payment_user;
CREATE DATABASE routing_db              OWNER payment_user;
CREATE DATABASE fx_db                   OWNER payment_user;
CREATE DATABASE ledger_db               OWNER payment_user;
CREATE DATABASE notification_db         OWNER payment_user;
CREATE DATABASE reconciliation_db       OWNER payment_user;

-- Grant connection privileges
GRANT ALL PRIVILEGES ON DATABASE payment_orchestrator_db TO payment_user;
GRANT ALL PRIVILEGES ON DATABASE risk_fraud_db           TO payment_user;
GRANT ALL PRIVILEGES ON DATABASE routing_db              TO payment_user;
GRANT ALL PRIVILEGES ON DATABASE fx_db                   TO payment_user;
GRANT ALL PRIVILEGES ON DATABASE ledger_db               TO payment_user;
GRANT ALL PRIVILEGES ON DATABASE notification_db         TO payment_user;
GRANT ALL PRIVILEGES ON DATABASE reconciliation_db       TO payment_user;
