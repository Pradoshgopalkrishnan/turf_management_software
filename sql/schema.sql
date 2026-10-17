-- ============================================
-- Turf Booking System: schema.sql
-- Run this FIRST. It creates the database and all tables.
-- To start over from scratch, uncomment the next line:
-- DROP DATABASE IF EXISTS turf;
-- ============================================

CREATE DATABASE IF NOT EXISTS turf;
USE turf;

-- Admins and customers
CREATE TABLE users (
    user_id   INT AUTO_INCREMENT PRIMARY KEY,
    name      VARCHAR(100) NOT NULL,
    contact   VARCHAR(15)  NOT NULL,
    username  VARCHAR(50)  NOT NULL UNIQUE,
    password  VARCHAR(100) NOT NULL,
    role      ENUM('ADMIN','CUSTOMER') NOT NULL
);

-- Turfs that can be booked
CREATE TABLE turfs (
    turf_id     INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    location    VARCHAR(150),
    capacity    INT NOT NULL,
    base_price  DOUBLE NOT NULL          -- price for one 1-hour slot
);

-- Surge multipliers (weekend / evening)
CREATE TABLE pricing_rules (
    rule_name   VARCHAR(20) PRIMARY KEY, -- 'WEEKEND' or 'EVENING'
    multiplier  DOUBLE NOT NULL
);

-- Discount codes
CREATE TABLE promo_codes (
    code              VARCHAR(20) PRIMARY KEY,
    discount_percent  DOUBLE NOT NULL,
    expiry_date       DATE NOT NULL,
    is_active         BOOLEAN DEFAULT TRUE
);

-- Every booking ever made
CREATE TABLE bookings (
    booking_id        INT AUTO_INCREMENT PRIMARY KEY,
    user_id           INT NOT NULL,
    turf_id           INT NOT NULL,
    booking_date      DATE NOT NULL,
    time_slot         VARCHAR(20) NOT NULL,   -- e.g. '18:00-19:00'
    base_price        DOUBLE NOT NULL,
    surge_multiplier  DOUBLE NOT NULL,
    promo_code        VARCHAR(20),            -- NULL if no promo used
    discount_amount   DOUBLE DEFAULT 0,
    final_total       DOUBLE NOT NULL,
    status            ENUM('CONFIRMED','CANCELLED') DEFAULT 'CONFIRMED',
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (turf_id) REFERENCES turfs(turf_id)
);
