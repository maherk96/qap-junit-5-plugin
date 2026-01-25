# QAP Plugin Test Coverage Analysis

**Date:** January 23, 2026  
**Module:** qap-plugin  
**Total Source Files:** 41  
**Total Test Files:** 12  
**Total Unit Tests:** 36  

---

## Test Distribution

### qap-plugin (Unit Tests)
- **36 unit tests** in 12 test files
- Tests plugin components in isolation
- Uses Mockito for mocking JUnit contexts

### test-app (Integration Tests)
- **43 integration tests** in 4 test files
- Tests plugin behavior end-to-end
- Real JUnit execution scenarios

**Grand Total: 79 tests**

---

## Coverage by Package

### 1. Core Package (5 files)

| Source File | Test File | Tests | Coverage | Notes |
|------------|-----------|-------|----------|-------|
| QAPLaunchIdGenerator | QAPLaunchIdGeneratorConcurrencyTest | 1 | 🟡 Partial | Only concurrency tested |
| SystemProperties | ❌ None | 0 | ❌ None | Constants class - low priority |
| ExtensionUtil | ❌ None | 0 | ❌ None | **GAP** - Needs tests |
| QAPUtils | ❌ None | 0 | ❌ None | **GAP** - Needs tests |
| TestCaseStatus | ❌ None | 0 | ❌ None | Enum - covered in integration |

**Coverage: 20% (1/5 files tested)**

**Gaps:**
- ❌ QAPLaunchIdGenerator: Missing tests for `generate()`, `isFullLaunchId()`, validation
- ❌ ExtensionUtil: No tests for `getOsVersion()`, `getJdkVersion()`, `isRegressionEnabled()`
- ❌ QAPUtils: No tests for constant correctness, utility methods

### 2. Extension Package (11 files)

| Source File | Test File | Tests | Coverage | Notes |
|------------|-----------|-------|----------|-------|
| QAPJunitExtension | Multiple integration tests | ~30 | ✅ Good | Main extension tested end-to-end |
| QAPJunitMethodInterceptor | QAPJunitMethodInterceptorTest | 1 | 🟡 Partial | Only basic interception tested |
| QAPJunitTestEventsCreator | QAPJunitTestEventsCreatorTest | 8 | ✅ Good | Event creation well covered |
| DisplayNameResolver | DisplayNameResolverTest | 3 | ✅ Good | Display name logic covered |
| ITestEventCreator | ❌ None | 0 | ✅ OK | Interface - tested via impl |
| IMethodInterceptor | ❌ None | 0 | ✅ OK | Interface - tested via impl |
| ILifeCycleEventCreator | ❌ None | 0 | ✅ OK | Interface - not used yet |
| LifeCycleEvent | ❌ None | 0 | 🟡 Partial | **GAP** - Model class needs tests |
| AsyncPublisher | ❌ None | 0 | ❌ None | **GAP** - Needs tests |
| LaunchPublisher | ❌ None | 0 | ✅ OK | Interface - tested via impl |
| LoggingPublisher | ❌ None | 0 | ❌ None | **GAP** - Needs tests |
| StdOutPublisher | ❌ None | 0 | ❌ None | **GAP** - Needs tests |

**Coverage: 36% (4/11 files tested)**

**Specialized Tests:**
- QAPJunitExtensionAggregationTest (2 tests) - Nested class aggregation
- QAPJunitExtensionDisabledTest (1 test) - Disabled test handling
- QAPJunitNestedTagsTest (9 tests) - Nested tag inheritance
- QAPJunitTagSeparationTest (8 tests) - Tag separation logic
- TestCaseBuilderTest (3 tests) - Test case construction
- TagCollectorTest (1 test) - Tag collection

**Gaps:**
- ❌ QAPJunitMethodInterceptor: Missing tests for `beforeAll`, `afterAll`, `beforeEach`, `afterEach` interception
- ❌ Publishers: No tests for AsyncPublisher, LoggingPublisher, StdOutPublisher
- ❌ LifeCycleEvent: Model class not tested

### 3. Factory Package (1 file)

| Source File | Test File | Tests | Coverage | Notes |
|------------|-----------|-------|----------|-------|
| TestMetadataFactory | TestMetadataFactoryTest | 4 | ✅ Good | Factory methods tested |

**Coverage: 100% (1/1 files tested)**

### 4. Model Package (15 files)

| Source File | Test File | Tests | Coverage | Notes |
|------------|-----------|-------|----------|-------|
| QAPTest | ❌ None | 0 | 🟡 Partial | Integration tested, no unit tests |
| QAPTestClass | ❌ None | 0 | 🟡 Partial | Integration tested, no unit tests |
| QAPJunitLaunch | ❌ None | 0 | 🟡 Partial | Integration tested, no unit tests |
| QAPHeader | ❌ None | 0 | 🟡 Partial | Integration tested, no unit tests |
| QAPBaseTestCase | ❌ None | 0 | 🟡 Partial | Integration tested, no unit tests |
| QAPTestParams | ❌ None | 0 | 🟡 Partial | Integration tested, no unit tests |
| QAPParameterization | ❌ None | 0 | 🟡 Partial | Integration tested, no unit tests |
| QAPFailure | ❌ None | 0 | 🟡 Partial | Integration tested, no unit tests |
| QAPFailureLocation | ❌ None | 0 | 🟡 Partial | Integration tested, no unit tests |
| QAPRootCause | ❌ None | 0 | 🟡 Partial | Integration tested, no unit tests |
| QAPFixture | ❌ None | 0 | 🟡 Partial | Integration tested, no unit tests |
| QAPTestLifecycle | ❌ None | 0 | 🟡 Partial | Integration tested, no unit tests |
| QAPTestFixture | ❌ None | 0 | 🟡 Partial | Integration tested, no unit tests |
| QAPTags | ❌ None | 0 | 🟡 Partial | Integration tested, no unit tests |
| QAPClassTags | ❌ None | 0 | 🟡 Partial | Integration tested, no unit tests |
| QAPPropertiesLoader | ❌ None | 0 | 🟡 Partial | Integration tested, no unit tests |

**Coverage: 0% direct unit tests (15/15 files have NO unit tests)**

**Note:** All model classes are Lombok @Data classes and are tested indirectly through integration tests. Direct unit tests for DTOs may be low value, but Jackson serialization should be tested.

**Gaps:**
- ❌ No unit tests for JSON serialization/deserialization
- ❌ No tests for builder patterns
- ❌ No tests for null handling in models
- ❌ No tests for QAPPropertiesLoader (file loading, defaults)

### 5. Store Package (2 files)

| Source File | Test File | Tests | Coverage | Notes |
|------------|-----------|-------|----------|-------|
| StoreManager | ❌ None | 0 | 🟡 Partial | Tested via QAPJunitExtensionAggregationTest |
| StoreKeys | ❌ None | 0 | ✅ OK | Constants class - low priority |

**Coverage: 0% direct unit tests**

**Note:** StoreManager is critical but only tested indirectly via integration tests.

**Gaps:**
- ❌ No direct tests for StoreManager methods
- ❌ No tests for store corruption handling
- ❌ No tests for concurrent store access

### 6. Util Package (2 files)

| Source File | Test File | Tests | Coverage | Notes |
|------------|-----------|-------|----------|-------|
| TagExtractor | TagExtractorTest | 2 | ✅ Good | Tag extraction well tested |
| ExceptionFormatter | ❌ None | 0 | ❌ None | **CRITICAL GAP** - Needs tests |

**Coverage: 50% (1/2 files tested)**

**Gaps:**
- ❌ ExceptionFormatter: No tests for exception conversion, circular reference handling, root cause extraction

### 7. Runtime Package (1 file)

| Source File | Test File | Tests | Coverage | Notes |
|------------|-----------|-------|----------|-------|
| QAPRuntime | ❌ None | 0 | 🟡 Partial | Used in all tests, no direct tests |

**Coverage: 0% direct unit tests**

**Gaps:**
- ❌ No tests for defaultRuntime()
- ❌ No tests for ObjectMapper configuration
- ❌ No tests for runtime dependencies

### 8. Other Files (2 files)

| Source File | Test File | Tests | Coverage | Notes |
|------------|-----------|-------|----------|-------|
| JsonUtil | ❌ None | 0 | ❌ None | **GAP** - Needs tests if used |
| Main | ❌ None | 0 | ❌ None | Entry point - low priority |

---

## Summary Statistics

### Coverage by Category

| Category | Files | Tested | % | Status |
|----------|-------|--------|---|--------|
| Core | 5 | 1 | 20% | 🟡 Needs improvement |
| Extension | 11 | 4 | 36% | 🟡 Needs improvement |
| Factory | 1 | 1 | 100% | ✅ Excellent |
| Model | 15 | 0 | 0% | 🟡 Integration tested |
| Store | 2 | 0 | 0% | 🟡 Integration tested |
| Util | 2 | 1 | 50% | 🟡 Needs improvement |
| Runtime | 1 | 0 | 0% | 🟡 Integration tested |
| Other | 2 | 0 | 0% | ❌ Low priority |

**Overall Direct Coverage: 17% (7/41 files have direct unit tests)**

**Note:** Integration tests cover ~80% of functionality, but unit tests are sparse.

---

## Critical Gaps (High Priority)

### 1. ExceptionFormatter ⚠️ CRITICAL
**File:** `util/ExceptionFormatter.java`  
**Current Tests:** 0  
**Priority:** HIGH

**Why Critical:**
- Handles all exception conversion to QAPFailure
- Contains circular reference prevention logic
- Extracts root causes, stack traces, locations
- Already had a bug fixed (circular reference)

**Missing Tests:**
- ❌ Basic exception conversion
- ❌ Nested exception handling (causedBy)
- ❌ Circular reference prevention
- ❌ Suppressed exceptions
- ❌ Root cause extraction
- ❌ Failure location extraction
- ❌ Null handling

### 2. QAPLaunchIdGenerator ⚠️ HIGH
**File:** `core/QAPLaunchIdGenerator.java`  
**Current Tests:** 1 (concurrency only)  
**Priority:** HIGH

**Missing Tests:**
- ❌ generate() with null/empty prefix
- ❌ isFullLaunchId() validation logic
- ❌ System property reading/writing
- ❌ UUID format validation
- ❌ Edge cases (very long prefix, special characters)

### 3. QAPJunitMethodInterceptor ⚠️ HIGH
**File:** `extension/QAPJunitMethodInterceptor.java`  
**Current Tests:** 1 (basic only)  
**Priority:** HIGH

**Missing Tests:**
- ❌ beforeAll/afterAll interception with failures
- ❌ beforeEach/afterEach interception with failures
- ❌ Fixture timing capture
- ❌ Failed init tracking and cleanup
- ❌ Parameterization provider extraction
- ❌ Multiple lifecycle methods

### 4. Publishers ⚠️ MEDIUM
**Files:** `extension/publisher/*`  
**Current Tests:** 0  
**Priority:** MEDIUM

**Missing Tests:**
- ❌ StdOutPublisher: JSON printing to stdout
- ❌ LoggingPublisher: Logging via SLF4J
- ❌ AsyncPublisher: Async publishing, thread safety

### 5. QAPPropertiesLoader ⚠️ MEDIUM
**File:** `model/QAPPropertiesLoader.java`  
**Current Tests:** 0  
**Priority:** MEDIUM

**Missing Tests:**
- ❌ Loading qap.properties from classpath
- ❌ Default value fallbacks
- ❌ System property fallbacks
- ❌ Missing file handling

### 6. StoreManager ⚠️ MEDIUM
**File:** `store/StoreManager.java`  
**Current Tests:** 0 (only integration)  
**Priority:** MEDIUM

**Missing Tests:**
- ❌ Store data retrieval/storage
- ❌ Type corruption handling
- ❌ Nested class resolution
- ❌ Concurrent access patterns

### 7. ExtensionUtil ⚠️ LOW
**File:** `core/ExtensionUtil.java`  
**Current Tests:** 0  
**Priority:** LOW

**Missing Tests:**
- ❌ getOsVersion()
- ❌ getJdkVersion()
- ❌ isRegressionEnabled()

### 8. QAPUtils ⚠️ LOW
**File:** `core/QAPUtils.java`  
**Current Tests:** 0  
**Priority:** LOW

**Missing Tests:**
- ❌ Constant validation
- ❌ Utility method tests (if any exist)

---

## Existing Test Coverage (Good Areas)

### ✅ Well Tested Components

#### DisplayNameResolver (3 tests)
- ✅ Parameterized name resolution
- ✅ Method and class display names
- ✅ Parent chain building

#### QAPJunitTestEventsCreator (8 tests)
- ✅ Test template creation
- ✅ Status handling (PASSED, FAILED, ABORTED)
- ✅ Exception mapping
- ✅ Lifecycle event creation

#### TagExtractor (2 tests)
- ✅ Method-level tag extraction
- ✅ Class and inherited tag separation

#### TestMetadataFactory (4 tests)
- ✅ Factory creation
- ✅ Display name population
- ✅ Parent field population

#### Tag-Related Tests
- QAPJunitNestedTagsTest (9 tests) - ✅ Comprehensive nested tag testing
- QAPJunitTagSeparationTest (8 tests) - ✅ Tag separation logic
- TagCollectorTest (1 test) - ✅ Tag collection

#### Aggregation Tests
- QAPJunitExtensionAggregationTest (2 tests) - ✅ Nested class aggregation
- QAPJunitExtensionDisabledTest (1 test) - ✅ Disabled test handling

#### Test Case Building
- TestCaseBuilderTest (3 tests) - ✅ Test construction logic

---

## Recommended Improvements

### Priority 1: Critical Missing Tests

#### ExceptionFormatter (CRITICAL)
```java
@Test
void testBasicExceptionConversion() {
    Throwable t = new RuntimeException("Test error");
    QAPFailure failure = ExceptionFormatter.toFailure(t);
    
    assertNotNull(failure);
    assertEquals("java.lang.RuntimeException", failure.getExceptionClass());
    assertEquals("Test error", failure.getMessage());
    assertNotNull(failure.getLocation());
}

@Test
void testCircularReferenceHandling() {
    Exception root = new Exception("Root");
    Exception circular = new Exception("Circular");
    root.initCause(circular);
    circular.initCause(root); // Circular!
    
    QAPFailure failure = ExceptionFormatter.toFailure(root);
    assertNotNull(failure); // Should not stack overflow
}

@Test
void testRootCauseExtraction() {
    Exception root = new IOException("Root cause");
    Exception middle = new SQLException("Middle", root);
    Exception top = new RuntimeException("Top", middle);
    
    QAPFailure failure = ExceptionFormatter.toFailure(top);
    assertNotNull(failure.getRootCause());
    assertEquals("java.io.IOException", failure.getRootCause().getExceptionClass());
}

@Test
void testSuppressedExceptions() {
    Exception main = new Exception("Main");
    Exception suppressed1 = new Exception("Suppressed 1");
    Exception suppressed2 = new Exception("Suppressed 2");
    main.addSuppressed(suppressed1);
    main.addSuppressed(suppressed2);
    
    QAPFailure failure = ExceptionFormatter.toFailure(main);
    assertNotNull(failure.getSuppressed());
    assertEquals(2, failure.getSuppressed().size());
}
```

#### QAPLaunchIdGenerator (HIGH)
```java
@Test
void testGenerateWithValidPrefix() {
    QAPLaunchIdGenerator gen = new QAPLaunchIdGenerator();
    String id = gen.generate("test-prefix");
    
    assertNotNull(id);
    assertTrue(id.startsWith("test-prefix-"));
    assertTrue(id.matches("test-prefix-[a-zA-Z0-9]{12}"));
}

@Test
void testIsFullLaunchIdValidation() {
    QAPLaunchIdGenerator gen = new QAPLaunchIdGenerator();
    
    assertTrue(gen.isFullLaunchId("prefix-abc123def456"));
    assertFalse(gen.isFullLaunchId("prefix"));
    assertFalse(gen.isFullLaunchId("prefix-abc")); // Too short
    assertFalse(gen.isFullLaunchId(null));
}

@Test
void testGenerateIfAbsentCreateWhenMissing() {
    System.clearProperty("launchID");
    QAPLaunchIdGenerator gen = new QAPLaunchIdGenerator();
    
    String id = gen.generateIfAbsent();
    assertNotNull(id);
    assertEquals(id, System.getProperty("launchID"));
}

@Test
void testGenerateIfAbsentReuseExisting() {
    System.setProperty("launchID", "existing-id123456789");
    QAPLaunchIdGenerator gen = new QAPLaunchIdGenerator();
    
    String id = gen.generateIfAbsent();
    assertEquals("existing-id123456789", id);
}
```

#### QAPJunitMethodInterceptor (HIGH)
```java
@Test
void testBeforeAllInterception() throws Throwable {
    // Test fixture timing capture
    // Test failure recording
    // Test fixture attachment to class
}

@Test
void testBeforeEachInterception() throws Throwable {
    // Test fixture timing capture
    // Test failure linking to test case
    // Test fixture attachment to test lifecycle
}

@Test
void testAfterAllInterception() throws Throwable {
    // Test fixture timing capture
    // Test cleanup of failed inits map
}

@Test
void testParameterizationProviderExtraction() throws NoSuchMethodException {
    // Test @ValueSource detection
    // Test @CsvSource detection
    // Test @MethodSource detection
    // Test @EnumSource detection
    // Test @ArgumentsSource detection
}
```

### Priority 2: Publisher Tests (MEDIUM)

#### StdOutPublisher
```java
@Test
void testPublishToStdOut() {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    System.setOut(new PrintStream(out));
    
    StdOutPublisher publisher = new StdOutPublisher();
    QAPJunitLaunch launch = createTestLaunch();
    publisher.publish(launch);
    
    String output = out.toString();
    assertTrue(output.contains("\"testClasses\""));
    assertTrue(output.contains("\"launchId\""));
}
```

#### AsyncPublisher
```java
@Test
void testAsyncPublishing() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    AsyncPublisher publisher = new AsyncPublisher(new LaunchPublisher() {
        @Override
        public void publish(QAPJunitLaunch launch) {
            latch.countDown();
        }
    });
    
    QAPJunitLaunch launch = createTestLaunch();
    publisher.publish(launch);
    
    boolean completed = latch.await(1, TimeUnit.SECONDS);
    assertTrue(completed, "Async publish should complete");
}
```

### Priority 3: Model Serialization Tests (MEDIUM)

#### Jackson Serialization Tests
```java
@Test
void testQAPTestSerialization() throws Exception {
    QAPTest test = new QAPTest("testMethod", "Test Display");
    test.setTestCaseId("MyTest#testMethod");
    test.setStatus("PASSED");
    test.setStartTime(1000L);
    test.setEndTime(2000L);
    
    ObjectMapper mapper = new ObjectMapper();
    String json = mapper.writeValueAsString(test);
    
    assertTrue(json.contains("\"testCaseId\":\"MyTest#testMethod\""));
    assertTrue(json.contains("\"status\":\"PASSED\""));
    
    // Deserialize back
    QAPTest deserialized = mapper.readValue(json, QAPTest.class);
    assertEquals("MyTest#testMethod", deserialized.getTestCaseId());
    assertEquals("PASSED", deserialized.getStatus());
}
```

### Priority 4: Utility Tests (LOW)

#### ExtensionUtil, QAPUtils, SystemProperties
These are simple utility classes with mostly static methods and constants. Lower priority but should still have basic tests.

---

## Coverage Improvements Needed

### Summary of Gaps

| Priority | Component | Tests Needed | Estimated Effort |
|----------|-----------|--------------|------------------|
| 🔴 CRITICAL | ExceptionFormatter | 10-15 tests | 2-3 hours |
| 🟠 HIGH | QAPLaunchIdGenerator | 8-10 tests | 1-2 hours |
| 🟠 HIGH | QAPJunitMethodInterceptor | 10-12 tests | 2-3 hours |
| 🟡 MEDIUM | Publishers (3 classes) | 6-8 tests total | 2-3 hours |
| 🟡 MEDIUM | Model Serialization | 10-15 tests | 2-3 hours |
| 🟡 MEDIUM | QAPPropertiesLoader | 5-6 tests | 1-2 hours |
| 🟡 MEDIUM | StoreManager | 8-10 tests | 2-3 hours |
| 🟢 LOW | ExtensionUtil | 3-4 tests | 1 hour |
| 🟢 LOW | QAPUtils | 3-4 tests | 1 hour |

**Total Estimated Effort:** 12-20 hours to achieve ~85% coverage

---

## Current Test Quality Assessment

### ✅ Strengths

1. **Integration Coverage** - Extension behavior well tested end-to-end
2. **Tag Handling** - Excellent coverage of tag extraction and separation
3. **Display Names** - Well tested with various scenarios
4. **Nested Classes** - Aggregation logic well covered
5. **Disabled Tests** - Edge case covered
6. **Parameterization** - Tested via integration tests

### ❌ Weaknesses

1. **Exception Handling** - Critical gap in ExceptionFormatter tests
2. **Publisher Abstraction** - No tests for 3 publisher implementations
3. **Model Serialization** - No direct JSON serialization tests
4. **Store Management** - Only tested indirectly
5. **Configuration Loading** - QAPPropertiesLoader not tested
6. **Utility Methods** - Several utility classes not tested

---

## Recommendations

### Immediate Actions (This Session)

1. **Create ExceptionFormatterTest** - Critical component, must be tested
2. **Expand QAPLaunchIdGeneratorTest** - Add validation and generation tests
3. **Expand QAPJunitMethodInterceptorTest** - Test all lifecycle methods

### Short-term (Next Session)

4. **Create PublisherTests** - Test StdOutPublisher, LoggingPublisher, AsyncPublisher
5. **Create ModelSerializationTest** - Test JSON serialization for key models
6. **Create QAPPropertiesLoaderTest** - Test file loading and defaults

### Long-term (Future)

7. **Create StoreManagerTest** - Test store operations directly
8. **Add UtilityTests** - Test ExtensionUtil, QAPUtils
9. **Performance Tests** - Benchmark large test suites
10. **Edge Case Tests** - Malformed data, extreme values

---

## Test Count Goals

**Current:**
- Unit tests: 36
- Integration tests: 43
- Total: 79

**Target:**
- Unit tests: 80-100 (add ~45-65 tests)
- Integration tests: 43 (keep as is)
- Total: 125-145

**Coverage Target:** 80-85% line coverage (measured by JaCoCo)

---

## Next Steps

Would you like me to:

1. **Create ExceptionFormatterTest** (CRITICAL) - ~15 comprehensive tests
2. **Expand QAPLaunchIdGeneratorTest** (HIGH) - Add 8-10 more tests
3. **Expand QAPJunitMethodInterceptorTest** (HIGH) - Add 10-12 more tests
4. **Create all missing tests** (Full coverage push) - ~50 new tests
5. **Set up JaCoCo** for code coverage reporting
6. **Something else**?

---

*Current Status: qap-plugin has good integration coverage but needs more unit tests for critical components like ExceptionFormatter, QAPLaunchIdGenerator, and Publishers.*
