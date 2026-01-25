# DTO Cleanup Analysis - QAP JUnit Model Classes

## Current State Analysis

### Redundant/Unused Fields Identified

#### 1. **QAPBaseTestCase** - Legacy fields no longer used

**Unused fields:**
```java
@JsonIgnore protected byte[] logs;        // ❌ UNUSED - replaced by logEntries
@JsonIgnore protected byte[] fix;         // ❌ UNUSED - always empty, unknown purpose
protected Set<String> tag;                 // ⚠️  Complex - wrapped in getTags()
protected Set<String> classTags;           // ⚠️  Complex - wrapped in getTags()
protected Set<String> inheritedClassTags;  // ⚠️  Complex - wrapped in getTags()
```

**Unused methods:**
```java
public boolean hasLogs()                  // ❌ UNUSED
public boolean hasFix()                   // ❌ UNUSED
@JsonProperty("logs") getLogs()           // ❌ Returns empty list always
@JsonProperty("fix") getFixArray()        // ❌ Returns empty list always
@JsonProperty("hasFailure") hasFailure()  // ❌ Hidden in QAPTest, redundant
```

**Evidence:**
- `logs` and `fix` byte arrays are never populated
- `getLogs()` always returns `Collections.emptyList()`
- `getFixArray()` always returns `Collections.emptyList()`
- `hasFailure()` is explicitly `@JsonIgnore` in QAPTest
- JSON output shows `"fix": []` - waste of bytes

#### 2. **QAPTest** - Hiding parent redundancy

**Hidden but still present:**
```java
// These are @JsonIgnore but still exist in parent class
@Override @JsonIgnore getDurationMillis()     // Hidden - use lifecycle or totalDurationNanos
@Override @JsonIgnore getDurationNanos()      // Hidden - use lifecycle.test.durationNanos
@Override @JsonIgnore hasFailure()            // Hidden - infer from failure != null
@Override @JsonIgnore getLogs()               // Hidden - use lifecycle logs
```

**Issue:** The parent class still has these fields/methods, we're just hiding them. Better to remove entirely.

#### 3. **Tag Management** - Overly complex

**Current structure:**
```java
// QAPBaseTestCase
protected Set<String> tag;                    // Method-level tags
protected Set<String> classTags;              // Class-level tags
protected Set<String> inheritedClassTags;     // Parent class tags

// Exposed as:
@JsonProperty("tags")
public QAPTags getTags() {
  return new QAPTags(tag, classTags, inheritedClassTags);
}
```

**Output:**
```json
{
  "tags": {
    "class": ["financial", "payment"],
    "method": [],
    "inherited": []
  }
}
```

**Issue:** Three separate sets internally, one wrapper object externally. Could be simplified.

#### 4. **Duration Fields** - Multiple representations

**Current:**
```java
// QAPBaseTestCase
protected long startTime;              // epoch millis
protected long endTime;                // epoch millis
@JsonIgnore protected long startTimeNanos;   // System.nanoTime()
@JsonIgnore protected long endTimeNanos;     // System.nanoTime()

// Computed:
@JsonProperty getDurationMillis()      // endTime - startTime
@JsonProperty getDurationNanos()       // endTimeNanos - startTimeNanos (or convert millis)

// QAPTest additional:
@JsonProperty getTotalDurationNanos()  // Sum of lifecycle phases
@JsonIgnore getTestOnlyDurationNanos() // Just test execution
```

**Issue:** 
- 4 time fields (2 hidden) + 4 computed methods = 8 time-related fields
- `durationMillis` and `durationNanos` are hidden in QAPTest but still computed
- Could simplify to just timestamps + one computed duration

#### 5. **Properties not used**

**QAPPropertiesLoader:**
```java
private final String fixMessageLogging;  // ❌ Property never used
```

**Property:** `qap.report.fix.messaging` - loaded but never accessed

---

## Cleanup Recommendations

### Priority 1: Remove Completely Unused Fields

#### Remove `logs` and `fix` byte arrays

**Files to modify:**
- `QAPBaseTestCase.java`

**Changes:**
```java
// DELETE these fields:
@JsonIgnore protected byte[] logs;
@JsonIgnore protected byte[] fix;

// DELETE these methods:
public boolean hasLogs()
public boolean hasFix()
@JsonProperty("logs") public List<String> getLogs()
@JsonProperty("fix") public List<String> getFixArray()
```

**Impact:** 
- ✅ No runtime impact - never used
- ✅ Removes `"fix": []` from JSON output
- ✅ Cleaner DTOs

---

### Priority 2: Simplify Tag Management

**Option A: Keep wrapper, simplify internals**

Current JSON output is good:
```json
{
  "tags": {
    "class": ["financial", "payment"],
    "method": [],
    "inherited": []
  }
}
```

**Keep as-is** - the complexity is internal and JSON is clean.

**Option B: Flatten to single Set**

If we don't need the distinction:
```java
protected Set<String> allTags;  // Combine all tags

@JsonProperty("tags")
public Set<String> getTags() {
  return Collections.unmodifiableSet(allTags);
}
```

Output would be:
```json
{
  "tags": ["financial", "payment"]
}
```

**Recommendation:** **Keep Option A** - the class/method/inherited distinction is useful for filtering.

---

### Priority 3: Remove Hidden Duration Fields

**Current problem:**
- `durationMillis` and `durationNanos` exist in base class
- Hidden via `@JsonIgnore` in QAPTest
- Still computed even though hidden

**Solution:** Remove from JSON output entirely

**Files to modify:**
- `QAPBaseTestCase.java`

**Changes:**
```java
// Make these @JsonIgnore at base level
@JsonIgnore
public long getDurationMillis() { ... }

@JsonIgnore  
public long getDurationNanos() { ... }
```

Then remove the override `@JsonIgnore` in QAPTest - it's redundant.

**JSON output:**
```json
{
  "startTime": 1737734567329,
  "endTime": 1737734567330,
  "totalDurationNanos": 13903791,  // Only this
  "lifecycle": {
    "test": { "durationNanos": 12000000 }
  }
}
```

**Impact:**
- ✅ Cleaner JSON (one duration field instead of three hidden ones)
- ✅ No breaking changes (hidden fields weren't in output anyway)

---

### Priority 4: Remove `hasFailure()`

**Current:**
```java
// QAPBaseTestCase
@JsonProperty("hasFailure")
public boolean hasFailure() {
  return failure != null;
}

// QAPTest
@Override
@JsonIgnore
public boolean hasFailure() {
  return super.hasFailure();
}
```

**Issue:** Redundant - can infer from `failure != null`

**Solution:** Make `@JsonIgnore` at base level

```java
// QAPBaseTestCase
@JsonIgnore  // ← Add this
public boolean hasFailure() {
  return failure != null;
}
```

Remove override in QAPTest - no longer needed.

**JSON output:**
```json
{
  "failure": null  // or { ... }
  // No hasFailure field
}
```

**Impact:**
- ✅ Smaller JSON
- ✅ Still accessible in code via `hasFailure()` method
- ✅ Clients can infer: `failure != null`

---

### Priority 5: Clean Up Properties

**Remove unused property:**

**File:** `QAPPropertiesLoader.java`

```java
// DELETE:
private final String fixMessageLogging;

// DELETE from constructor:
this.fixMessageLogging = qapAttributes.getProperty("qap.report.fix.messaging");
```

**Impact:** No impact - never used

---

## Summary of Changes

### Fields to Remove Entirely

| Field | Class | Reason | Impact |
|-------|-------|--------|--------|
| `byte[] logs` | QAPBaseTestCase | Never populated, replaced by logEntries | None |
| `byte[] fix` | QAPBaseTestCase | Never populated, unknown purpose | Removes `"fix": []` from JSON |
| `fixMessageLogging` | QAPPropertiesLoader | Property never accessed | None |

### Methods to Remove

| Method | Class | Reason | Impact |
|--------|-------|--------|--------|
| `hasLogs()` | QAPBaseTestCase | Always false | None |
| `hasFix()` | QAPBaseTestCase | Always false | None |
| `getLogs()` | QAPBaseTestCase | Always returns empty list | Removes `"logs"` from JSON (currently hidden) |
| `getFixArray()` | QAPBaseTestCase | Always returns empty list | Removes `"fix": []` from JSON |

### Annotations to Add (Hide from JSON)

| Method | Class | Change | Impact |
|--------|-------|--------|--------|
| `getDurationMillis()` | QAPBaseTestCase | Add `@JsonIgnore` | Cleaner JSON |
| `getDurationNanos()` | QAPBaseTestCase | Add `@JsonIgnore` | Cleaner JSON |
| `hasFailure()` | QAPBaseTestCase | Add `@JsonIgnore` | Cleaner JSON |

### Overrides to Remove (Redundant)

| Method | Class | Change | Reason |
|--------|-------|--------|--------|
| `getDurationMillis()` | QAPTest | Remove override | Base class is already `@JsonIgnore` |
| `getDurationNanos()` | QAPTest | Remove override | Base class is already `@JsonIgnore` |
| `hasFailure()` | QAPTest | Remove override | Base class is already `@JsonIgnore` |
| `getLogs()` | QAPTest | Remove override | Method deleted from base |

---

## Migration Path (No Backward Compatibility Needed)

Since you stated no backward compatibility needed, we can:

1. **Remove fields immediately** - they're not in use
2. **Change JSON output** - remove `fix`, `logs`, `hasFailure`
3. **Clean up code** - remove unused methods

**Test impact:** Only one test references `hasFailure()`:
```java
// QAPJunitExtensionDisabledTest.java:62
assertFalse(t.hasFailure(), "Disabled tests should not have failures");
```

**Fix:**
```java
// Change to:
assertNull(t.getFailure(), "Disabled tests should not have failures");
```

---

## Expected JSON Output After Cleanup

### Before (Current)
```json
{
  "startTime": 1769310864544,
  "endTime": 1769310864556,
  "status": "PASSED",
  "methodName": "testProcessCreditCardPayment",
  "displayName": "Should process valid credit card payment",
  "testCaseId": "PaymentProcessorTest#testProcessCreditCardPayment",
  "parameters": [],
  "testType": "TEST",
  "disabledReason": null,
  "failure": null,
  "totalDurationNanos": 13903791,
  "fix": [],                          // ❌ Remove
  "tags": { "class": ["financial"] }
}
```

### After (Cleaned)
```json
{
  "startTime": 1769310864544,
  "endTime": 1769310864556,
  "status": "PASSED",
  "methodName": "testProcessCreditCardPayment",
  "displayName": "Should process valid credit card payment",
  "testCaseId": "PaymentProcessorTest#testProcessCreditCardPayment",
  "parameters": [],
  "testType": "TEST",
  "failure": null,
  "totalDurationNanos": 13903791,
  "tags": { "class": ["financial"] }
}
```

**Removed:**
- `"fix": []` - always empty
- `disabledReason: null` - omitted when null (already handled by `@JsonInclude(NON_NULL)`)

**Bytes saved:** ~15-20 bytes per test case

---

## Files to Modify

1. **QAPBaseTestCase.java** - Remove logs/fix fields and methods, add @JsonIgnore
2. **QAPTest.java** - Remove redundant overrides
3. **QAPPropertiesLoader.java** - Remove fixMessageLogging
4. **QAPJunitExtensionDisabledTest.java** - Update assertion

---

## Estimated Impact

**Lines of code removed:** ~40-50 lines  
**JSON payload reduction:** ~5-10% (removing `fix: []` from every test)  
**Breaking changes:** None (unused fields/methods only)  
**Test failures:** 1 test needs minor update  

**Confidence:** HIGH ✅ - All removed code is provably unused
