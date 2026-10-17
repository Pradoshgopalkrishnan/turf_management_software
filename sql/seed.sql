-- ============================================
-- Turf Booking System: seed.sql
-- Run this SECOND (after schema.sql). Adds test data.
-- ============================================

USE turf;

INSERT INTO users (name, contact, username, password, role) VALUES
('Admin Owner',   '9000000000', 'admin', 'admin123', 'ADMIN'),
('Test Customer', '9111111111', 'cust1', 'pass123',  'CUSTOMER');

INSERT INTO turfs (name, location, capacity, base_price) VALUES
('Green Arena',   'Thrikkakara', 14, 1000),
('Kick Off Turf', 'Kakkanad',    10,  800);

INSERT INTO pricing_rules (rule_name, multiplier) VALUES
('WEEKEND', 1.20),
('EVENING', 1.30);

INSERT INTO promo_codes (code, discount_percent, expiry_date, is_active) VALUES
('WELCOME10', 10, '2026-12-31', TRUE),   -- valid code
('OLD20',     20, '2026-01-01', TRUE),   -- expired code (for testing)
('OFF50',     50, '2026-12-31', FALSE);  -- inactive code (for testing)
