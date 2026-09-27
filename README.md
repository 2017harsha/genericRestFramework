# REST Assured API Generic Test Framework

A reusable base for API test automation, built with **Java 21 · REST Assured 5 · JUnit 5 · Maven · Allure**. It includes:
- a **parameterised Jenkins pipeline** (qa / dev / prod)
- the **prod password read at runtime from AWS SSM Parameter Store**
- **automatic retry** of failing tests (1 run + 2 retries): a test is FAILED only if every attempt fails, and each attempt is logged
- a complete sample test suite against the public **[Restful-Booker](https://restful-booker.herokuapp.com)** API: about 60 positive and negative tests

Copy this project for each new API project. Replace the sample `booking` package with your own service objects and tests. You don't need to change the framework core (`com.qa.framework`).

---

## Contents

1. [Quick start](#1-quick-start)
2. [Tech stack](#2-tech-stack)
3. [Project layout](#3-project-layout)
4. [Running tests & the Allure report](#4-running-tests--the-allure-report)
5. [Configuration](#5-configuration)
6. [Prod password in AWS Parameter Store](#6-prod-password-in-aws-parameter-store)
7. [Jenkins setup](#7-jenkins-setup)
8. [Sample test suite – Restful-Booker](#8-sample-test-suite--restful-booker)
9. [Adding tests for a new API](#9-adding-tests-for-a-new-api)
10. [Troubleshooting](#10-troubleshooting)

---

## 1. Quick start

Prerequisites: **JDK 21+** and **Maven 3.8+** on the `PATH`. Check with `java -version` and `mvn -v`.

```bash
cd RestAPI
mvn clean test          # runs every test against the qa environment
mvn allure:serve        # builds the Allure report and opens it in your browser
```

The first run downloads the dependencies and the Allure command-line tool, which takes a few minutes. Later runs are quick.

## 2. Tech stack

| Component | Version | Purpose |
|---|---|---|
| Java | 21 | language level (`maven.compiler.release`) |
| Maven | 3.8+ | build, `-Denv` / `-Dgroups` switches |
| REST Assured | 5.5.1 | HTTP client + fluent assertions |
| JUnit Jupiter | 5.11.4 | test runner, tags, parameterised & nested tests |
| Allure (allure-java) | 2.29.1 | `allure-junit5` + `allure-rest-assured` result writers |
| Allure report CLI | 2.46.1 | report generator used by `mvn allure:serve` (own release line) |
| AspectJ weaver | 1.9.22.1 | makes Allure `@Step` annotations work |
| Jackson | 2.18.2 | JSON ⇄ Java (records supported) |
| AssertJ | 3.27.3 | readable assertions |
| JSON Schema Validator | 5.5.1 | response contract checks |
| AWS SDK v2 (SSM) | 2.30.0 | reads secrets from Parameter Store |
| Maven Surefire | 3.5.2 | runs the tests; `rerunFailingTestsCount` = automatic retry |
| Allure Maven plugin | 2.15.2 | `mvn allure:serve` / `allure:report` |
| SLF4J (simple) | 2.0.16 | logging, including the `RetryLog` retry messages |

All versions are properties at the top of `pom.xml`.

## 3. Project layout

```
RestAPI
├── .gitignore                      ignores target/, .allure/, IDE files, local secrets
├── pom.xml                         dependencies, surefire + AspectJ (Allure), -Denv / -Dgroups switches
├── Jenkinsfile                     parameterised pipeline (ENVIRONMENT, TEST_TAGS, …)
├── jenkins/api-test-job-dsl.groovy optional: create one job per project automatically
├── aws/
│   ├── iam-policy-ssm-read.json    least-privilege policy for the Jenkins agent / user
│   ├── create-prod-secret.sh       stores the prod password as a SecureString
│   └── create-prod-secret.ps1      same, for Windows
└── src
    ├── main/java/com/qa/framework          ← reusable core (don't edit per project)
    │   ├── config/     ConfigManager, Environment, ConfigKeys
    │   ├── secrets/    SecretResolver + AWS Parameter Store / env-var providers
    │   ├── auth/       none | basic | bearer | token (login) strategies
    │   ├── client/     RequestSpecFactory (shared spec), BaseService (HTTP helpers)
    │   ├── assertions/ ResponseValidator (fluent, Allure-reported checks)
    │   ├── base/       BaseApiTest, @DestructiveTest (auto-skipped on prod)
    │   ├── listeners/  AllureEnvironmentWriter (Allure environment widget),
    │   │               RetryLoggingExtension (logs every retry attempt)
    │   └── utils/      JsonUtils, RandomData
    └── test
        ├── java/com/qa/tests                 ← YOUR tests (sample: Restful-Booker)
        │   ├── booking/  HealthCheckApiTest, AuthApiTest, BookingApiTest,
        │   │             BookingNegativeApiTest, BookingTestBase (clean-up)
        │   ├── services/ BookingService, AuthService, HealthCheckService
        │   ├── models/booking/  Booking, BookingDates, CreateBookingResponse, AuthRequest/Response …
        │   └── data/     BookingTestData (test-data builder)
        ├── java/com/qa/framework/config       offline self-tests of the core (@Tag framework)
        └── resources
            ├── config/common.properties       shared settings
            ├── config/{qa,dev,prod}.properties
            ├── schemas/   booking-schema.json, create-booking-response-schema.json, booking-ids-schema.json
            ├── testdata/  booking.json
            └── allure.properties, junit-platform.properties, simplelogger.properties
```

## 4. Running tests & the Allure report

### Run tests

```bash
mvn clean test                                      # qa (default), all tests
mvn clean test -Denv=dev                            # dev config (logs every request); or -Pdev
mvn clean test -Denv=prod                           # prod – needs AWS access, see section 6

mvn clean test -Dgroups=smoke                       # quick checks only
mvn clean test -Dgroups=regression                  # full regression
mvn clean test -Dgroups=negative                    # negative scenarios only
mvn clean test -Dgroups=security                    # token / 403 checks
mvn clean test -Dgroups="regression & !negative"    # positive regression
mvn clean test -DexcludedGroups=destructive         # read-only tests only
mvn clean test -Dgroups=framework                   # offline framework self-tests (no network)

mvn clean test -Dtest=BookingApiTest                # one class
mvn clean test -Dtest=BookingApiTest#createBooking  # one test
mvn clean test -Djunit.jupiter.execution.parallel.enabled=true   # parallel
```

Always use `clean` after renaming or deleting test classes. Otherwise old compiled classes in `target` still run.

### Automatic retry of failing tests (1 run + 2 retries)

A failing test is re-run automatically before it is reported as failed. This uses Maven Surefire's `rerunFailingTestsCount`, configured by the `retry.count` property in `pom.xml` (default **2**):

| Attempt 1 | Attempt 2 | Attempt 3 | Final result |
|---|---|---|---|
| ✅ pass | – | – | **PASSED** |
| ❌ fail | ✅ pass | – | **FLAKY**, counted as passed, build stays green |
| ❌ fail | ❌ fail | ✅ pass | **FLAKY**, counted as passed, build stays green |
| ❌ fail | ❌ fail | ❌ fail | **FAILED**, only now does the test fail |

- Only the failing test is re-run, not the whole class. For parameterised tests only the failing invocation is re-run.
- `@AfterEach` runs after every attempt, so bookings created by a failed attempt are still cleaned up.
- The console summary lists `Flakes:` separately from `Failures:`.
- Every attempt is logged by `RetryLoggingExtension` (logger name `RetryLog`), so you can see in the console / Jenkins log how many times a test was retried before it failed or passed:

```
WARN  RetryLog - [RETRY] BookingApiTest > POST /booking creates a booking and echoes it back | attempt 1/3 FAILED | will retry (2 retries left) | cause: AssertionError: 1 expectation failed. Expected status code <200> but was <503>.
INFO  RetryLog - [RETRY] BookingApiTest > POST /booking creates a booking and echoes it back | attempt 2/3 started (retry 1 of 2)
WARN  RetryLog - [RETRY] BookingApiTest > POST /booking creates a booking and echoes it back | PASSED on attempt 2/3 after 1 failed attempt(s) -> FLAKY

WARN  RetryLog - [RETRY] BookingNegativeApiTest > UnknownIds > ... | attempt 1/3 FAILED | will retry (2 retries left) | cause: ...
INFO  RetryLog - [RETRY] BookingNegativeApiTest > UnknownIds > ... | attempt 2/3 started (retry 1 of 2)
WARN  RetryLog - [RETRY] BookingNegativeApiTest > UnknownIds > ... | attempt 2/3 FAILED | will retry (1 retry left) | cause: ...
INFO  RetryLog - [RETRY] BookingNegativeApiTest > UnknownIds > ... | attempt 3/3 started (retry 2 of 2)
ERROR RetryLog - [RETRY] BookingNegativeApiTest > UnknownIds > ... | FAILED on all 3 attempts (retried 2 times) -> marked FAILED | cause: ...
```

  Search the log for `[RETRY]` to find them. Tests that pass first time log nothing extra. In Allure, each attempt also has an **"Attempt n of 3"** step.
- In Allure, the last attempt is the test's result and earlier attempts are in the test's **Retries** tab. Tests that passed on a retry are marked **flaky**.
- Change the number per run:

```bash
mvn clean test -Dretry.count=0     # no retries – see the real first-attempt result (use while debugging)
mvn clean test -Dretry.count=3     # 1 run + 3 retries
```

A retry hides *intermittent* problems (network, shared test data, a slow free server). It never turns a real bug into a pass: a test that fails every time is still reported as **FAILED**. Look at flaky tests regularly. They often point to timing or test-data issues worth fixing.

### View the Allure report

```bash
mvn allure:serve     # generate + open in the browser (Ctrl+C to stop)
mvn allure:report    # static report → target/site/allure-maven-plugin/index.html
```

Run the report command **after** the tests; it reads `target/allure-results`. The static report must be served over HTTP (it shows blank when double-clicked), so use `allure:serve` for local viewing.

What the report shows:

- **Overview:** pass/fail/skip totals and an *Environment* widget (env, base URL, auth type, Java, Jenkins build).
- **Behaviors:** tests grouped as *Restful-Booker → Authentication / Bookings / Health check → story* (Create booking, Security, …).
- **Test details:** every service call is an Allure step, e.g. "Create booking" or "Delete booking 123". Each step has the full **request and response** attached (URL, headers, body). Sensitive headers are masked in the console log.
- **Skipped:** known-defect tests show as skipped, with their issue id.
- **Retries:** a retried test has a **Retries** tab listing the earlier attempts, and an "Attempt n of 3" step. Tests that passed only on a retry are marked **flaky**.

### Override any setting from the command line

```bash
mvn test -Dbase.url=https://my-feature-branch.qa.example.com -Dlog.mode=all
# or as environment variables: BASE_URL=... LOG_MODE=all mvn test
```

## 5. Configuration

Lookup order (first match wins): **`-Dkey`** → **ENV_VAR** (dots → underscores, upper case) → **`config/<env>.properties`** → **`config/common.properties`**.

| Key | Meaning |
|---|---|
| `base.url`, `base.path` | Main API under test |
| `auth.type` | `none` · `basic` · `bearer` · `token` |
| `auth.username`, `auth.password` | For basic / token |
| `auth.token` | For bearer (static token / API key) |
| `auth.token.url`, `auth.token.json.path` | For token: login endpoint (absolute, or relative to `base.url`) + where the token is in the response |
| `auth.header.name`, `auth.header.prefix` | Default `Authorization` / `Bearer `. Restful-Booker uses `Cookie` / `token=`; an API key would be `x-api-key` with no prefix |
| `services.<name>.base.url` | Extra APIs tested from the same project – `RequestSpecFactory.forService("<name>")` |
| `http.connect.timeout.ms`, `http.read.timeout.ms` | Timeouts |
| `http.relaxed.https` | Trust self-signed certs (non-prod only) |
| `log.mode` | `all` · `failures` · `none` — `Authorization`, cookies and `x-api-key` are always masked |
| `sla.response.ms` | Response-time limit used by `respondedWithin(...)` checks |
| `aws.region`, `aws.profile` | For Parameter Store lookups |
| `tests.allow.destructive` | Defaults to `false` on prod, `true` elsewhere |
| `retry.count` | Maven property (`pom.xml`, default `2`): extra attempts for a failing test. Override with `-Dretry.count=0` … `3` |

**Current setup (Restful-Booker):** `common.properties` sets token auth. The framework POSTs `admin` and the password to `/auth` once, then sends `Cookie: token=<token>` on every request.

| Env | `base.url` | Password source |
|---|---|---|
| qa | `https://restful-booker.herokuapp.com` | `password123` (public demo credentials) |
| dev | same, with `log.mode=all` | `password123` |
| prod | same | `ssm:/api-test-framework/prod/api-password` |

**Default headers:** every request sends `Content-Type: application/json` and **`Accept: application/json`** (exactly that value; see Troubleshooting).

### Secret references

A value can point to a secret instead of containing it. Secrets are resolved **lazily**, so a qa run never touches AWS just because prod uses `ssm:`. Each secret is fetched once per run.

```properties
auth.password=ssm:/api-test-framework/prod/api-password   # AWS SSM Parameter Store (SecureString decrypted)
auth.password=env:API_PASSWORD                             # env var, e.g. bound by Jenkins credentials
```

You can add other back-ends (Vault, Secrets Manager) by implementing `SecretProvider` and calling `SecretResolver.register(...)`.

## 6. Prod password in AWS Parameter Store

**One-time setup (AWS admin):**

1. Store the password as an encrypted **SecureString**:
   ```bash
   ./aws/create-prod-secret.sh            # Linux/macOS/Git Bash
   ./aws/create-prod-secret.ps1           # Windows PowerShell
   # or directly:
   aws ssm put-parameter --region ap-south-1 --type SecureString \
       --name /api-test-framework/prod/api-password --value '<password>' --overwrite
   ```
2. Give Jenkins **read-only** access with `aws/iam-policy-ssm-read.json`. Replace `<AWS_ACCOUNT_ID>` and the KMS key. For the default `aws/ssm` key you can drop the KMS statement if your account policy already allows it. Attach the policy to **one** of:
   - **(Recommended)** the IAM role of the Jenkins agent (EC2 instance profile / ECS task role). No keys are stored anywhere. Set `AWS_CREDENTIALS_ID = ''` in the `Jenkinsfile`.
   - an IAM user whose access key is saved in Jenkins as an **AWS Credentials** entry with ID `aws-api-tests-ssm-reader` (CloudBees AWS Credentials plugin).
3. In `config/prod.properties`, set `auth.password=ssm:<your parameter name>` and `aws.region`.

**How it flows at runtime:** Jenkins exposes only AWS credentials (or the agent's role supplies them). The framework calls `ssm:GetParameter --with-decryption` via the AWS SDK default credential chain and keeps the password in memory. The password is never written to the repo, the Jenkins config, console logs or the Allure report. The valid-login test is skipped on prod for the same reason.

**Running prod locally** (with your own AWS access): `aws sso login --profile my-profile`, then
`mvn test -Denv=prod -Daws.profile=my-profile -Dgroups=smoke`.

## 7. Jenkins setup

**Plugins:** Pipeline, Git, JUnit, **Allure Jenkins Plugin**, Credentials Binding, Timestamper. Also **CloudBees AWS Credentials** if you use access keys instead of an agent IAM role. Optional: Job DSL, Parameterized Scheduler.

**Global tools** (*Manage Jenkins → Tools*). The names must match the `Jenkinsfile`, or you can edit the file:
- JDK → `JDK21`
- Maven → `Maven3`
- Allure Commandline → `Allure`

**Create the job (manual):**
1. *New Item* → **Pipeline** → name, e.g. `restful-booker-api-tests`.
2. *Pipeline* → **Pipeline script from SCM** → Git → repo URL + credentials → branch `*/main` → Script Path `Jenkinsfile`.
3. Save → **Build Now** once. The first build registers the parameters from the `Jenkinsfile`. After that the job shows **Build with Parameters**:

| Parameter | Values |
|---|---|
| `ENVIRONMENT` | `qa` · `dev` · `prod` |
| `TEST_TAGS` | e.g. `smoke`, `regression`, `negative`, `smoke & !security` (empty = all; **prod defaults to `smoke`**) |
| `EXCLUDE_TAGS` | comma-separated; `destructive` is **always** excluded on prod |
| `TEST_CLASS` | optional `-Dtest` filter, e.g. `BookingApiTest` or `BookingApiTest#createBooking` |
| `PARALLEL` | JUnit 5 parallel execution |
| `RETRY_COUNT` | `2` (default) · `1` · `0` · `3`: extra attempts for a failing test. The test is marked FAILED only if every attempt fails; passing on a retry shows as flaky |

**Or with Job DSL:** add your repos to `jenkins/api-test-job-dsl.groovy` and run it from a seed job. It creates one parameterised job per project, with parameters available from build #1 and an optional nightly qa regression trigger.

**Results:** Failed tests (all 3 attempts failed) turn the build **UNSTABLE** (not FAILED). Flaky tests don't. JUnit trends and the **Allure Report** link appear on every build.

**qa/dev credentials (optional):** add a *Username with password* credential. Put its ID in `QA_DEV_CREDENTIALS` in the `Jenkinsfile`. Then use `auth.username=env:API_USERNAME` / `auth.password=env:API_PASSWORD` in `qa.properties` or `dev.properties`.

## 8. Sample test suite – Restful-Booker

[Restful-Booker](https://restful-booker.herokuapp.com/apidoc/index.html) is a public practice API. It really stores data, uses token auth and has **deliberate bugs**, so it is good for realistic scenarios. Its data is reset every few minutes and other people use it too, so every test creates its own bookings with unique names. `BookingTestBase` deletes them after each test.

### Endpoints covered

| Method | Endpoint | Auth | Notes |
|---|---|---|---|
| GET | `/ping` | – | health check, returns **201** |
| POST | `/auth` | – | returns `{"token":…}`, or **200** `{"reason":"Bad credentials"}` |
| GET | `/booking`, `/booking?firstname=&lastname=` | – | list / search ids |
| GET | `/booking/{id}` | – | needs `Accept: application/json` exactly |
| POST | `/booking` | – | create |
| PUT | `/booking/{id}` | token or Basic | full update |
| PATCH | `/booking/{id}` | token or Basic | partial update |
| DELETE | `/booking/{id}` | token or Basic | returns **201** |

### Test classes

| Class | Tests | Positive | Negative |
|---|---|---|---|
| `HealthCheckApiTest` | 1 | API is up, within the SLA | – |
| `AuthApiTest` | 9 | valid login returns a token | 7 bad-credential cases (wrong / case-changed username and password, empty, trailing space, SQL injection) and missing fields: **no token issued** |
| `BookingApiTest` | 20 | list and get (prod-safe smoke), create with schema check, create from JSON file, 8 data variations (deposit not paid, optional field omitted, 1 night, 30 nights, large price, apostrophes/hyphens, non-ASCII, long text), read-after-write, search (found / empty), PUT with token, PATCH changes only the given fields, PUT with Basic auth, delete then 404, full customer journey (book → view → extend → add request → cancel) | – |
| `BookingNegativeApiTest` | 26 | – | unknown / malformed ids (404), decimal id like `7.5` treated as `7` (200 – quirk), PUT/DELETE unknown id (405), delete twice, PUT/PATCH/DELETE **without token** and **with an invalid token** (403, and data unchanged), each mandatory field missing, empty body, malformed JSON (400), unsupported `Accept` (418), 2 known defects |
| `ConfigManagerTest` | 5 | framework self-tests (offline) | unknown environment fails fast |

### Tags

| Tag | Meaning |
|---|---|
| `smoke` | fast, business-critical, safe on prod |
| `regression` | full suite |
| `negative` | invalid input, missing auth, unknown resources |
| `security` | authorisation checks (403 without / with bad token) |
| `destructive` | added by `@DestructiveTest`: writes data, **auto-skipped on prod** |
| `framework` | offline checks of the framework core |

### How API quirks and bugs are handled

- **Quirks** (non-standard but intended: 201 on delete, 200 for bad login, 500 instead of 400 for missing fields, 405 for an unknown id, 418 for an unsupported Accept, `/booking/7.5` returning booking 7): the test checks the **actual** behaviour and says so in its name and `@Description`, so a change in behaviour is noticed.
- **Real defects** (check-out before check-in accepted, negative price accepted): written with the **correct** expectation, marked `@Disabled("Known defect RB-10x …")` and linked with `@Issue`. Remove `@Disabled` once the bug is fixed.

## 9. Adding tests for a new API

1. **Config:** set `base.url` and `auth.*` in `config/common.properties` and `config/{qa,dev,prod}.properties`.
2. **Model:** add Java records (or POJOs) in `models/`, e.g. `record Order(String id, String status) {}`.
3. **Service object:** add one class per resource:
   ```java
   public class OrdersService extends BaseService {
       public OrdersService() { super("/orders"); }                              // authenticated
       public OrdersService(RequestSpecification spec) { super("/orders", spec); } // custom / no auth

       @Step("Get order {id}")
       public Response getOrder(String id) { return get("/{id}", Map.of("id", id)); }

       @Step("Create order")
       public Response create(Object order) { return post("", order); }
   }
   ```
4. **Test data builder:** unique values, sensible defaults, e.g. `OrderTestData.anOrder().withQuantity(3).build()` (see `BookingTestData`).
5. **Tests:** extend `BaseApiTest`, or a base class like `BookingTestBase` that cleans up created data:
   ```java
   @Epic("Orders") @Feature("Create order")
   class OrdersApiTest extends BaseApiTest {
       private final OrdersService orders = new OrdersService();
       private final OrdersService noAuth = new OrdersService(RequestSpecFactory.unauthenticated());

       @Test @Tag("smoke")
       void getOrder() {
           ResponseValidator.of(orders.getOrder("A-1"))
               .statusCode(200)
               .matchesSchema("schemas/order-schema.json")
               .respondedWithin(2000)
               .body("status", equalTo("CONFIRMED"));
       }

       @Test @Tag("regression") @Tag("negative") @DestructiveTest   // skipped on prod automatically
       void createOrderWithoutTokenIsRejected() {
           ResponseValidator.of(noAuth.create(anOrder().build())).statusCode(401);
       }
   }
   ```
6. **Other auth in one test:** `RequestSpecFactory.build(new BasicAuthStrategy("user2", () -> "..."))`, or an invalid token with `new HeaderTokenAuthStrategy("Cookie", "token=", () -> "bad")`.
7. **A second API in the same project:** add `services.payments.base.url=...` and use `super("/payments", RequestSpecFactory.forService("payments"))`.
8. **Available checks** (`ResponseValidator.of(response)…`), each shown as a step in Allure:

   | Method | Checks |
   |---|---|
   | `statusCode(int)` | HTTP status |
   | `contentType(String)` | `Content-Type` contains the value |
   | `hasHeader(String)` | header is present |
   | `matchesSchema(String)` | body matches a JSON schema on the classpath (`schemas/...`) |
   | `respondedWithin(long ms)` | response time ≤ limit |
   | `body(String path, Matcher)` | JSON path value, e.g. `body("totalprice", equalTo(750))` |
   | `bodyContains(String)` / `bodyDoesNotContain(String)` | raw body text |
   | `as(Class)` / `response()` | deserialize (records/POJOs) or get the raw `Response` for AssertJ checks |
9. Delete the sample `booking` package, its services, models, data builder, schemas and `testdata/booking.json`.

> **Reusing the core as a library (optional):** `mvn install` this project. In each API project, depend on `com.qa.automation:api-test-framework:1.0.0` and keep only `src/test`. The Jenkinsfile and config files work the same way.

## 10. Troubleshooting

| Symptom | Cause / fix |
|---|---|
| Many tests fail with **418** | The API requires `Accept: application/json` exactly. REST Assured's `ContentType.JSON` sends four media types. The framework now sets the exact value in `RequestSpecFactory`. Keep it that way. |
| `allure:serve` fails: *Could not find artifact allure-commandline:zip:2.29.1* | The report CLI has its own versions, separate from allure-java. Set `allure.commandline.version` in `pom.xml` to an existing release (currently 2.46.1). |
| A test that was deleted/renamed still runs | Old classes in `target`. Run `mvn clean test`. |
| One or two random failures / timeouts | Restful-Booker is a free shared service that resets its data and is sometimes slow. Failing tests are already retried twice automatically (see *Automatic retry*). If a test still fails all 3 attempts, it's a real failure, or the service is down. If it's often slow, raise `sla.response.ms` or `http.read.timeout.ms`. |
| Want to see a failure without retries while debugging | `mvn clean test -Dretry.count=0 -Dtest=ClassName#method` |
| No `[RETRY]` lines in the log | The test class must extend `BaseApiTest`, which registers `RetryLoggingExtension`. Check `org.slf4j.simpleLogger.log.RetryLog` in `simplelogger.properties` isn't set above `warn`. |
| Every attempt is logged as `attempt 1/3` | Surefire re-ran the test in a new JVM, so the attempt counter reset. Keep the default single fork (don't set `reuseForks=false`). |
| `git` says *Another git process seems to be running* | A stale `.git/index.lock` was left behind (e.g. an interrupted command or a sync tool). Close other git tools and delete that file. |
| `Could not transfer artifact` / `PKIX path building failed` | Corporate proxy or SSL inspection. Configure the proxy in `~/.m2/settings.xml` or import the company CA into the JDK truststore. |
| Prod run fails with *AWS SSM parameter not found* / *AccessDenied* | Check the parameter name, `aws.region`, and IAM permissions (`ssm:GetParameter`, `kms:Decrypt`). See section 6. |
| Project on OneDrive syncs thousands of files | `target/` and `.allure/` are build output (git-ignored). Pause sync while building, or keep the project outside OneDrive. |
