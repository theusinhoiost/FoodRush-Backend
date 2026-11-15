-- Migration V7: Add Mercado Pago Connect split payment and commission fields to restaurants
ALTER TABLE restaurants
    ADD COLUMN commission_rate NUMERIC(5,2) DEFAULT 10.00 NOT NULL,
    ADD COLUMN mp_collector_id VARCHAR(100),
    ADD COLUMN mp_access_token TEXT,
    ADD COLUMN mp_refresh_token TEXT,
    ADD COLUMN mp_connected BOOLEAN DEFAULT FALSE NOT NULL,
    ADD COLUMN mp_connected_at TIMESTAMP WITH TIME ZONE;
