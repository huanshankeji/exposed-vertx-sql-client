# JSON and JSONB Support - Findings and Status

## Summary

This document summarizes the work done to review and test JSON and JSONB column type support in the exposed-vertx-sql-client library.

## Exposed JSON/JSONB Support

According to the [Exposed documentation](https://www.jetbrains.com/help/exposed/json-and-jsonb-types.html), Exposed v1.0.0+ provides built-in support for JSON and JSONB column types through the `exposed-json` module.

### Prerequisites
- **Exposed version**: v1.0.0-rc-3 (current version in this project)
- **Module**: `org.jetbrains.exposed:exposed-json`
- **Serialization**: Requires kotlinx.serialization (kotlinx-serialization-json)
- **Plugin**: kotlin("plugin.serialization") for @Serializable annotation support

### Database Support
- **JSON**: PostgreSQL, MySQL, Oracle, SQL Server (2016+)
- **JSONB**: PostgreSQL only (binary JSON with better query performance)

## Changes Made

### 1. Dependencies Added
**File**: `integrated/build.gradle.kts`
```kotlin
plugins {
    ...
    kotlin("plugin.serialization") version "2.2.21"
}

dependencies {
    // For JSON/JSONB testing
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.exposed:exposed-json:1.0.0-rc-3")
    ...
}
```

### 2. Test Implementation
**File**: `integrated/src/test/kotlin/com/huanshankeji/exposedvertxsqlclient/JsonTests.kt`

Created comprehensive tests covering:
- Table definitions with JSON and JSONB columns
- Serializable data classes (UserData, Address, ComplexData)
- INSERT operations with simple and complex JSON data
- SELECT operations to verify deserialization
- UPDATE operations
- DELETE operations
- Nullable JSON columns

## Test Results

### Compilation Status
✅ **SUCCESS**: All code compiles successfully

### Runtime Test Status
❌ **FAILING**: 13 of 43 tests failing

### Failure Breakdown by Database

1. **PostgreSQL (6 failures)**: 
   - Error: `io.vertx.core.VertxException`
   - Affects: Both JSON and JSONB tests across SqlClient, Pool, and SqlConnection
   - **Root Cause**: Table creation or data serialization issue

2. **MySQL (3 failures)**:
   - Error: `io.vertx.mysqlclient.MySQLException`
   - Affects: JSON tests (JSONB not applicable to MySQL)
   - **Root Cause**: Needs investigation

3. **Oracle (2 failures)**:
   - Error: `java.sql.SQLException`
   - Affects: JSON tests
   - **Root Cause**: Needs investigation

4. **SQL Server (2 failures)**:
   - Error: `java.lang.IllegalArgumentException at DataType.java:1129`
   - Affects: JSON tests
   - **Root Cause**: JSON type may not be supported in current MSSQL driver version

## Technical Implementation Details

### Column Definition Pattern
```kotlin
import kotlinx.serialization.json.Json as KotlinxJson

@Serializable
data class UserData(val name: String, val age: Int, val email: String? = null)

object JsonTable : IntIdTable("json_test") {
    val data = json<UserData>(
        "data", 
        KotlinxJson.Default::encodeToString,
        KotlinxJson.Default::decodeFromString
    )
    val complexData = json<ComplexData>(
        "complex_data",
        KotlinxJson.Default::encodeToString,
        KotlinxJson.Default::decodeFromString
    ).nullable()
}
```

### Usage Pattern
```kotlin
// Insert
databaseClient.insert(JsonTable) {
    it[data] = UserData("Alice", 30, "alice@example.com")
    it[complexData] = complexData
}

// Select
val userData: UserData = databaseClient.selectSingleColumn(JsonTable, JsonTable.data, {
    where(JsonTable.id eq 1)
}).single()

// Update
databaseClient.update(JsonTable, { JsonTable.id eq 1 }) {
    it[data] = updatedUserData
}
```

## Known Issues and Limitations

### 1. Runtime Failures
The tests compile but fail at runtime. Possible causes:
- **Vert.x driver compatibility**: The Vert.x reactive SQL clients may not fully support Exposed's JSON serialization mechanism
- **Transaction context**: JSON operations may require special transaction handling
- **Type mapping**: Mismatch between Exposed's JSON type and database driver's type handling

### 2. Database-Specific Issues
- **SQL Server**: The IllegalArgumentException suggests the driver doesn't recognize the JSON data type
- **Oracle**: SQL exception indicates potential compatibility issues with Oracle's JSON support
- **MySQL/PostgreSQL**: VertxException and MySQLException need deeper investigation

## Recommendations

### For Immediate Use
1. **DO NOT use** JSON/JSONB columns in production until runtime issues are resolved
2. **Alternative**: Use TEXT/CLOB columns with manual JSON serialization/deserialization

### For Future Investigation
1. **Test with JDBC directly**: Verify if JSON/JSONB works with Exposed over JDBC (non-reactive)
2. **Driver versions**: Check if newer Vert.x driver versions have better JSON support
3. **Exposed version**: Monitor Exposed releases for improved reactive driver compatibility
4. **Manual serialization**: Consider implementing custom column types with explicit JSON handling

### Debugging Steps
1. Enable SQL logging to see generated DDL and DML statements
2. Test table creation separately from data operations
3. Verify JSON serialization format matches database expectations
4. Check if databases actually support JSON/JSONB in the versions used by testcontainers

## Conclusion

**JSON and JSONB column types are supported by Exposed and compile successfully with exposed-vertx-sql-client, but fail at runtime due to driver or serialization compatibility issues.**

The existing APIs (insert, select, update, delete) work correctly with the JSON/JSONB column definitions from a code perspective. The failure appears to be at the database driver level when attempting to:
1. Create tables with JSON/JSONB columns
2. Serialize data to JSON format
3. Deserialize JSON data back to Kotlin objects

**Status**: ⚠️ **PARTIALLY WORKING** - Compiles but does not run successfully

**Next Steps**: Requires deeper investigation into Vert.x driver compatibility with Exposed's JSON serialization mechanism.
