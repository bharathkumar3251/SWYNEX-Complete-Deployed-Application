# NalamCamp

NalamCamp helps residents in Tamil Nadu find approved medical camps, check the details on a forwarded poster, and reserve a token for a specific service counter. Before travelling, residents can check when their counter closes, whether walk-ins are allowed, what the visit costs, and what they need to bring.

**[Visit NalamCamp](https://nalamcamp-intern.vercel.app/)**

## How it works

1. An **organizer** creates a camp and adds counters such as eye check-up, dental screening, or general consultation. Each counter has its own hours, fee, instructions, and token limit.
2. A **reviewer** checks the camp details against an official source or organizer confirmation. Once approved, the camp appears in the public directory.
3. A **resident** searches by area, date, or service, selects a counter, and books an available token. The confirmation includes a booking reference, token number, venue, and reporting window.
4. On camp day, a **volunteer** checks in booked visitors and registers walk-ins at eligible counters. Residents can retrieve their booking through **My Token**.

## Poster verification

Residents can upload an English or Tamil camp poster. Tesseract OCR extracts the text, and the resident can correct any reading errors before checking it.

NalamCamp compares the camp name, date, venue, and organizer with approved records. It shows matching details and flags differences. When multiple records match equally well or there is too little information, the result is **Unable to confirm**. The poster can then be submitted for manual review.

A match confirms that the submitted details agree with an approved listing. Image authorship is outside the scope of this check.

## Service-wise booking

Each counter supports online booking, walk-ins, or both. If the eye check-up counter fills up, residents can still book another counter with available places. A token reserves a place within the counter’s reporting window.

- **Last-token handling:** The backend locks the selected counter row in MySQL while it creates the booking and updates availability. Two simultaneous requests cannot take the same final place.
- **Booking retries:** The frontend preserves the request key. If a reservation succeeded before the visitor received a response, retrying with the same key retrieves that booking.
- **Cancellation:** Cancelling an online reservation releases its place back to the counter.
- **Attendance:** Staff can look up a booking by reference or token number. A second check-in is flagged with the existing check-in time.

## Technology stack

| Component | Technology |
|---|---|
| Frontend | HTML, CSS, JavaScript |
| Backend API | Java 17, Spring Boot, Spring MVC |
| Database | MySQL, Spring Data JPA, Hibernate |
| Staff access | Session-based login, role checks, encoded passwords |
| Poster text extraction | Tesseract OCR with English and Tamil language data |
| Build and testing | Maven Wrapper, Spring Boot integration tests |
| Deployment | Vercel for the frontend, Docker and Render for the backend |

## Project structure

The application is inside the `Nalamcamp/` directory.

| Path | Contents |
|---|---|
| `src/main/resources/static/` | Frontend pages, stylesheets, JavaScript, and Vercel API routing |
| `src/main/java/in/nalamcamp/Nalamcamp/` | Spring Boot application and database entities |
| `controller/` | REST endpoints for camps, counters, bookings, poster checks, staff access, and attendance |
| `service/` | Approval workflow, booking rules, poster matching, and check-in logic |
| `repository/` | Database queries and persistence |
| `dto/` | API request and response models |
| `src/test/` | Application and integration tests |
| `Dockerfile` | Backend container build |
| `pom.xml` | Maven dependencies and build configuration |

The `controller/`, `service/`, `repository/`, and `dto/` packages are inside the Java application package.

## Run locally

Install Java 17 and MySQL. For poster uploads, install Tesseract OCR with the `eng` and `tam` language files.

### 1. Clone the project

```powershell
git clone https://github.com/bharathkumar3251/SWYNEX-Complete-Deployed-Application.git
cd .\SWYNEX-Complete-Deployed-Application\Nalamcamp
```

### 2. Create the database

Run this in MySQL Workbench or the MySQL command line.

```sql
CREATE DATABASE IF NOT EXISTS nalamcamp CHARACTER SET utf8mb4;
```

### 3. Configure the application

Set these variables in PowerShell using your local MySQL credentials and Tesseract installation path.

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:mysql://127.0.0.1:3306/nalamcamp"
$env:SPRING_DATASOURCE_USERNAME = "<your-mysql-username>"
$env:SPRING_DATASOURCE_PASSWORD = "<your-mysql-password>"
$env:TESSERACT_PATH = "C:\Program Files\Tesseract-OCR\tesseract.exe"
```

### 4. Start NalamCamp

```powershell
.\mvnw.cmd spring-boot:run
```

Open [localhost:8080](http://localhost:8080/). Spring Boot serves the frontend and API together locally. A fresh database shows an empty directory until an organizer creates a camp and a reviewer approves it. Staff actions require an account with the corresponding role.

## Testing

The integration tests create records. Configure a dedicated local test database before running the suite.

```powershell
.\mvnw.cmd verify
```

The tests cover competing requests for the last token, booking retries, cancellation and capacity release, duplicate check-in prevention, poster discrepancies, and ambiguous poster matches.

## Deployment

The live frontend runs on Vercel. Requests under `/api/` are forwarded to the Spring Boot backend on Render, which reads and writes camp records in the hosted MySQL database. Database credentials are configured in the backend environment.

[Live application](https://nalamcamp-intern.vercel.app/) · [Public camps API](https://nalamcamp-intern.vercel.app/api/camps)
