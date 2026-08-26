-- LockElite — DDL Schema (CREATE TABLE IF NOT EXISTS — safe to run on every startup)
-- INSERT data is handled by DataInitializer.java (Spring @PostConstruct)

CREATE DATABASE IF NOT EXISTS lockelite_db1
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- BANKS
CREATE TABLE IF NOT EXISTS banks (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    code           VARCHAR(30)  NOT NULL UNIQUE,
    name           VARCHAR(100) NOT NULL,
    primary_color  VARCHAR(10)  NOT NULL DEFAULT '#F68222',
    sidebar_color  VARCHAR(10)  NOT NULL DEFAULT '#0f172a',
    bg_color       VARCHAR(10)  NOT NULL DEFAULT '#F5F0E8',
    accent_color   VARCHAR(10)  NOT NULL DEFAULT '#FFF0E0',
    layout         VARCHAR(20)  NOT NULL DEFAULT 'sidebar',
    logo_text      VARCHAR(10)  NOT NULL DEFAULT 'LE',
    is_active      BOOLEAN DEFAULT TRUE
) ENGINE=InnoDB;

-- BRANCHES
CREATE TABLE IF NOT EXISTS branches (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    bank_id     BIGINT,
    bank_name   VARCHAR(100) NOT NULL,
    branch_name VARCHAR(100) NOT NULL,
    address     TEXT         NOT NULL,
    latitude    DECIMAL(10,8),
    longitude   DECIMAL(11,8),
    phone       VARCHAR(20),
    is_active   BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (bank_id) REFERENCES banks(id) ON DELETE SET NULL,
    INDEX idx_branch_bank (bank_id)
) ENGINE=InnoDB;

-- USERS
CREATE TABLE IF NOT EXISTS users (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name        VARCHAR(100) NOT NULL,
    email            VARCHAR(150) NOT NULL UNIQUE,
    username         VARCHAR(50)  NOT NULL UNIQUE,
    password_hash    VARCHAR(255) NOT NULL,
    phone_number     VARCHAR(15)  NOT NULL,
    date_of_birth    DATE         NOT NULL,
    role             ENUM('CUSTOMER','EMPLOYEE','ADMIN') NOT NULL,
    is_active        BOOLEAN DEFAULT TRUE,
    email_verified   BOOLEAN DEFAULT FALSE,
    password_changed BOOLEAN DEFAULT TRUE,
    bank_id          BIGINT,
    branch_id        BIGINT,
    created_at       DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (bank_id)   REFERENCES banks(id)    ON DELETE SET NULL,
    FOREIGN KEY (branch_id) REFERENCES branches(id) ON DELETE SET NULL,
    INDEX idx_user_email  (email),
    INDEX idx_user_role   (role),
    INDEX idx_user_bank   (bank_id),
    INDEX idx_user_branch (branch_id)
) ENGINE=InnoDB;

-- LOCKERS
CREATE TABLE IF NOT EXISTS lockers (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    branch_id     BIGINT        NOT NULL,
    locker_number VARCHAR(20)   NOT NULL,
    floor         VARCHAR(10)   NOT NULL,
    size          ENUM('SMALL','MEDIUM','LARGE','XLARGE') NOT NULL,
    price         DECIMAL(10,2) NOT NULL,
    status        ENUM('AVAILABLE','RESERVED','OCCUPIED','SUSPENDED') DEFAULT 'AVAILABLE',
    FOREIGN KEY (branch_id) REFERENCES branches(id),
    INDEX idx_locker_branch (branch_id),
    INDEX idx_locker_status (status)
) ENGINE=InnoDB;

-- CUSTOMER KYC PROFILES
CREATE TABLE IF NOT EXISTS customer_profiles (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          BIGINT NOT NULL UNIQUE,
    full_name        VARCHAR(100),
    address          TEXT,
    phone_number     VARCHAR(15),
    bank_account     VARCHAR(20),
    aadhaar_masked   VARCHAR(20),
    aadhaar_pdf_path VARCHAR(500),
    aadhaar_verified BOOLEAN DEFAULT FALSE,
    pan_number       VARCHAR(10),
    pan_pdf_path     VARCHAR(500),
    pan_verified     BOOLEAN DEFAULT FALSE,
    name_match       BOOLEAN DEFAULT FALSE,
    kyc_status       ENUM('PENDING','APPROVED','REJECTED') DEFAULT 'PENDING',
    reviewed_by      BIGINT,
    rejection_reason TEXT,
    nominee_name     VARCHAR(100),
    nominee_email    VARCHAR(150),
    nominee_phone    VARCHAR(15),
    nominee_address  TEXT,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_kyc_status (kyc_status)
) ENGINE=InnoDB;

-- ALLOCATIONS
CREATE TABLE IF NOT EXISTS allocations (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id      BIGINT        NOT NULL,
    locker_id        BIGINT        NOT NULL,
    tenure_months    INT           NOT NULL,
    rent_amount      DECIMAL(10,2) NOT NULL,
    status           ENUM('PENDING','PARTIALLY_APPROVED','APPROVED','REJECTED') DEFAULT 'PENDING',
    officer_1_id     BIGINT,
    officer_2_id     BIGINT,
    requested_at     DATETIME DEFAULT CURRENT_TIMESTAMP,
    approved_at      DATETIME,
    rejection_reason TEXT,
    FOREIGN KEY (customer_id)  REFERENCES users(id),
    FOREIGN KEY (locker_id)    REFERENCES lockers(id),
    FOREIGN KEY (officer_1_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (officer_2_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_alloc_status   (status),
    INDEX idx_alloc_customer (customer_id)
) ENGINE=InnoDB;

-- APPOINTMENTS
CREATE TABLE IF NOT EXISTS appointments (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id            BIGINT       NOT NULL,
    branch_id              BIGINT       NOT NULL,
    locker_id              BIGINT,
    visit_date             DATE         NOT NULL,
    visit_time             TIME         NOT NULL,
    purpose                VARCHAR(100) NOT NULL,
    status                 ENUM('UPCOMING','CONFIRMED','COMPLETED','CANCELLED') DEFAULT 'UPCOMING',
    notes                  TEXT,
    digital_key            VARCHAR(12),
    digital_key_sent       BOOLEAN DEFAULT FALSE,
    digital_key_expires_at DATETIME,
    created_at             DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES users(id),
    FOREIGN KEY (branch_id)   REFERENCES branches(id),
    FOREIGN KEY (locker_id)   REFERENCES lockers(id) ON DELETE SET NULL,
    INDEX idx_appt_date   (visit_date),
    INDEX idx_appt_status (status),
    INDEX idx_appt_branch (branch_id)
) ENGINE=InnoDB;

-- AUDIT LOGS
CREATE TABLE IF NOT EXISTS audit_logs (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT,
    action         VARCHAR(100) NOT NULL,
    entity_type    VARCHAR(50),
    entity_id      BIGINT,
    ip_address     VARCHAR(50),
    previous_state TEXT,
    new_state      TEXT,
    previous_hash  VARCHAR(64),
    current_hash   VARCHAR(64) NOT NULL,
    timestamp      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_user      (user_id),
    INDEX idx_audit_action    (action),
    INDEX idx_audit_timestamp (timestamp)
) ENGINE=InnoDB;

-- OTP TOKENS
CREATE TABLE IF NOT EXISTS otp_tokens (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    otp        VARCHAR(100) NOT NULL,
    type       ENUM('EMAIL','SMS','PASSWORD_RESET') NOT NULL,
    expires_at DATETIME     NOT NULL,
    used       BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_otp_user (user_id)
) ENGINE=InnoDB;

-- PAYMENTS
CREATE TABLE IF NOT EXISTS payments (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    allocation_id  BIGINT        NOT NULL,
    amount         DECIMAL(10,2) NOT NULL,
    payment_date   DATETIME DEFAULT CURRENT_TIMESTAMP,
    status         ENUM('PENDING','SUCCESS','FAILED') DEFAULT 'PENDING',
    transaction_id VARCHAR(100),
    FOREIGN KEY (allocation_id) REFERENCES allocations(id)
) ENGINE=InnoDB;
