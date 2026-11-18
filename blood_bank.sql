-- ============================================================================
-- HOSPITAL BLOOD BANK & DONOR MATCHER DATABASE SCHEMA
-- Compatible with MySQL 5.7+ / MySQL 8.0+ / MariaDB
-- Description: Tracks donated blood units by ABO/Rh group, component
--              (plasma, platelets, RBC), expiration dates, and matches
--              emergency hospital requests with compatible inventory & donors.
-- ============================================================================

CREATE DATABASE IF NOT EXISTS `blood_bank_db` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `blood_bank_db`;

-- ----------------------------------------------------------------------------
-- 1. TABLE: donors
-- Stores registered volunteer blood donors with contact & eligibility info
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS `match_allocations`;
DROP TABLE IF EXISTS `blood_inventory`;
DROP TABLE IF EXISTS `emergency_requests`;
DROP TABLE IF EXISTS `donors`;

CREATE TABLE `donors` (
    `donor_id` INT AUTO_INCREMENT PRIMARY KEY,
    `full_name` VARCHAR(100) NOT NULL,
    `blood_type` ENUM('A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-') NOT NULL,
    `gender` ENUM('Male', 'Female', 'Other') NOT NULL DEFAULT 'Male',
    `age` INT NOT NULL,
    `phone` VARCHAR(25) NOT NULL,
    `email` VARCHAR(100),
    `city` VARCHAR(100) NOT NULL,
    `last_donation_date` DATE,
    `status` ENUM('Eligible', 'Deferred', 'Ineligible') NOT NULL DEFAULT 'Eligible',
    `registered_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_donor_blood_type` (`blood_type`),
    INDEX `idx_donor_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------------------------
-- 2. TABLE: blood_inventory
-- Tracks donated blood units by ABO/Rh group, component, volume & expiration
-- Standards:
--   - Red Blood Cells (RBC): refrigerated ~42 days lifespan
--   - Platelets: room temperature with agitation ~5-7 days lifespan
--   - Plasma (Fresh Frozen): frozen ~365 days (1 year) lifespan
-- ----------------------------------------------------------------------------
CREATE TABLE `blood_inventory` (
    `unit_id` INT AUTO_INCREMENT PRIMARY KEY,
    `unit_code` VARCHAR(30) NOT NULL UNIQUE,
    `donor_id` INT NULL,
    `blood_type` ENUM('A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-') NOT NULL,
    `component` ENUM('Red Blood Cells (RBC)', 'Platelets', 'Plasma') NOT NULL,
    `volume_ml` INT NOT NULL DEFAULT 450,
    `donation_date` DATE NOT NULL,
    `expiration_date` DATE NOT NULL,
    `storage_location` VARCHAR(100) NOT NULL,
    `status` ENUM('Available', 'Reserved', 'Transfused', 'Expired') NOT NULL DEFAULT 'Available',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_inventory_donor` FOREIGN KEY (`donor_id`) 
        REFERENCES `donors` (`donor_id`) ON DELETE SET NULL,
    INDEX `idx_inventory_blood_comp` (`blood_type`, `component`),
    INDEX `idx_inventory_expiry` (`expiration_date`),
    INDEX `idx_inventory_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------------------------
-- 3. TABLE: emergency_requests
-- Hospital requests for urgent blood transfusions with urgency rating
-- ----------------------------------------------------------------------------
CREATE TABLE `emergency_requests` (
    `request_id` INT AUTO_INCREMENT PRIMARY KEY,
    `patient_name` VARCHAR(100) NOT NULL,
    `hospital_name` VARCHAR(150) NOT NULL,
    `required_blood_type` ENUM('A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-') NOT NULL,
    `required_component` ENUM('Red Blood Cells (RBC)', 'Platelets', 'Plasma') NOT NULL,
    `units_needed` INT NOT NULL DEFAULT 1,
    `urgency_level` ENUM('CRITICAL', 'URGENT', 'ROUTINE') NOT NULL DEFAULT 'CRITICAL',
    `request_date` DATE NOT NULL,
    `status` ENUM('Pending', 'Matched', 'Fulfilled', 'Cancelled') NOT NULL DEFAULT 'Pending',
    `notes` TEXT,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_req_blood_comp` (`required_blood_type`, `required_component`),
    INDEX `idx_req_urgency` (`urgency_level`),
    INDEX `idx_req_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------------------------
-- 4. TABLE: match_allocations
-- Links emergency requests to allocated blood inventory units
-- ----------------------------------------------------------------------------
CREATE TABLE `match_allocations` (
    `allocation_id` INT AUTO_INCREMENT PRIMARY KEY,
    `request_id` INT NOT NULL,
    `unit_id` INT NOT NULL,
    `allocated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `status` ENUM('Allocated', 'Dispatched', 'Transfused') NOT NULL DEFAULT 'Allocated',
    CONSTRAINT `fk_alloc_request` FOREIGN KEY (`request_id`) 
        REFERENCES `emergency_requests` (`request_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_alloc_unit` FOREIGN KEY (`unit_id`) 
        REFERENCES `blood_inventory` (`unit_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================================
-- SEED SAMPLE DATA: DONORS
-- (10 realistic donors spanning all ABO/Rh blood groups)
-- ============================================================================
INSERT INTO `donors` (`donor_id`, `full_name`, `blood_type`, `gender`, `age`, `phone`, `email`, `city`, `last_donation_date`, `status`) VALUES
(1, 'Sarah Jenkins',      'O-',  'Female', 29, '+1 (555) 234-8901', 'sarah.j@gmail.com',     'Metro City',   '2026-08-15', 'Eligible'),
(2, 'Marcus Vance',       'O+',  'Male',   34, '+1 (555) 345-6712', 'mvance@outlook.com',    'Springfield',  '2026-09-01', 'Eligible'),
(3, 'Dr. Priya Sharma',   'A+',  'Female', 38, '+1 (555) 456-7823', 'priya.s@hospital.org',  'Metro City',   '2026-10-10', 'Eligible'),
(4, 'Liam O\'Connor',     'A-',  'Male',   26, '+1 (555) 567-8934', 'liam.oc@gmail.com',     'Riverdale',    '2026-11-01', 'Deferred'),
(5, 'Carlos Ramirez',     'B+',  'Male',   41, '+1 (555) 678-9045', 'c.ramirez@company.net', 'Metro City',   '2026-07-20', 'Eligible'),
(6, 'Amina Al-Mansoor',   'B-',  'Female', 31, '+1 (555) 789-0156', 'amina.m@gmail.com',     'Oakwood',      '2026-08-28', 'Eligible'),
(7, 'David Kim',          'AB+', 'Male',   24, '+1 (555) 890-1267', 'dkim99@gmail.com',      'Springfield',  '2026-09-18', 'Eligible'),
(8, 'Elena Rostova',      'AB-', 'Female', 35, '+1 (555) 901-2378', 'elena.rost@tech.io',    'Metro City',   '2026-10-05', 'Eligible'),
(9, 'James Wilson',       'O+',  'Male',   45, '+1 (555) 112-3344', 'jwilson@cityline.org',  'Riverdale',    '2026-06-12', 'Eligible'),
(10, 'Sophia Martinez',   'O-',  'Female', 27, '+1 (555) 223-4455', 'smartinez@gmail.com',   'Metro City',   '2026-09-22', 'Eligible');

-- ============================================================================
-- SEED SAMPLE DATA: BLOOD INVENTORY
-- (Varied components: RBC, Platelets, Plasma with realistic expiration dates)
-- ============================================================================
INSERT INTO `blood_inventory` (`unit_id`, `unit_code`, `donor_id`, `blood_type`, `component`, `volume_ml`, `donation_date`, `expiration_date`, `storage_location`, `status`) VALUES
-- Universal RBC Donor (O-) units
(1,  'BLD-2026-0101', 1,  'O-',  'Red Blood Cells (RBC)', 450, '2026-11-01', '2026-12-13', 'Main Cold Room - Shelf A1', 'Available'),
(2,  'BLD-2026-0102', 10, 'O-',  'Red Blood Cells (RBC)', 450, '2026-10-18', '2026-11-29', 'Main Cold Room - Shelf A1', 'Available'),
(3,  'BLD-2026-0103', 1,  'O-',  'Plasma',                250, '2026-06-10', '2027-06-10', 'Deep Freezer - Box F1',    'Available'),

-- O+ Blood Units
(4,  'BLD-2026-0104', 2,  'O+',  'Red Blood Cells (RBC)', 450, '2026-11-05', '2026-12-17', 'Main Cold Room - Shelf A2', 'Available'),
(5,  'BLD-2026-0105', 9,  'O+',  'Platelets',             300, '2026-11-15', '2026-11-21', 'Agitator Chamber - Bay 1', 'Available'),
(6,  'BLD-2026-0106', 2,  'O+',  'Plasma',                250, '2026-08-01', '2027-08-01', 'Deep Freezer - Box F2',    'Available'),

-- A+ Blood Units
(7,  'BLD-2026-0107', 3,  'A+',  'Red Blood Cells (RBC)', 450, '2026-11-10', '2026-12-22', 'Main Cold Room - Shelf B1', 'Available'),
(8,  'BLD-2026-0108', 3,  'A+',  'Platelets',             300, '2026-11-14', '2026-11-20', 'Agitator Chamber - Bay 2', 'Available'),
(9,  'BLD-2026-0109', 3,  'A+',  'Plasma',                250, '2026-09-15', '2027-09-15', 'Deep Freezer - Box F3',    'Available'),

-- A- Blood Units
(10, 'BLD-2026-0110', 4,  'A-',  'Red Blood Cells (RBC)', 450, '2026-10-12', '2026-11-23', 'Main Cold Room - Shelf B2', 'Available'),

-- B+ Blood Units
(11, 'BLD-2026-0111', 5,  'B+',  'Red Blood Cells (RBC)', 450, '2026-10-25', '2026-12-06', 'Main Cold Room - Shelf C1', 'Available'),
(12, 'BLD-2026-0112', 5,  'B+',  'Platelets',             300, '2026-11-16', '2026-11-22', 'Agitator Chamber - Bay 3', 'Available'),

-- B- Blood Units
(13, 'BLD-2026-0113', 6,  'B-',  'Red Blood Cells (RBC)', 450, '2026-10-20', '2026-12-01', 'Main Cold Room - Shelf C2', 'Available'),

-- AB+ Blood Units (Universal Recipient for RBC, High-value Platelets)
(14, 'BLD-2026-0114', 7,  'AB+', 'Red Blood Cells (RBC)', 450, '2026-11-08', '2026-12-20', 'Main Cold Room - Shelf D1', 'Available'),
(15, 'BLD-2026-0115', 7,  'AB+', 'Platelets',             300, '2026-11-13', '2026-11-19', 'Agitator Chamber - Bay 4', 'Available'),

-- AB- Blood Units (Universal Plasma Donor)
(16, 'BLD-2026-0116', 8,  'AB-', 'Plasma',                250, '2026-05-20', '2027-05-20', 'Deep Freezer - Box F4',    'Available'),
(17, 'BLD-2026-0117', 8,  'AB-', 'Red Blood Cells (RBC)', 450, '2026-10-08', '2026-11-19', 'Main Cold Room - Shelf D2', 'Available');

-- ============================================================================
-- SEED SAMPLE DATA: EMERGENCY REQUESTS
-- (Urgent and critical hospital transfusions requiring match engine)
-- ============================================================================
INSERT INTO `emergency_requests` (`request_id`, `patient_name`, `hospital_name`, `required_blood_type`, `required_component`, `units_needed`, `urgency_level`, `request_date`, `status`, `notes`) VALUES
(1, 'Michael Chang',   'City Trauma Center - ICU',  'A+', 'Red Blood Cells (RBC)', 2, 'CRITICAL', '2026-11-17', 'Pending',   'Severe motor vehicle trauma with hemorrhagic shock.'),
(2, 'Emma Watson',      'St. Jude Children Hospital','O-', 'Red Blood Cells (RBC)', 1, 'CRITICAL', '2026-11-17', 'Pending',   'Emergency pediatric cardiac surgery. Strict O- negative required.'),
(3, 'Robert Gallagher', 'Memorial Oncology Wing',    'B+', 'Platelets',             2, 'URGENT',   '2026-11-16', 'Pending',   'Severe post-chemotherapy thrombocytopenia, active mucosal bleed.'),
(4, 'Anita Desai',      'Women & Children Hospital', 'AB+','Plasma',                1, 'ROUTINE',  '2026-11-16', 'Fulfilled', 'Disseminated intravascular coagulation management - unit dispatched.'),
(5, 'Lucas Bennett',    'General Hospital - ER',     'O+', 'Red Blood Cells (RBC)', 2, 'CRITICAL', '2026-11-17', 'Pending',   'Ruptured abdominal aortic aneurysm, massive transfusion protocol initiated.');

-- ============================================================================
-- SEED SAMPLE DATA: ALLOCATIONS
-- ============================================================================
INSERT INTO `match_allocations` (`allocation_id`, `request_id`, `unit_id`, `status`) VALUES
(1, 4, 16, 'Dispatched');
