# GlobalPass

A full-stack international payment platform built for Ethiopian users. Users browse a static service catalog or submit custom requests and upload payment proofs, and admins review/approve them — all backed by Supabase (PostgreSQL + Storage).

## Tech Stack

| Layer     | Technology                                       |
|-----------|--------------------------------------------------|
| Frontend  | Next.js 16, React 19, TypeScript, Tailwind CSS   |
| Backend   | Spring Boot 4, Java 21, Spring Security, JPA     |
| Database  | Supabase PostgreSQL (prod) / H2 in-memory (dev)  |
| Storage   | Supabase Storage (prod) / Local filesystem (dev)  |
| Auth      | JWT (jjwt) with role claims, BCrypt password hashing |
| Currency  | EUR (default), with ETB parallel market conversion |
| Tools     | Lombok, MapStruct, Maven, pnpm                    |

## Architecture

```
globalpass/
├── back-end/          Spring Boot REST API (port 8080)
│   ├── auth/          JWT auth (signup, login, forgot/reset password, email verification)
│   ├── bookings/      Auto-created booking summaries
│   ├── config/        Security, CORS, ServiceCatalog, H2 console
│   ├── contact/       Contact form messages
│   ├── exception/     Custom exceptions and global error handler
│   ├── payments/      Payment upload, cancel, re-upload, status management, file storage
│   └── users/         User CRUD, profile, password change, account deletion
├── front-end/         Next.js App Router (port 3000)
│   ├── app/(main)/    Pages: home, booking, payments, admin, profile, contact, support
│   ├── app/api/       Proxy route to backend, exchange rates
│   ├── components/    Navbar, Footer, AuthGuard, Toast, Skeleton, ThemeProvider
│   └── lib/           API client, auth context, static services, types
├── infra/             Infrastructure docs
└── docker-compose.yml
```

## Key Design Decisions

- **Static service catalog** — The 38 services (tuition, Netflix, flights, etc.) are defined as constants in both frontend (`lib/services.ts`) and backend (`ServiceCatalog.java`). No DB queries needed to browse services.
- **EUR as default currency** — Services are priced in EUR. ETB conversion uses parallel market rates from ethioblackmarket.com API.
- **Bookings auto-created** — When a user uploads payment proof, a booking summary row is automatically created in the `bookings` table with user info, service name, amount, and status.
- **Denormalized tables** — Both `payments` and `bookings` tables store user name/email and service name directly. Profile updates sync denormalized data automatically.
- **File naming** — Uploaded files are named `UserName_ServiceName_timestamp.ext` for easy identification in Supabase Storage.
- **Role-based access** — JWT tokens include user role. Admin endpoints are protected with `@PreAuthorize`. All user-specific endpoints verify ownership.
- **Token invalidation** — Changing password sets `passwordChangedAt`, and JwtFilter rejects tokens issued before that timestamp.

## Database Tables

| Table              | Purpose                                          |
|--------------------|--------------------------------------------------|
| `users`            | Registered users (name, email, password, role, emailVerified, passwordChangedAt) |
| `bookings`         | High-level booking summary (who, what, how much) |
| `payments`         | Payment proof details (file, amount, status, admin note) |
| `contact_messages` | Contact form submissions                          |

## Getting Started

### Prerequisites
- Java 21+
- Node.js 20+
- pnpm

### Backend (dev profile — no env vars needed)
```bash
cd back-end
# Edit application.yaml: spring.profiles.active: dev
mvn clean spring-boot:run
```

### Backend (prod profile — requires env vars)
```powershell
$env:JWT_SECRET="your-256-bit-secret"
$env:DATABASE_URL="jdbc:postgresql://your-host:6543/postgres?prepareThreshold=0"
$env:DATABASE_USERNAME="postgres.your-project-ref"
$env:DATABASE_PASSWORD="your-db-password"
$env:SUPABASE_URL="https://your-project.supabase.co"
$env:SUPABASE_KEY="your-supabase-service-role-key"
$env:SUPABASE_BUCKET="payments"
$env:CORS_ORIGINS="http://localhost:3000"
mvn spring-boot:run
```
Runs at http://localhost:8080

### Frontend
```bash
cd front-end
pnpm install
pnpm dev
```
Runs at http://localhost:3000

### Docker (full stack)
```bash
docker compose up --build
```

## Profiles

| Profile | Database           | Storage          | Config                    |
|---------|--------------------|------------------|---------------------------|
| `dev`   | H2 in-memory       | Local filesystem | `application-dev.yaml`    |
| `prod`  | Supabase PostgreSQL | Supabase Storage | `application-prod.yaml`   |

Set in `application.yaml`: `spring.profiles.active: prod` or `dev`

## Environment Variables (prod)

| Variable           | Required | Description                          |
|--------------------|----------|--------------------------------------|
| `JWT_SECRET`       | Yes      | 256-bit secret for signing JWT tokens |
| `DATABASE_URL`     | Yes      | Supabase PostgreSQL JDBC URL          |
| `DATABASE_USERNAME`| Yes      | Database username                     |
| `DATABASE_PASSWORD`| Yes      | Database password                     |
| `SUPABASE_URL`     | Yes      | Supabase project URL                  |
| `SUPABASE_KEY`     | Yes      | Supabase service role key             |
| `SUPABASE_BUCKET`  | No       | Storage bucket name (default: payments) |
| `CORS_ORIGINS`     | No       | Allowed origins (default: localhost:3000) |
| `JWT_EXPIRATION`   | No       | Token expiry in ms (default: 86400000 = 24h) |

## User Flow
1. Register / Login → JWT token issued (with role claim)
2. Browse static service catalog → pick a service → enter amount in EUR
3. Upload payment proof (screenshot/PDF) with optional note
4. View booking status in "My Bookings" (with cancel option for pending)
5. Contact support via form or WhatsApp/Telegram
6. Manage profile, change password, delete account

## Admin Flow
1. Login → redirected to admin bookings dashboard
2. **Bookings tab** — table view of all bookings (user, service, amount, status, payment note)
3. **Payments tab** — review uploaded proofs, approve/reject/revert with admin note
4. **Messages tab** — view all contact form submissions
5. **Users tab** — view all registered users with roles
6. Status syncs between `payments` and `bookings` tables automatically

## Security Features
- JWT tokens include user role (USER/ADMIN)
- `@PreAuthorize` on all admin endpoints
- Ownership verification on all user-specific endpoints
- Password change invalidates existing tokens
- Duplicate payment prevention (one pending per user per service)
- File type and size validation
- Honeypot spam protection on contact form
- No secrets in source code — all via environment variables

## Services (38 total)
- Study Abroad: tuition, deposits, application fees, exam registration, student housing
- Digital Subscriptions: Netflix, Spotify, Adobe, AI tools, domains, online courses
- Freelancer & Remote: Upwork, JetBrains, certifications, LinkedIn Premium
- Gaming & Entertainment: PlayStation, Steam, in-game currency, YouTube Premium
- Online Shopping: Amazon, AliExpress, electronics, books, shipping
- Relocation & Setup: accommodation, transport cards, SIM/onboarding
- Travel & Hotels: flights, hotels, travel insurance, car rental, events
- Other: custom payments, donations, recurring payments

## Admin Setup
Register a user, then promote via Supabase SQL Editor:
```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'your-email@example.com';
```
Then log out and log back in to get a new JWT with the ADMIN role.

## Database Migration (if upgrading from earlier version)
Run in Supabase SQL Editor:
```sql
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified BOOLEAN DEFAULT false;
ALTER TABLE users ADD COLUMN IF NOT EXISTS verification_token VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS reset_token VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS reset_token_expiry TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_changed_at TIMESTAMP;
```
