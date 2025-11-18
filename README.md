QAP JUnit 5 Plugin — Comprehensive Guide

Overview
- Bridges JUnit 5 lifecycle to a simple, structured reporting model (“QAP”).
- Builds a Launch object per top-level test class, collects all tests (including nested/parameterized), and publishes as JSON.
- Designed for clarity, predictable aggregation, and easy post-processing.

Key Features
- One JSON per top-level test class (per JVM). Nested classes aggregate into their parent’s JSON.
- Rich test metadata: method names, display names, parent chain, tags, timings, status, exceptions.
- Clear tag semantics: tags grouped by origin under a single field:
  "tags": { "class": [...], "method": [...], "inherited": [...] }.
- Pluggable publishing: stdout + logs by default; logging-only and async variants available.
- Resilient: handles missing lifecycle edges by recovering at afterAll without failing the run.

Install & Enable
- Gradle: this project is already configured with JUnit 5.
- Enable the extension at the class level:
  `@ExtendWith(com.mk.fx.qa.qap.junit.extension.QAPJunitExtension.class)`

Quick Start
- Annotate a test class and run tests. A JSON payload for the class appears in logs/stdout.
- Example: `src/test/java/DemoExtensionUsageTest.java:1`

How It Works
- Lifecycle bridging
  - beforeAll: generates a launch for the top-level class and stores it in the JUnit root store.
  - beforeEach: creates a QAPTest, sets start time and tags, and stores it in the method store.
  - TestWatcher (success/aborted/failed/disabled): finalizes status/exception.
  - afterEach: moves the test case into the class store.
  - afterAll (top-level): enriches header (git/app/env), attaches collected tests, publishes JSON.
- Nested classes: still receive lifecycle callbacks, but only the top-level afterAll publishes.
- Recovery: if afterAll(top-level) runs without a recorded launch (e.g., missing beforeAll), the extension rebuilds a minimal launch and proceeds (logged at WARN).

Data Model (JSON)
- Launch header: launchId, start/end time, app/user/env/os/java/JUnit version, optional gitBranch.
- Test class: className, displayName, lifecycle events (reserved), tests array.
- Test case fields (important ones):
  - methodName: Java method name.
  - displayName: run-level display (keeps parameterized display like “Run 1: …”).
  - testCaseId: stable identifier `TopLevelClass#methodName[runIndex]` for parameterized runs, or `TopLevelClass#methodName` for normal tests.
- methodDisplayName: static @DisplayName on the method; null if absent.
- parentDisplayName: current class’ @DisplayName; null if absent.
  - parentClassKey: fully qualified name of the test class (handles nested names).
  - parentChain: top-down display names of ancestors ending with current class display.
  - tags: object grouping tags by origin:
    - class: tags found on the current class.
    - method: tags found on the method.
    - inherited: tags found on enclosing classes (excluding current).
  - parameters: array of { index, type, value } for template/parameterized invocations.
  - startTime/endTime/status/exception.

Display Names
- Method run display (displayName) prioritizes the dynamic parameterized name (if present), then @DisplayName, then method name.
- Method static display (methodDisplayName) is the @DisplayName if present; otherwise null.
- Class display (parentDisplayName) is the class’ @DisplayName if present; otherwise null.
- Parent chain includes ancestors’ display names plus the current class’ display name.

Tags
- tags.method: `@Tag` on the test method only.
- tags.class: `@Tag` on the current test class only.
- tags.inherited: `@Tag` on enclosing classes (excluding current).

Nested Classes
- The extension aggregates all nested tests under their outermost class. The resulting JSON includes correct parentChain, parentDisplayName, and class/inherited tags.
- Example: `src/test/java/DemoExtensionUsageTest.java:1` shows nested groups with `@Nested` and tags (e.g., `@Tag("InnerTag")`).

Parameterized & Template Tests
- Dynamic run names are preserved in `displayName` (e.g., `@ParameterizedTest(name = "Run {index}: {0} + {1} = {2}")`).
- Method static name is preserved separately in `methodDisplayName`.
- Parameters are captured as structured JSON, not Base64, e.g.:
  `"parameters": [ { "index": 0, "type": "Integer", "value": "1" } ]`

Parallel & Multiple JVMs
- Aggregation is per top-level class per JVM. In parallel forks, each fork produces its own class-level JSON.
- If you need a single run file, merge outputs post-test (e.g., via a Gradle task that concatenates `testClass` arrays).

Publishers
- Default: StdOutPublisher — logs summary at INFO and prints full JSON to stdout; full payload also at DEBUG.
- LoggingPublisher: logs summary at INFO; full JSON at DEBUG; no stdout.
- AsyncPublisher: wraps any publisher and publishes on a background thread (daemon).
- Customizing publishers:
  - With `@RegisterExtension` you can inject a custom runtime and publisher:
    `@RegisterExtension static QAPJunitExtension ext = new QAPJunitExtension(
        QAPRuntime.defaultRuntime() /* or custom */,
        new QAPJunitLifeCycleEventCreator(new ConcurrentHashMap<>()),
        new QAPJunitTestEventsCreator(),
        new QAPJunitMethodInterceptor(new ConcurrentHashMap<>()),
        new QAPLaunchIdGenerator()
    );`

Configuration
- qap.properties (optional) on classpath:
  - `qap.app.name`: application name
  - `qap.user`: user name (defaults to system user)
  - `qap.test.environment`: environment label
  - `qap.run.environment`: run env (default: UAT)
  - `qap.report.test.data`: `true/false` to enable publishing (default: true)
- git.properties (optional): if present, `git.branch` is included.

Logging & Observability
- Logs are structured with `launchId`, class display, and basic payload metrics.
- Lifecycle failures are logged at WARN and do not fail tests.

Thread Safety
- LaunchId generation uses a synchronized `generateIfAbsent()` to avoid races.
- Shared state is held in JUnit’s root `ExtensionContext.Store` and in thread-safe maps shared by lifecycle and method interceptors.

Error Handling & Recovery
- If `afterAll` runs at the top-level without a stored launch (e.g., custom engine skipped `beforeAll`), the extension rebuilds a minimal launch, logs a WARN including `launchId`, and proceeds.
- Serialization failures are logged; the test run is not failed.

Examples
- Normal tests:
  `@Test @DisplayName("Should run a normal test") @Tag("Normal") void test() { ... }`
- Parameterized:
  `@ParameterizedTest(name = "Run {index}: {0} + {1} = {2}")`
- Nested classes:
  `@Nested @Tag("Outer") class Group { @Nested @Tag("InnerTag") class Inner { ... } }`
- Programmatic registration (custom runtime/publisher): use `@RegisterExtension` as shown above.

Troubleshooting
- “My nested class tags don’t appear”: see `tags.class` for current class and `tags.inherited` for ancestors.
- “I see multiple JSONs”: one per top-level class per JVM is expected. Merge if you need a single file.
- “No JSON printed”: ensure `qap.report.test.data=true` and logger level allows INFO/DEBUG as needed.
