# NalamCamp — Backend API & Database

Backend service for **NalamCamp** (SWYNEX Task 2 — Backend API and Database).  
NalamCamp enables residents to verify medical camp posters, discover officially approved health camps and counters, reserve service-specific consultation tokens with strict concurrency safeguards, and support volunteer check-ins and walk-ins.

---

## 1. System Architecture & Features

- **Staff Access Control & Session Management**:
  - Secure session-cookie authentication (`/api/sessions`) with BCrypt password hashing.
  - Role-Based Access Control enforcing `ORGANIZER`, `REVIEWER`, and `VOLUNTEER` permissions.
  - Ownership enforcement: camps are tied to the logged-in staff organizer. Older legacy records with `organizer_id = NULL` are preserved and cannot be claimed by unauthorized organizers.
- **Camp & Service Lifecycle**:
  - State machine: `DRAFT` → `PENDING_REVIEW` → `APPROVED`.
  - Reviewers can approve camps with an official verification source (e.g., government circulars, health department memos) or return submissions for corrections with detailed feedback.
  - Cancelled camps remain transparently visible in public queries with `CANCELLED` status and reject any new token reservations.
- **Validated Counters / Services**:
  - Each camp supports multiple counters (e.g., General Medicine, Eye Care, Dental Screening).
  - Each counter exposes hours, fee, items to bring, booking mode (`ONLINE`, `WALK_IN`, `BOTH`), capacity, and available token count.
  - Capacity reduction protection: prevents editing online capacity below the count of already reserved tokens.
- **High-Concurrency Online Token Booking**:
  - Transactional booking with **MySQL pessimistic row-level locking (`PESSIMISTIC_WRITE`)** on camp counters. Guarantees that when only 1 slot remains, two simultaneous booking requests cannot both succeed.
  - **Idempotency Protection**: Repeated booking attempts with the same `idempotencyKey` and identical details safely replay the original booking reference and token number. Reusing an idempotency key with conflicting details returns `409 Conflict`.
- **Private Booking Lookup & Cancellation**:
  - Token details are retrieved via an unguessable booking reference.
  - Cancellation releases reserved slot capacity exactly once.
- **Volunteer Check-In & Walk-In Desk**:
  - Real-time volunteer check-in prevents duplicate check-ins (`409 Conflict`).
  - Eligible walk-in registrations are recorded without consuming online booking capacity.
- **Tesseract OCR Poster Verification**:
  - Validates JPEG/PNG images up to 10MB.
  - Extracts camp metadata (dates, organizer, venue, title) with English and Tamil language data support.
  - Matching engine compares poster details with approved camps and flags discrepancies (venue, organizer, date) or offers manual review submission.

---

## 2. Prerequisites & Environment Setup

### Required Tools
- **Java**: Java 17 or higher (tested with OpenJDK 27).
- **Build Tool**: Apache Maven (wrapper `mvnw.cmd` included).
- **Database**: MySQL 8.0+ running on `127.0.0.1:3306`.
- **OCR Engine (Optional for OCR testing)**: Tesseract OCR 5.x with English (`eng`) and Tamil (`tam`) traineddata.

### Environment Variables
Configure the following environment variables or use the defaults configured in `src/main/resources/application.properties`:

| Variable | Description | Default Value |
|---|---|---|
| `JAVA_HOME` | Path to JDK 17+ | `C:\Users\kumar\.jdks\openjdk-27.0.1.1` |
| `DB_URL` | MySQL JDBC Connection URL | `jdbc:mysql://127.0.0.1:3306/nalamcamp?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true` |
| `DB_USERNAME` | MySQL Username | `root` |
| `DB_PASSWORD` | MySQL Password | `Bharath@3251` |
| `TESSERACT_PATH` | Path to `tesseract.exe` on Windows | `C:\Program Files\Tesseract-OCR\tesseract.exe` |
| `TESSDATA_PREFIX`| Directory holding `.traineddata` files | `C:\Program Files\Tesseract-OCR\tessdata` |

> [!NOTE]
> If Tesseract OCR is not installed locally on your Windows machine, the rest of the application runs completely normally. When a poster is uploaded without Tesseract configured, the endpoint returns a clear `503 Service Unavailable` with guidance on setting up the executable.

---

## 3. Pre-Seeded Local Accounts

The application automatically seeds necessary organizations and staff accounts on startup without resetting or overwriting existing records:

| Role | Email | Default Password | Organizer Affiliation |
|---|---|---|---|
| **ORGANIZER** | `organizer@nalamcamp.test` | `Organizer@123` | Nalam Volunteers (`VOLUNTEER_GROUP`) |
| **REVIEWER** | `reviewer@nalamcamp.test` | `Reviewer@123` | District Health Oversight |
| **VOLUNTEER** | `volunteer@nalamcamp.test` | `Volunteer@123` | Nalam Volunteers |

---

## 4. How to Run on Windows & IntelliJ IDEA

### Option A: Running from PowerShell / Terminal

1. Open PowerShell and set your JDK path:
   ```powershell
   $env:JAVA_HOME = "C:\Users\kumar\.jdks\openjdk-27.0.1.1"
   $env:Path = "$env:JAVA_HOME\bin;$env:Path"
   ```
2. Navigate to the Maven project directory:
   ```powershell
   cd "intern project\Nalamcamp\Nalamcamp"
   ```
3. Run the Spring Boot application:
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```
   The application will start on `http://localhost:8080`.

### Option B: Running from IntelliJ IDEA

1. Open IntelliJ IDEA → **File** → **Open**.
2. Select the directory containing `pom.xml` (`intern project/Nalamcamp/Nalamcamp`).
3. Set the Project SDK: **File** → **Project Structure** → **Project SDK** → Select JDK 17+.
4. Ensure Maven dependencies are loaded (click the Maven sync button).
5. Locate `src/main/java/in/nalamcamp/Nalamcamp/NalamcampApplication.java`, right-click, and select **Run 'NalamcampApplication'**.

---

## 5. Running Automated Integration Tests

To run the full suite of automated tests (including MySQL concurrency, idempotency, role security, and poster matching tests):

```powershell
$env:JAVA_HOME = "C:\Users\kumar\.jdks\openjdk-27.0.1.1"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
cd "intern project\Nalamcamp\Nalamcamp"
.\mvnw.cmd test
```

### Verified Test Cases:
1. `testCampLifecycle`: End-to-end flow from Draft → Add Counter → Submit → Reviewer Approval with verification source.
2. `testReviewerReturnForCorrection`: Reviewer returns camp for corrections with descriptive rejection reason; camp returns to Draft.
3. `testPublicSearchVisibility`: Unpublished drafts are hidden from public queries; approved camps appear with filtering.
4. `testConcurrentLastTokenBooking`: Two distinct threads race to book the last available token (`capacity = 1`) on the same MySQL counter. Exactly 1 thread succeeds; the second receives `409 Conflict`.
5. `testIdempotentBookingRetry`: Safe retry on repeated request with identical key; rejection with `409 Conflict` when key is reused with different visitor details.
6. `testDuplicateCheckInPrevention`: Volunteer check-in succeeds on the first call; a second check-in attempt on the same token fails with `409 Conflict`.
7. `testBookingCancellationReleasesCapacity`: Resident cancellation releases slot capacity exactly once; duplicate cancellation returns error.
8. `testPosterCheckExactMatch`: OCR poster match against approved camp returns status `MATCHED` and official verification memo.
9. `testPosterCheckDiscrepancy`: Poster match detects and reports discrepancies when venue, date, or organizer deviates from approved records.
10. `testSecurityAndOwnershipEnforcement`: 401 for missing session cookie, 403 for unauthorized roles, and 403 when trying to claim legacy unlinked camps.

---

## 6. Postman Collection & Demo Flow

A complete Postman collection is provided under:
`docs/NalamCamp.postman_collection.json`

### Import into Postman:
1. Open Postman → **Import** → select `docs/NalamCamp.postman_collection.json`.
2. The collection uses variables (`baseUrl = http://localhost:8080`) and automatically stores returned IDs (`campId`, `serviceId1`, `bookingReference`, `tokenNumber`, `posterCheckId`) into collection variables as requests are run.
3. The demo sequence runs in chronological order:

```
[01] Organizer Login                -> POST /api/sessions
[02] Create Camp (Draft)            -> POST /api/camps
[03] Add Counter 1 (General)        -> POST /api/camps/{{campId}}/services
[04] Add Counter 2 (Eye Care)       -> POST /api/camps/{{campId}}/services
[05] Submit Camp for Review         -> POST /api/camps/{{campId}}/submit
[06] Reviewer Login                 -> POST /api/sessions
[07] Approve Camp with Source       -> PATCH /api/camps/{{campId}}/review
[08] Public Search Camps            -> GET /api/camps?area=Gandhipuram&service=Medicine
[09] Public Direct Camp Detail      -> GET /api/camps/{{campId}}
[10] Resident Reserve Token         -> POST /api/services/{{serviceId1}}/bookings
[11] Private Booking Lookup         -> GET /api/bookings/{{bookingReference}}
[12] Volunteer Login                -> POST /api/sessions
[13] Volunteer Check-In             -> PATCH /api/bookings/{{tokenNumber}}/check-in
[14] Duplicate Check-In Test (409)  -> PATCH /api/bookings/{{tokenNumber}}/check-in
[15] Volunteer Walk-In Desk         -> POST /api/services/{{serviceId1}}/walk-ins
[16] View Camp Attendance Totals    -> GET /api/camps/{{campId}}/attendance
[17] Poster OCR Upload              -> POST /api/poster-checks
[18] Poster Match & Verification    -> POST /api/poster-checks/{{posterCheckId}}/match
[19] Resident Cancel Booking        -> DELETE /api/bookings/{{bookingReference}}
[20] Staff Logout                   -> DELETE /api/sessions
```

---

## 7. Complete REST API Reference

### 🔐 Authentication & Session
- `POST /api/sessions` — Log in with email & password (sets HTTP session cookie).
- `GET /api/sessions/me` — Inspect currently logged-in staff user & role.
- `DELETE /api/sessions` — Invalidate session and log out.

### 🏥 Camp Management
- `POST /api/camps` — Create camp draft (derives organizer from logged-in account).
- `GET /api/camps` — Public search (filters by `area`, `date`, `service`; only `APPROVED` and `CANCELLED` visible).
- `GET /api/camps/{id}` — Get camp details with counters (drafts only visible to camp owner/reviewer).
- `PUT /api/camps/{id}` — Update camp details (owner only; drafts only).
- `POST /api/camps/{id}/submit` — Submit camp draft for reviewer review.
- `PATCH /api/camps/{id}/cancel` — Cancel an approved camp (marks as cancelled; rejects new bookings).

### 🩺 Counters / Services
- `POST /api/camps/{id}/services` — Add counter with hours, fee, booking mode, capacity.
- `PUT /api/services/{id}` — Update counter details (validates capacity ≥ reserved count).
- `DELETE /api/services/{id}` — Delete counter (drafts only).

### 🔍 Reviewer Actions
- `GET /api/review-queue` — View list of camps awaiting review (`PENDING_REVIEW`).
- `PATCH /api/camps/{id}/review` — Approve camp (`APPROVE` with verification source) or return for corrections (`RETURN_FOR_CORRECTIONS` with reason).

### 🎟️ Bookings & Attendance
- `POST /api/services/{id}/bookings` — Reserve online token (pessimistic lock, idempotency key).
- `GET /api/bookings/{reference}` — Private booking lookup by reference code.
- `DELETE /api/bookings/{reference}` — Cancel booking and release capacity.
- `PATCH /api/bookings/{token}/check-in` — Volunteer check-in (prevents duplicate check-in).
- `POST /api/services/{id}/walk-ins` — Volunteer walk-in registration (does not consume online slots).
- `GET /api/camps/{id}/attendance` — View total check-ins, online attendance, and walk-in counts.

### 🖼️ Poster OCR & Verification
- `POST /api/poster-checks` — Upload poster JPEG/PNG (up to 10MB); runs Tesseract OCR.
- `POST /api/poster-checks/{id}/match` — Match corrected fields against approved camps.
- `POST /api/poster-checks/{id}/submit-review` — Submit unmatched poster for manual reviewer inspection.
- `PATCH /api/poster-checks/{id}/review` — Reviewer marks poster check as confirmed/rejected.
