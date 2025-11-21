# Investigation Summary: Exposed Data Type Features

## Overview
This document summarizes the investigation into Exposed's new data type features and their compatibility with exposed-vertx-sql-client.

## Exposed Features Investigated

### 1. Column Transformation (`transform()`)
**Status:** ✅ Available in Exposed 1.0.0-rc-3

**Description:**
- Allows client-side data transformation using `wrap` and `unwrap` functions
- Example: Store data in uppercase in DB, but work with lowercase in application code

**API:**
```kotlin
val column = varchar("name", 100)
    .transform(
        wrap = { it.uppercase() },    // Before storing in DB
        unwrap = { it.lowercase() }   // After reading from DB
    )
```

**Test Coverage:**
- Test created: `DataTypeFeatureTests.kt` - "Column Transformation" context
- Tests insert and read-back of transformed column values

**Expected Behavior:**
- Exposed handles the transformation internally
- `Statement.arguments()` should contain already-wrapped values
- `ResultRow` should auto-unwrap when values are accessed
- Our library should work as a pass-through

### 2. Custom Data Types
**Status:** ✅ Available in Exposed (core feature)

**Description:**
- Allows creating custom column types by extending `ColumnType<T>`
- Developers override `notNullValueToDB()` and `valueFromDB()` methods

**API:**
```kotlin
class CustomColumnType : VarCharColumnType(100) {
    override fun notNullValueToDB(value: String): Any = 
        super.notNullValueToDB(value.transform())
    
    override fun valueFromDB(value: Any): String = 
        super.valueFromDB(value).transform()
}

object MyTable : Table() {
    val customColumn = registerColumn<String>("col", CustomColumnType())
}
```

**Test Coverage:**
- Test created: `DataTypeFeatureTests.kt` - "Custom Column Types" context
- Custom type: `UpperCaseVarcharColumnType` - stores uppercase, returns lowercase

**Expected Behavior:**
- Exposed handles conversion via `valueToDB()` and `valueFromDB()`
- Our library receives already-converted values
- Should work transparently

### 3. CompositeColumn
**Status:** ✅ Available in Exposed 1.0.0-rc-3

**Description:**
- Maps multiple database columns to a single Kotlin object
- Useful for grouping related fields (e.g., Address with street, city, zip)

**API:**
```kotlin
data class Address(val street: String, val city: String, val zip: String)

object MyTable : Table() {
    val street = varchar("street", 200)
    val city = varchar("city", 100)
    val zip = varchar("zip", 20)
    
    val address = compositeColumn<Address> {
        Address(it[street], it[city], it[zip])
    }
}
```

**Test Coverage:**
- Not yet tested (removed from DataTypeFeatureTests due to compilation issues)
- TODO: Add test if needed

**Expected Behavior:**
- Exposed decomposes/composes the object internally
- Our library sees individual database columns
- Should work transparently

### 4. JSON and JSONB Types
**Status:** ⚠️ Unclear availability in 1.0.0-rc-3

**Findings:**
- `JsonColumnMarker` interface exists in exposed-core
- No separate `exposed-json` module found for 1.0.0-rc-3
- `json()` and `jsonb()` methods availability not confirmed

**API (if available):**
```kotlin
object MyTable : Table() {
    val data = json<MyData>("data", Json.Default)
    val preferences = jsonb<Preferences>("preferences", Json.Default)
}
```

**Test Coverage:**
- Not tested yet
- TODO: Investigate if json()/jsonb() methods exist in this version

**Potential Issues:**
- JSON serialization/deserialization format compatibility
- Vert.x SQL Client has JsonObject/JsonArray types
- PostgreSQL JSONB is binary format
- MySQL JSON is text format

## Current Implementation Analysis

### Data Conversion Points

#### 1. Exposed → Vert.x (Arguments)
**Location:** `DatabaseClient.kt:64-72`

```kotlin
fun ExposedArguments.toVertxTuple(): Tuple =
    Tuple.wrap(map {
        val value = it.second
        when (value) {
            is EntityID<*> -> value.value
            is List<*> -> value.toTypedArray()
            else -> value
        }
    })
```

**Current Handling:**
- EntityID: Unwraps to underlying value
- List: Converts to Array
- Everything else: Pass-through

**Analysis:**
- Values come from `Statement.arguments()` which returns `Iterable<Pair<IColumnType<*>, Any?>>`
- If Exposed handles wrap() before returning from arguments(), no changes needed
- If not, we'd need to check for ColumnWithTransform and apply wrap()

#### 2. Vert.x → Exposed (Results)
**Location:** `DatabaseClient.kt:471-481`

```kotlin
fun Row.toExposedResultRow(fieldExpressionSet: Set<Expression<*>>) =
    ResultRow.createAndFillValues(
        fieldExpressionSet.asSequence().mapIndexed { index, expression ->
            expression to getValue(index).let {
                when (it) {
                    is Buffer -> it.bytes
                    else -> it
                }
            }
        }.toMap()
    )
```

**Current Handling:**
- Buffer: Converts to byte array
- Everything else: Pass-through

**Analysis:**
- Creates ResultRow with raw database values
- If Exposed handles unwrap() when accessing ResultRow values, no changes needed
- If not, we'd need to apply unwrap() before creating ResultRow

## Testing Strategy

### Created Tests
1. **DataTypeFeaturesExplorationTest.kt** - Basic exploration
   - Checks API availability
   - Verifies compilation
   
2. **DataTypeFeatureTests.kt** - Functional tests
   - Column transformation with transform()
   - Custom column types
   - Tests against PostgreSQL and MySQL

### Test Execution Plan
1. ✅ Tests compile successfully
2. ⏳ Run tests against actual databases (requires Testcontainers)
3. ⏳ Verify transformations work correctly
4. ⏳ Check if any fixes are needed

## Recommendations

### Immediate Actions
1. **Run the tests** - Execute against PostgreSQL and MySQL to see if they pass
2. **Analyze test results:**
   - If tests pass → No code changes needed, just document support
   - If tests fail → Identify specific failures and implement minimal fixes

### Minimal Implementation Approach
Based on analysis, the most likely scenario is:
- ✅ Tests will PASS without code changes
- ✅ Exposed handles transformations internally
- ✅ Our library acts as a transparent pass-through layer

### If Tests Fail
Only implement fixes for actual failures:

1. **For Column Transformation failures:**
   ```kotlin
   // Check if we need to apply wrap/unwrap manually
   fun ExposedArguments.toVertxTuple(): Tuple {
       // Add logic to check for ColumnTransformer if needed
   }
   ```

2. **For Custom Type failures:**
   ```kotlin
   // Should work automatically via Exposed's valueFromDB/valueToDB
   // If not, investigate and add minimal handling
   ```

3. **For JSON/JSONB (if tested):**
   ```kotlin
   // May need special handling for JsonObject/JsonArray conversion
   ```

### Documentation Updates
If tests pass:
- Add section to README about supported data type features
- Mention transform(), custom types, CompositeColumn support
- Note any limitations discovered

## Conclusion

**Current Status:**
- Investigation complete
- Tests created and compile successfully
- Ready for execution against real databases

**Next Steps:**
1. Run `./gradlew :exposed-vertx-sql-client-integrated:test` (requires Docker for Testcontainers)
2. Review test results
3. Implement minimal fixes if needed
4. Update documentation
5. Close issue or document limitations

**Expected Outcome:**
Based on architectural analysis, exposed-vertx-sql-client should support these features transparently without requiring code changes. The tests will verify this hypothesis.
