# AutoTest AI — Web & API Automation Testing Platform

AutoTest AI is a full-stack automated UI and API testing platform built with Spring Boot, Playwright for Java, REST Assured, MySQL, and React (Vite + Tailwind CSS).

---

## 1. System Architecture

The project follows a layered Spring Boot architecture on the backend coupled with a React single-page frontend:

```text
autotest-ai/
├── backend/                  # Java 21 / Spring Boot 3.3.4
│   ├── src/main/java/com/autotest/
│   │   ├── config/           # Security, Web & CORS configuration
│   │   ├── controller/       # REST API controllers (/api/v1/*)
│   │   ├── dto/              # Strongly-typed Request/Response DTOs
│   │   ├── entity/           # JPA Entities (User, TestingProject, TestCase, TestRun, TestResult)
│   │   ├── repository/       # Spring Data JPA Repositories
│   │   ├── service/          # Business logic interfaces
│   │   │   └── impl/         # Service implementations
│   │   ├── execution/        # Test runner engine
│   │   │   ├── ui/           # Playwright UI browser test execution
│   │   │   ├── api/          # REST Assured & HTTP safe executor
│   │   │   └── model/        # Execution action & configuration models
│   │   ├── exception/        # Global exception handling & error responses
│   │   └── util/             # Utility classes & token helpers
│   └── src/main/resources/
│       ├── application.yml   # Production & default profiles
│       └── db/migration/     # Flyway database schema versioning
└── frontend/                 # React 19 + Vite + Tailwind CSS v4
    ├── src/
    │   ├── api/              # Centralized Fetch API client
    │   ├── components/       # Reusable UI components (Buttons, Badges, Modals, Tables)
    │   ├── features/         # Modular feature logic (Auth, Projects, TestCases, Runs)
    │   ├── pages/            # Application routes & views
    │   └── App.jsx
```

---

## 2. Prerequisites

* **Java**: JDK 21+ (Installed: Java 24 / Java 21)
* **Maven**: 3.9+
* **Node.js**: v20+ (Installed: v24.14.1, npm 11.11.0)
* **Database**: MySQL 8.0+

---

## 3. Database & Environment Configuration

Copy the example environment configuration:

```bash
cp .env.example .env
```

Set the values in `.env` in the environment used to start the backend (or configure them in your IDE). Spring Boot does not load `.env` files automatically. Use your own MySQL password and a unique random `APP_JWT_SECRET` of at least 32 characters; the application has no embedded credential defaults.

Ensure MySQL is running on port 3306. By default, the application connects to:
* **Host**: `localhost:3306`
* **Database**: `autotest_db`
* **User**: `root`

Flyway migrations are executed automatically upon backend startup from `backend/src/main/resources/db/migration`.

---

## 4. Startup Instructions

### Backend (Spring Boot)

Navigate to the `backend` directory and run:

```bash
cd backend
mvn spring-boot:run
```

The backend server starts on `http://localhost:8080`.
* **Health Check**: `http://localhost:8080/api/v1/health`
* **Swagger OpenAPI Docs**: `http://localhost:8080/swagger-ui.html`

### Frontend (React + Vite)

Navigate to the `frontend` directory and run:

```bash
cd frontend
npm install
npm run dev
```

The React frontend starts on `http://localhost:5173`. Requests to `/api/*` are automatically proxied to the backend on `http://localhost:8080`.

---

## 5. Running Automated Tests

### Backend Tests
Execute unit and integration tests using:

```bash
cd backend
mvn test
```

### Frontend Tests
Execute frontend build and tests:

```bash
cd frontend
npm run build
```

## 6. Running a Test Case

Open a project in AutoTest AI and click **Run Test** on the test case card. The backend
creates a MySQL test run and result, then executes UI tests with Playwright for Java
or API tests with the existing API executor. The card displays the persisted PASS,
FAIL, or ERROR result and the execution details.

The dashboard and **Execution History** use persisted run and result data. Select a
run to inspect its individual results, failure messages, logs, and any captured
failure screenshots. The details screen can download an HTML report. Screenshot and
report endpoints require authentication and verify that the run/result belongs to
the signed-in user:

* `GET /api/v1/dashboard/metrics`
* `GET /api/v1/test-runs` (supports project, status, and date filters)
* `GET /api/v1/test-runs/{id}/report`
* `GET /api/v1/test-results/{id}/screenshot`

Executed-test totals and pass rate exclude skipped tests; skipped results are
reported separately. New run durations are measured and persisted in milliseconds.
Earlier records retain a zero duration because their timestamp columns do not store
subsecond precision, so the UI reports their duration as unavailable rather than
inventing a value. Flyway applies schema migrations for run-level error counts and
duration at backend startup. Existing installations should restart the backend once
after updating so Flyway can apply pending migrations without changing run records.

For a FeastFlow UI smoke test, start FeastFlow on `http://localhost:5174` and first
install Playwright's Chromium browser from the `backend` directory:

```bash
mvn exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"
```

In AutoTest AI, create a project with base URL `http://localhost:5174`. Add a
**UI Test (Playwright Browser)** named `FeastFlow landing page loads` with these two
actions, in order:

1. `NAVIGATE` with value `/`
2. `ASSERT_TEXT` with selector `h1` and value `Food that`

The assertion checks the actual landing-page heading from FeastFlow; a navigation
or assertion failure is recorded as FAIL or ERROR, never as a pass. Ensure the
FeastFlow frontend is running and reachable from the machine running Spring Boot.

For API tests, use an absolute HTTP(S) endpoint or set the test's **API Base URL**
separately from the project's frontend **Base URL** when entering a relative endpoint.
