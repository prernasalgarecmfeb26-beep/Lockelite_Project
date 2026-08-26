<div align="center">

![LockElite Banner](./banner.png)

# 🔐 LockElite — Bank Locker Management System

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.1-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.3.1-61DAFB?style=for-the-badge&logo=react&logoColor=black)](https://reactjs.org)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://mysql.com)
[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org)
[![JWT](https://img.shields.io/badge/JWT-Auth-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)](https://jwt.io)
[![Razorpay](https://img.shields.io/badge/Razorpay-Payments-072654?style=for-the-badge&logo=razorpay&logoColor=white)](https://razorpay.com)
[![Vite](https://img.shields.io/badge/Vite-5.3-646CFF?style=for-the-badge&logo=vite&logoColor=white)](https://vitejs.dev)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)](LICENSE)

> **A full-stack, microservices-based Bank Locker Management Platform** built as a CDAC Final Project. Enables customers to register, complete digital KYC, book bank lockers, pay rent via Razorpay, and schedule branch visits — while employees manage KYC approvals, dual-authorization locker allocations, and digital access key verification.

</div>

---

## 📋 Table of Contents

- [✨ Features](#-features)
- [🏗️ Architecture](#️-architecture)
- [📁 Project Structure](#-project-structure)
- [🛠️ Tech Stack](#️-tech-stack)
- [⚙️ Prerequisites](#️-prerequisites)
- [🚀 Getting Started](#-getting-started)
  - [1. Database Setup](#1-database-setup)
  - [2. Backend (Port 8080)](#2-backend-port-8080)
  - [3. Payment Service (Port 8081)](#3-payment-service-port-8081)
  - [4. Frontend (Port 5173)](#4-frontend-port-5173)
- [🔑 Environment Variables](#-environment-variables)
- [📡 API Reference](#-api-reference)
- [👥 User Roles & Flow](#-user-roles--flow)
- [🔒 Security Features](#-security-features)
- [⏰ Scheduled Jobs (Cron)](#-scheduled-jobs-cron)
- [🗄️ Database Schema](#️-database-schema)
- [👨‍💻 Contributors](#-contributors)

---

## ✨ Features

### 👤 Customer Portal
| Feature | Description |
|---|---|
| 🔐 **Register & OTP Login** | Email OTP verification + Google OAuth2 sign-in |
| 📋 **Mock DigiLocker KYC** | Aadhaar PDF upload, share-code verification, PAN regex validation, masked storage |
| 🏦 **Branch Finder** | GPS-based nearest branch search using Google Places API + Haversine formula |
| 🗄️ **Locker Booking** | Browse available lockers (SMALL/MEDIUM/LARGE/XLARGE), select tenure, request allocation |
| 💳 **Razorpay Payment** | Create order → Pay → Signature verification |
| 📅 **Visit Scheduling** | Book Mon–Fri, 9AM–5PM branch visits |
| 🔑 **Digital Access Key** | Auto-receive `LK-XXXXXX` key 60 min before visit via email |

### 👨‍💼 Employee Portal
| Feature | Description |
|---|---|
| ✅ **KYC Review** | Approve / Reject KYC with reason — email sent to customer |
| 🔒 **Dual Authorization** | Four-Eyes Principle — 2 different officers must approve every locker allocation |
| 📆 **Appointment Management** | Confirm / Complete / Cancel branch visits |
| 🔑 **Key Verification** | Scan & verify digital access key at branch entry |

### 👑 Admin Panel
| Feature | Description |
|---|---|
| 📊 **Analytics Dashboard** | Revenue, KYC funnel, locker occupancy, user growth |
| 🤖 **AI Anomaly Detection** | Flags suspicious patterns (e.g., KYC approved < 25 min, self-approval attempts) |
| 📜 **Immutable Audit Trail** | SHA-256 hash-chained logs — tamper-proof record of every action |
| 🏦 **Multi-Bank Support** | Manage banks (SBI, HDFC, ICICI, etc.) with custom themes per bank |
| 🏢 **Branch & Locker CRUD** | Full management of branches, lockers, and users |

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│              React + Vite Frontend  (Port 5173)                 │
│        Customer  │  Employee Portal  │  Admin Dashboard         │
└──────────────────┬──────────────────────────────────────────────┘
                   │  REST API + JWT (Bearer Token)
         ┌─────────┴──────────────────────────┐
         ▼                                    ▼
┌─────────────────────────┐      ┌───────────────────────────┐
│  Spring Boot Backend    │─────▶│  Payment Microservice     │
│     Port 8080           │proxy │     Port 8081             │
│                         │      │                           │
│  • JWT + RBAC Security  │      │  • Razorpay Integration   │
│  • KYC Service          │      │  • Order Creation         │
│  • Locker Service       │      │  • Payment Verification   │
│  • Branch Service       │      │  • Payment History        │
│  • Email Service        │      └──────────────┬────────────┘
│  • Cron Jobs            │                     │
│  • Audit Trail          │                     │
└────────────┬────────────┘                     │
             │                                  │
             └──────────────┬───────────────────┘
                            ▼
              ┌──────────────────────────┐
              │      MySQL Database      │
              │   lockelite_db           │
              │                          │
              │  users  │  lockers       │
              │  kyc    │  allocations   │
              │  otp    │  appointments  │
              │  audit  │  payments      │
              └──────────────────────────┘

External Services:
  📍 Google Places API  →  Branch location search
  💰 Razorpay           →  Payment gateway
  📧 Gmail SMTP         →  HTML email notifications
```

---

## 📁 Project Structure

```
lockelite-microservices/
│
├── 📦 lockelite-backend/                  # Main Spring Boot App (Port 8080)
│   ├── src/main/java/com/lockelite/
│   │   ├── LockEliteApplication.java      # Main entry point
│   │   ├── model/                         # JPA Entities
│   │   │   ├── User.java                  # Customer / Employee / Admin
│   │   │   ├── CustomerProfile.java       # KYC data + Aadhaar + PAN
│   │   │   ├── Locker.java                # Locker size, price, status
│   │   │   ├── Allocation.java            # Dual-auth locker booking
│   │   │   ├── Appointment.java           # Branch visit + digital key
│   │   │   ├── Payment.java               # Razorpay payment record
│   │   │   ├── OtpToken.java              # Email / SMS / Reset OTP
│   │   │   ├── AuditLog.java              # SHA-256 hash-chained log
│   │   │   ├── Branch.java                # Bank branch + GPS coords
│   │   │   └── Bank.java                  # Multi-bank theme config
│   │   ├── controller/                    # REST Controllers
│   │   │   ├── AuthController.java        # Register, OTP, Login
│   │   │   ├── CustomerController.java    # KYC, lockers, bookings
│   │   │   ├── EmployeeController.java    # KYC review, dual-auth
│   │   │   ├── AdminController.java       # Analytics, audit logs
│   │   │   ├── BranchController.java      # GPS branch search
│   │   │   ├── PaymentController.java     # Proxy to payment-service
│   │   │   └── InternalController.java    # Service-to-service calls
│   │   ├── service/                       # Business Logic
│   │   │   ├── impl/
│   │   │   │   ├── AuthServiceImpl.java
│   │   │   │   ├── CustomerServiceImpl.java
│   │   │   │   ├── EmployeeServiceImpl.java
│   │   │   │   └── AdminServiceImpl.java
│   │   │   └── EmailService.java          # Rich HTML email sender
│   │   ├── security/
│   │   │   ├── JwtUtil.java               # Token generation + validation
│   │   │   └── JwtAuthFilter.java         # Request filter
│   │   ├── config/
│   │   │   ├── SecurityConfig.java        # RBAC + OAuth2 + CORS
│   │   │   └── DataInitializer.java       # Seed data on startup
│   │   ├── cron/
│   │   │   ├── DigitalKeyCron.java        # Auto-sends access key (every min)
│   │   │   └── RentManagementCron.java    # Rent reminders + penalty (daily)
│   │   ├── audit/
│   │   │   └── AuditLogService.java       # SHA-256 chained audit logs
│   │   ├── dto/                           # Request/Response DTOs
│   │   ├── repository/                    # Spring Data JPA Repositories
│   │   └── exception/                     # Global error handling
│   ├── schema.sql                         # Database schema (run first!)
│   └── pom.xml
│
├── 💳 payment-service/                    # Razorpay Microservice (Port 8081)
│   └── src/main/java/com/lockelite/payment/
│
└── 🌐 lockelite-frontend/                 # React + Vite (Port 5173)
    ├── src/
    │   ├── App.jsx                        # Role-based routing
    │   ├── pages/
    │   │   ├── public/                    # Login, Register, Branch Finder
    │   │   ├── customer/                  # Dashboard, KYC, Lockers, Visits
    │   │   ├── employee/                  # KYC Review, Allocations, Appointments
    │   │   └── admin/                     # Analytics, Audit, User Mgmt
    │   ├── context/                       # Auth context (JWT state)
    │   └── services/                      # Axios API calls
    └── package.json
```

---

## 🛠️ Tech Stack

### Backend
| Technology | Version | Purpose |
|---|---|---|
| Spring Boot | 3.3.1 | Core application framework |
| Spring Security | 6.x | RBAC + OAuth2 authentication |
| Spring Data JPA | 3.x | ORM + database operations |
| JJWT | 0.12.3 | JWT token generation & validation |
| MySQL Connector | 8.x | Database driver |
| JavaMail | — | HTML email sending |
| Lombok | — | Boilerplate reduction |
| Springdoc OpenAPI | 2.5.0 | Swagger UI |
| Spring OAuth2 | — | Google Sign-In |

### Frontend
| Technology | Version | Purpose |
|---|---|---|
| React | 18.3.1 | UI framework |
| Vite | 5.3.1 | Build tool & dev server |
| React Router DOM | 6.24.0 | Client-side routing |
| Axios | 1.7.2 | HTTP requests |
| jwt-decode | 4.0.0 | JWT parsing on client |
| TailwindCSS | 3.4.4 | Utility-first CSS styling |

### External APIs
| Service | Purpose |
|---|---|
| Google Places API | Real bank branch location search |
| Razorpay | Payment gateway (order + verification) |
| Gmail SMTP | Transactional email delivery |

---

## ⚙️ Prerequisites

- ☕ **Java 17+** — [Download](https://adoptium.net)
- 🐬 **MySQL 8.0+** — [Download](https://dev.mysql.com/downloads/)
- 📦 **Maven 3.8+** — [Download](https://maven.apache.org)
- 🟢 **Node.js 18+** — [Download](https://nodejs.org)

---

## 🚀 Getting Started

### 1. Database Setup

```bash
# Login to MySQL
mysql -u root -p

# Run the schema
source /path/to/lockelite-backend/schema.sql

# Verify tables
USE lockelite_db;
SHOW TABLES;
```

Expected tables: `banks, branches, users, customer_profiles, lockers, allocations, appointments, payments, otp_tokens, audit_logs`

---

### 2. Backend (Port 8080)

Create `src/main/resources/application.properties`:

```properties
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/lockelite_db
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD
spring.jpa.hibernate.ddl-auto=update

# JWT
jwt.secret=your_super_secret_key_min_32_characters_long
jwt.expiration-ms=86400000

# Email (Gmail)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your_gmail@gmail.com
spring.mail.password=your_app_password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# App
app.frontend-url=http://localhost:5173
payment.service-url=http://localhost:8081
```

```bash
cd lockelite-backend
./mvnw spring-boot:run
```

✅ Backend: `http://localhost:8080`  
📖 Swagger: `http://localhost:8080/swagger-ui.html`

---

### 3. Payment Service (Port 8081)

```properties
# Add to payment-service application.properties
razorpay.key_id=rzp_test_XXXXXXXXXX
razorpay.key_secret=XXXXXXXXXXXXXXXX
```

```bash
cd payment-service
./mvnw spring-boot:run
```

✅ Payment Service: `http://localhost:8081`

---

### 4. Frontend (Port 5173)

```bash
cd lockelite-frontend
npm install
npm run dev
```

✅ Frontend: `http://localhost:5173`

---

## 🔑 Environment Variables

| Variable | Description |
|---|---|
| `spring.datasource.url` | MySQL JDBC connection string |
| `spring.datasource.username` | MySQL username |
| `spring.datasource.password` | MySQL password |
| `jwt.secret` | JWT signing secret (min 32 chars) |
| `jwt.expiration-ms` | Token validity in ms (86400000 = 24h) |
| `spring.mail.username` | Gmail address for sending emails |
| `spring.mail.password` | Gmail App Password (not regular password) |
| `app.frontend-url` | Frontend origin (for CORS) |
| `payment.service-url` | Razorpay microservice URL |

> **Gmail App Password:** Google Account → Security → 2-Step Verification → App Passwords → Generate for "Mail"

---

## 📡 API Reference

### 🔓 Auth — `/api/auth`
| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/register` | Register new customer |
| `POST` | `/verify-otp` | Verify OTP → Get JWT |
| `POST` | `/resend-otp` | Resend OTP |
| `POST` | `/login` | Login (all roles) |
| `POST` | `/forgot-password` | Password reset email |
| `POST` | `/reset-password` | Reset with token |
| `POST` | `/change-password` | Change password (JWT required) |

### 👤 Customer — `/api/customer` *(JWT required)*
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/dashboard` | Dashboard summary |
| `POST` | `/select-branch` | Set bank & branch |
| `POST` | `/kyc/submit` | Submit KYC (multipart/form-data) |
| `GET` | `/kyc/status` | Check KYC status |
| `GET` | `/lockers/available` | Browse available lockers |
| `POST` | `/bookings/request` | Request locker allocation |
| `GET` | `/bookings/my-allocations` | My locker bookings |
| `POST` | `/bookings/schedule-visit` | Schedule branch visit |
| `GET` | `/bookings/my-visits` | My visit history |
| `POST` | `/payments/create-order` | Create Razorpay order |
| `POST` | `/payments/verify-payment` | Verify payment |
| `GET` | `/payments` | Payment history |

### 👨‍💼 Employee — `/api/employee` *(JWT required)*
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/dashboard` | Branch statistics |
| `GET` | `/kyc/pending` | Pending KYC list |
| `POST` | `/kyc/{id}/approve` | Approve KYC |
| `POST` | `/kyc/{id}/reject` | Reject KYC with reason |
| `GET` | `/allocations/pending` | Pending locker requests |
| `POST` | `/allocations/{id}/approve` | Dual sign-off approve |
| `POST` | `/allocations/{id}/reject` | Reject allocation |
| `GET` | `/appointments` | Branch appointments |
| `POST` | `/appointments/{id}/confirm` | Confirm visit |
| `POST` | `/appointments/{id}/complete` | Mark completed |
| `POST` | `/appointments/verify-key` | Verify digital key at entry |

### 👑 Admin — `/api/admin` *(JWT required)*
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/dashboard` | Full analytics |
| `GET` | `/audit-logs` | Immutable audit trail |
| `GET` | `/anomaly-report` | AI anomaly detection |
| `CRUD` | `/banks` | Bank management |
| `CRUD` | `/branches` | Branch management |
| `CRUD` | `/lockers` | Locker management |

### 📍 Public — `/api/branches`
| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/public?bankName=SBI&latitude=...&longitude=...` | GPS branch search |

---

## 👥 User Roles & Flow

### Customer Journey
```
REGISTER → Verify OTP → Select Branch → Submit KYC
    ↓ (Employee approves KYC)
Browse Lockers → Book Locker → Dual Authorization
    ↓ (Two employees approve)
Pay via Razorpay → Schedule Visit
    ↓ (Cron sends key 60 min before)
Receive Digital Key → Branch Entry ✅
```

### Employee Journey
```
Login → Review Pending KYC → Approve / Reject
     → Dual Sign-Off Allocations → Manage Appointments
     → Verify Digital Keys at Branch Entry
```

### Admin Journey
```
Login → Analytics Dashboard → Anomaly Report
     → Audit Chain Review → Manage Banks / Branches / Lockers
```

---

## 🔒 Security Features

| Feature | Details |
|---|---|
| **JWT + RBAC** | HMAC-SHA256 signed tokens; role enforced at route + method level |
| **BCrypt(12)** | 12 hashing rounds — brute-force resistant |
| **OTP Replay Prevention** | Each OTP has expiry + `used` flag; cannot be reused |
| **Dual Authorization** | Two different officers must approve every locker allocation |
| **Masked Aadhaar** | Stores only `XXXX-XXXX-XXXX`; full number never saved |
| **Google OAuth2** | Customers can sign in with Google |
| **CORS Policy** | Strict allowed-origins; no unauthorized cross-origin |
| **SHA-256 Audit Chain** | Blockchain-like hash chain; tampering is detectable |
| **SecureRandom Keys** | Digital keys use `SecureRandom`, cryptographically strong |
| **Stateless Design** | No server sessions; fully JWT-stateless |

---

## ⏰ Scheduled Jobs (Cron)

| Job | Schedule | Action |
|---|---|---|
| **DigitalKeyCron** | Every minute | Finds appointments in 60 min → emails `LK-XXXXXX` key |
| **RentManagementCron** | Daily midnight | Sends 7/3/1-day reminders → ₹50/day overdue penalty → reclaim flag at 30 days |
| **WeeklyRentSummary** | Every Monday 9AM | Weekly rent summary |

---

## 🗄️ Database Schema

Run `lockelite-backend/schema.sql` to create:

```
lockelite_db
├── banks              → Bank info + theme colors per bank
├── branches           → Branch info + GPS (latitude, longitude)
├── users              → All users (CUSTOMER / EMPLOYEE / ADMIN)
├── customer_profiles  → KYC: Aadhaar masked, PAN, nominee, kyc_status
├── lockers            → Inventory: size, price, status per branch
├── allocations        → Bookings: dual auth (officer1_id, officer2_id)
├── appointments       → Visits: digital_key, digital_key_expires_at
├── payments           → Razorpay: order_id, payment_id, signature
├── otp_tokens         → OTPs: type, expires_at, used flag
└── audit_logs         → SHA-256 chained: previous_hash, current_hash
```

---

## 🧪 Default Test Credentials

After running `DataInitializer.java` seed:

| Role | Email | Password |
|---|---|---|
| Admin | `admin@lockelite.com` | `Admin@123` |
| Employee | `employee@lockelite.com` | `Emp@12345` (change on first login) |
| Customer | Register at `/register` | — |

---

## 🧰 Useful Commands

```bash
# Backend clean build
cd lockelite-backend && ./mvnw clean package -DskipTests

# Run backend jar
java -jar target/lockelite-backend-1.0.0.jar

# Frontend production build
cd lockelite-frontend && npm run build
```

---

## 👨‍💻 Contributors

| Name | Role |
|---|---|
| **Prathmesh Pathari** | Full-Stack Developer — Backend, Frontend, Microservices |

> 🏫 **Institution:** CDAC — Centre for Development of Advanced Computing  
> 📅 **Year:** 2026

---

## 📄 License

Licensed under the **MIT License** — see [LICENSE](LICENSE) for details.

---

<div align="center">

Made with ❤️ by **Prathmesh Pathari** | CDAC Final Project 2026

⭐ **Star this repo if you found it helpful!**

</div>
