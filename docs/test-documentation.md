# Test Documentation

## 1. Purpose

This document describes the testing approach for the **Smart Job Search and Salary Prediction Platform**. Testing verifies business logic, API communication, data processing and the stability of the main user flows before the application is deployed to production.

The document focuses on implemented tests, testing tools, test scenarios and expected results.

---

## 2. Testing Types

| Testing type      | Purpose                                                                  | Project examples                                                      |
| ----------------- | ------------------------------------------------------------------------ | --------------------------------------------------------------------- |
| Unit tests        | Test individual functions, classes or components in isolation.          | Backend services, React components and Salary Service logic.          |
| Integration tests | Test cooperation between multiple application layers.                   | Backend API, database, AI flows and salary endpoints.                 |
| End-to-End tests  | Test the main user flows in a browser.                                   | Job search, CV upload, job comparison, analytics and salary prediction. |

---

## 3. Tested Components

| Component      | Role in the system                                                    | Testing type            | Tools                                     |
| -------------- | --------------------------------------------------------------------- | ----------------------- | ----------------------------------------- |
| Frontend       | User interface for searching, viewing and comparing job listings.    | Unit, E2E               | Vitest, React Testing Library, Playwright |
| Backend        | API endpoints, filtering, CV processing and data access.             | Unit, integration tests | JUnit, Spring Boot Test, Maven            |
| Salary Service | Salary prediction and model training.                                | Unit, API tests         | pytest                                    |
| Database       | Storage, retrieval and filtering of job listings.                    | Integration tests       | Spring Boot Test                          |

AI flows are currently tested through backend integration tests and live AI tests.

---

## 4. Unit Tests

Unit tests verify individual parts of the code by using mock objects or prepared test data. They provide fast validation of application logic without starting the complete system.

### 4.1 Backend Unit Tests

| ID        | Tool           | Scenario                    | Coverage                                                        |
| --------- | -------------- | --------------------------- | --------------------------------------------------------------- |
| BE-UT-001 | JUnit, Mockito | Empty search filter         | Returns available jobs without errors.                          |
| BE-UT-002 | JUnit, Mockito | Country filter              | Returns jobs from the selected country.                         |
| BE-UT-003 | JUnit, Mockito | City filter                 | Returns jobs from the selected city.                            |
| BE-UT-004 | JUnit, Mockito | Skill filter                | Returns jobs with matching skills.                              |
| BE-UT-005 | JUnit, Mockito | Combined filters            | Filters and ranks by location, skills, work type and experience. |
| BE-UT-006 | JUnit, Mockito | CV processing               | Converts CV content into a valid search profile.                |
| BE-UT-007 | JUnit, Mockito | Salary request              | Builds the request and processes the Salary Service response.   |
| BE-UT-008 | JUnit          | Allowed AI values           | Rejects unsupported values extracted by AI.                     |

### 4.2 Frontend Unit Tests

| ID        | Tool                           | Scenario          | Coverage                                                   |
| --------- | ------------------------------ | ----------------- | ---------------------------------------------------------- |
| FE-UT-001 | Vitest, RTL                    | Search results    | Displays essential job information.                        |
| FE-UT-002 | Vitest, RTL                    | Salary card       | Displays the predicted range when available.               |
| FE-UT-003 | Vitest, RTL, user-event        | Result sorting    | Sorts by match and posting date.                            |
| FE-UT-004 | Vitest, RTL, user-event        | Job comparison    | Limits comparison to two jobs.                             |
| FE-UT-005 | Vitest, RTL, user-event        | Job details       | Opens, expands and closes the details modal correctly.     |

### 4.3 Salary Service Unit Tests

| ID         | Tool   | Scenario                   | Coverage                                                     |
| ---------- | ------ | -------------------------- | ------------------------------------------------------------ |
| SAL-UT-001 | pytest | Valid prediction request   | Returns a correctly structured salary prediction.            |
| SAL-UT-002 | pytest | Unsupported market         | Rejects predictions outside Austria.                         |
| SAL-UT-003 | pytest | Missing country            | Assumes Austria and reports the assumption.                  |
| SAL-UT-004 | pytest | Missing model              | Returns a controlled unavailable response.                   |
| SAL-UT-005 | pytest | Dataset preparation        | Cleans and joins valid model-training data.                  |
| SAL-UT-006 | pytest | Training pipeline          | Trains and predicts with unseen categories.                  |
| SAL-UT-007 | pytest | Salary range               | Applies ratios and rounds values to EUR 50.                  |
| SAL-UT-008 | pytest | Derived features           | Builds seniority, domain, role and experience features.      |
| SAL-UT-009 | pytest | Market baselines           | Adjusts predictions using grouped salary medians.            |
| SAL-UT-010 | pytest | Insufficient data          | Rejects training with fewer than 100 valid records.          |

### 4.4 Running Unit Tests

```bash
cd backend
mvn clean test
```

```bash
cd frontend
npm ci
npm test
```

```bash
cd salary-service
pytest
```

---

## 5. Integration Tests

Integration tests verify cooperation between connected parts of the system. They cover REST API endpoints, database access and flows between the backend, AI logic and Salary Service.

| ID      | Tool                    | Scenario                 | Coverage                                                   |
| ------- | ----------------------- | ------------------------ | ---------------------------------------------------------- |
| INT-001 | JUnit, Spring Boot Test | Job API                  | Returns valid job responses.                               |
| INT-002 | JUnit, Spring Boot Test | Job filtering            | Connects request DTOs, services and test data.             |
| INT-003 | JUnit, Spring Boot Test | Import integrity         | Verifies stored data and relationships.                    |
| INT-004 | JUnit, Spring Boot Test | CV matching              | Returns ranked jobs from CV criteria.                      |
| INT-005 | JUnit, Spring Boot Test | Analytics                | Verifies analytics endpoints and aggregation.              |
| INT-006 | JUnit, Spring Boot Test | AI job search            | Converts prompts into filters and results.                 |
| INT-007 | JUnit, Spring Boot Test | Live AI                  | Tests real prompt and CV processing when enabled.          |
| INT-008 | JUnit, Spring Boot Test | Salary endpoint          | Processes Salary Service responses through the backend.    |
| INT-009 | JUnit, Spring Boot Test | Partial input            | Returns controlled responses for incomplete requests.      |

### 5.1 Live AI Pre-Deployment Tests

`AiLivePreDeploymentIntegrationTest` contains 16 opt-in tests that call the running AI Service and its configured OpenRouter model. Unlike `AiSearchIntegrationTest`, these tests do not mock the AI client.

The live suite verifies:

- AI Service availability and a valid structured JSON response;
- extraction of country, normalized city, work type and skills from prompts;
- extraction of multiple filters from one prompt;
- job searching with live AI-generated filters;
- controlled handling of unknown skills and vague prompts;
- CV rewriting into profile text;
- extraction of search filters from CV text;
- CV-based job matching;
- controlled handling of CVs without clearly stated skills.

The backend uses the `test` profile and an in-memory H2 database containing prepared integration-test jobs. The AI Service must be running separately and must have a valid `OPENROUTER_API_KEY`.

Required services and variables:

```text
AI Service: http://localhost:8090
AI_SERVICE_URL=http://localhost:8090
RUN_LIVE_AI_TESTS=true
OPENROUTER_API_KEY configured for the AI Service
```

Start the AI Service first. Then run only the live AI test class:

#### Windows (PowerShell)

```powershell
$env:RUN_LIVE_AI_TESTS="true"
$env:AI_SERVICE_URL="http://localhost:8090"
cd backend
mvn -Dtest=AiLivePreDeploymentIntegrationTest test
```

#### macOS/Linux

```bash
export RUN_LIVE_AI_TESTS=true
export AI_SERVICE_URL=http://localhost:8090
cd backend
mvn -Dtest=AiLivePreDeploymentIntegrationTest test
```

These tests are disabled during the standard test run because they depend on an external AI provider, network availability, model behavior and OpenRouter limits. They may consume API requests and can be less deterministic than mocked integration tests.

### 5.2 Running Integration Tests

Backend integration tests use the `*IntegrationTest.java` naming pattern and are therefore included by Maven Surefire when running:

```bash
cd backend
mvn clean test
```

Live AI tests additionally require an environment variable:

```bash
RUN_LIVE_AI_TESTS=true mvn clean test
```

---

## 6. End-to-End Tests

End-to-End tests verify the application from a user's perspective. They run in a browser with **Playwright** and cover the main functionality from user interaction to the displayed result.

| ID      | Tool       | Scenario               | Coverage                                           |
| ------- | ---------- | ---------------------- | -------------------------------------------------- |
| E2E-001 | Playwright | Home page              | Loads the main UI.                                 |
| E2E-002 | Playwright | Prompt search          | Submits a prompt and displays jobs.                |
| E2E-003 | Playwright | Empty prompt           | Prevents or handles an empty search.               |
| E2E-004 | Playwright | CV upload              | Uploads a CV and displays matching jobs.           |
| E2E-005 | Playwright | Job details            | Opens and displays listing details.                |
| E2E-006 | Playwright | Job comparison         | Compares two selected jobs.                        |
| E2E-007 | Playwright | Remove comparison      | Removes a selected job.                            |
| E2E-008 | Playwright | Empty comparison       | Displays the empty state.                          |
| E2E-009 | Playwright | Result sorting         | Changes the job-result order.                      |
| E2E-010 | Playwright | Analytics page         | Displays aggregated market data.                   |
| E2E-011 | Playwright | Result analytics       | Displays analytics for current results.            |
| E2E-012 | Playwright | Salary prediction      | Displays the predicted salary range.               |
| E2E-013 | Playwright | Fast prompt search     | Runs and displays fast-mode search.                |
| E2E-014 | Playwright | Fast CV upload         | Processes a CV in fast mode.                       |

### 6.1 Running E2E Tests

```bash
cd e2e
npm ci
npx playwright test
```

Sequential execution:

```powershell
cd e2e
./run-tests-sequential.ps1
```

---

## 7. Jenkins CI/CD Execution

Jenkins automates test execution and controls the transition of code toward the production version. The pipeline is divided into standard CI stages and additional production stages for the `production` branch.

### 7.1 Standard CI Stages

Standard CI stages run on the `main` branch and other development branches without a production-specific condition.

| Stage                | Command                                      | Coverage                                             |
| -------------------- | -------------------------------------------- | ---------------------------------------------------- |
| Backend Tests        | `cd backend && mvn clean test`               | Runs backend unit and integration tests.             |
| AI Service Tests     | `cd ai-service && mvn clean test`            | Compiles the module and runs its Maven test phase.   |
| Salary Service Tests | Build test image and run `pytest`            | Tests salary prediction and model preparation.       |
| Frontend Tests       | `npm ci && npm test && npm run build`        | Runs frontend unit tests and verifies the build.     |

### 7.2 Production Branch (`production`)

After the standard CI stages, Jenkins runs additional production stages on the `production` branch. These stages verify that the application is ready for deployment and that its main functionality also works in the production environment.

| Stage                | Mechanism                                  | Coverage                                          |
| -------------------- | ------------------------------------------ | ------------------------------------------------- |
| Deploy Production    | Pull and start the `production` branch     | Deploys the latest production version.            |
| Live Smoke Tests     | `curl` home page and `/api/jobs`           | Verifies that the application and API respond.    |
| Production E2E Tests | Playwright against the production URL      | Verifies the main production user flows.          |

Live AI tests are defined in `AiLivePreDeploymentIntegrationTest`, but they run only when the `RUN_LIVE_AI_TESTS=true` environment variable is set.

### 7.3 Deployment Flow

1. Jenkins runs the standard CI stages.
2. On the `production` branch, it retrieves the latest production version.
3. It prepares and starts the application in the production environment.
4. It performs smoke checks against the home page and the `/api/jobs` endpoint.
5. It runs Playwright E2E tests against the production URL.
6. The pipeline completes successfully only if all stages pass.

---

## 8. Success Criteria

Testing is considered successful when unit, integration and End-to-End tests complete without failures, Jenkins completes all required pipeline stages, and the production application passes its smoke checks and main user flows.

---

## 9. Conclusion

The project uses unit, integration and End-to-End tests to verify individual components, connections between modules and the main user-facing functionality.

Jenkins automates the standard code checks and, on the `production` branch, also performs deployment, smoke checks and Playwright E2E tests. This approach provides better control over releases and reduces the risk of errors when deploying a new application version.
