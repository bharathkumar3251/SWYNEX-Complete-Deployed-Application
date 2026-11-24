# NalamCamp

NalamCamp is a Spring Boot backend for finding medical camps, checking camp posters against approved records, and reserving tokens for individual services.

A camp may run from 9 a.m. to 1 p.m., while its eye check-up counter closes at 11 a.m. NalamCamp stores the timings, fees, items to bring, and booking limits for each counter separately. Residents can check those details before travelling.

## Internship work

I built NalamCamp as part of my internship at SWYNEX Technologies. Task 1 covered the project architecture and API design. For Task 2, I implemented the REST APIs, connected them to MySQL, and tested the workflow using Postman.

## How it works

1. An organizer creates a draft camp and adds service counters. Each counter can accept online bookings, walk-ins, or both.
2. A reviewer checks the camp details and records a verification source before approving it. The reviewer can also return the camp for corrections.
3. Residents search approved camps by area, date, or service. They can also upload a poster and compare its details with an approved camp.
4. A resident books a counter and receives a token number, a private booking reference, a reporting window, and instructions.
5. On camp day, a volunteer checks in the token or registers a walk-in. The attendance summary shows the camp’s totals.

## Backend stack

| Component | Technology |
| --- | --- |
| Language and framework | Java 17, Spring Boot |
| Database and persistence | MySQL, Spring Data JPA, Hibernate |
| Poster text extraction | Tesseract OCR with English and Tamil language data |
| Build and testing | Maven Wrapper, JUnit, Postman |

Requests pass through the controller, service, and repository layers. Controllers handle HTTP requests, services apply the booking and review rules, and repositories read and write MySQL records. The poster service calls Tesseract when an image is uploaded.

## Booking and poster matching

**Last available token.** If two residents request the final token at the same time, the backend locks the relevant database row while it confirms the booking. One request succeeds and the other receives `409 Conflict`. Retrying a successful request with the same idempotency key and details returns the original booking.

**Poster checks.** Tesseract extracts text from a JPEG or PNG poster. The backend compares the camp name, organizer, date, and venue with approved records. If two camps match equally well, the result is `UNABLE_TO_CONFIRM` and the poster can be submitted for manual review.

**Staff access.** Organizers manage their own camps, reviewers handle approvals, and volunteers record attendance. Staff login uses a session cookie. Residents can search camps and book tokens without a staff account.

## Run locally on Windows

Install a JDK compatible with Java 17 and start MySQL. Open PowerShell in the folder containing `pom.xml` and `mvnw.cmd`.

**1. Create the database**

Run this in MySQL Workbench:

~~~sql
CREATE DATABASE IF NOT EXISTS nalamcamp;
~~~

**2. Configure the database connection**

Set the datasource URL, username, and password for your local MySQL instance. The application configuration is in `src/main/resources/application.properties`. You can supply the connection details through PowerShell for the current session:

~~~powershell
$env:SPRING_DATASOURCE_URL = "jdbc:mysql://127.0.0.1:3306/nalamcamp"
$env:SPRING_DATASOURCE_USERNAME = "your_mysql_username"
$env:SPRING_DATASOURCE_PASSWORD = "your_mysql_password"
~~~

**3. Configure OCR**

Install Tesseract with English (`eng`) and Tamil (`tam`) language data. Adjust the paths below if you chose a different installation folder.

~~~powershell
$env:TESSERACT_PATH = "C:\Program Files\Tesseract-OCR\tesseract.exe"
$env:TESSDATA_PREFIX = "C:\Program Files\Tesseract-OCR\tessdata"
& $env:TESSERACT_PATH --list-langs
~~~

**4. Start the backend**

~~~powershell
.\mvnw.cmd spring-boot:run
~~~

The API runs at `http://localhost:8080`. To try a public request, send `GET http://localhost:8080/api/camps` from Postman. A fresh database returns an empty list until a reviewer approves a camp.

## Main API routes

| Method | Route | Purpose |
| --- | --- | --- |
| POST | `/api/sessions` | Sign in as a staff member |
| GET | `/api/sessions/me` | Check the current staff session |
| DELETE | `/api/sessions` | Sign out |
| POST | `/api/camps` | Create a draft camp |
| POST | `/api/camps/{id}/services` | Add a service counter |
| POST | `/api/camps/{id}/submit` | Submit a camp for review |
| PATCH | `/api/camps/{id}/review` | Approve a camp or return it for corrections |
| GET | `/api/camps` | Search public camps by area, date, or service |
| GET | `/api/camps/{id}` | View a camp and its counters |
| POST | `/api/services/{id}/bookings` | Reserve a token |
| GET | `/api/bookings/{reference}` | Look up a booking |
| DELETE | `/api/bookings/{reference}` | Cancel a booking |
| PATCH | `/api/bookings/{token}/check-in` | Record attendance for a booked token |
| POST | `/api/services/{id}/walk-ins` | Register a walk-in |
| GET | `/api/camps/{id}/attendance` | View attendance totals |
| POST | `/api/poster-checks` | Upload a poster for text extraction |
| POST | `/api/poster-checks/{id}/match` | Compare poster details with approved camps |

## Test with Postman

Import `NalamCamp.postman_collection.json` and set `baseUrl` to `http://localhost:8080`. Use local staff accounts for the organizer, reviewer, and volunteer requests.

Follow one camp through the collection: create it as an organizer, add two counters, submit it, approve it as a reviewer, then search for it as a resident. Book one counter and use the returned `bookingReference` for lookup. For volunteer check-in, use the `tokenNumber`.

## Automated tests

With the test database connection configured, run:

~~~powershell
.\mvnw.cmd verify
~~~

The Task 2 build passed all 12 automated tests on two consecutive runs. The suite covers the camp review lifecycle, access control, last-token booking, booking retries, cancellation, duplicate check-in, and poster matching.
