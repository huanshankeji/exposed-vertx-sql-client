package com.huanshankeji.exposedvertxsqlclient

import com.huanshankeji.exposedvertxsqlclient.crud.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json as KotlinxJson
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.json.json
import org.jetbrains.exposed.v1.json.jsonb
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.*

/**
 * Test JSON and JSONB column type support.
 * 
 * According to Exposed documentation (https://www.jetbrains.com/help/exposed/json-and-jsonb-types.html),
 * Exposed provides json() and jsonb() column types that work with kotlinx.serialization.
 * 
 * - JSON is supported by PostgreSQL, MySQL, Oracle, and SQL Server (2016+)
 * - JSONB is PostgreSQL-specific and offers better performance for queries
 */
class JsonTests : TestsForAllRdbmsTypesAndAllClientTypesWithTestcontainers(
    { databaseClient, rdbmsType ->
        test("test JSON column CRUD operations") {
            withJsonTables(rdbmsType) {
                testJsonCrud(databaseClient, rdbmsType)
            }
        }

        // JSONB is PostgreSQL-specific
        if (rdbmsType == RdbmsType.Postgresql) {
            test("test JSONB column CRUD operations") {
                withJsonbTables {
                    testJsonbCrud(databaseClient)
                }
            }
        }
    },
    // Enable all supported databases for JSON testing
    enabledRdbmsTypes = EnumSet.allOf(RdbmsType::class.java)
)

@Serializable
data class UserData(val name: String, val age: Int, val email: String? = null)

@Serializable
data class Address(val street: String, val city: String, val zipCode: String)

@Serializable
data class ComplexData(
    val user: UserData,
    val addresses: List<Address>,
    val metadata: Map<String, String> = emptyMap()
)

// Table with JSON column (supported by PostgreSQL, MySQL, Oracle, MSSQL)
object JsonTable : IntIdTable("json_test") {
    val data = json<UserData>("data", KotlinxJson.Default::encodeToString, KotlinxJson.Default::decodeFromString)
    val complexData = json<ComplexData>("complex_data", KotlinxJson.Default::encodeToString, KotlinxJson.Default::decodeFromString).nullable()
}

// Table with JSONB column (PostgreSQL-specific)
object JsonbTable : IntIdTable("jsonb_test") {
    val data = jsonb<UserData>("data", KotlinxJson.Default::encodeToString, KotlinxJson.Default::decodeFromString)
    val complexData = jsonb<ComplexData>("complex_data", KotlinxJson.Default::encodeToString, KotlinxJson.Default::decodeFromString).nullable()
}

val jsonTables = arrayOf(JsonTable)
val jsonbTables = arrayOf(JsonbTable)

suspend fun withJsonTables(rdbmsType: RdbmsType, block: suspend () -> Unit) {
    transaction {
        SchemaUtils.create(*jsonTables)
    }
    try {
        block()
    } finally {
        transaction {
            SchemaUtils.drop(*jsonTables)
        }
    }
}

suspend fun withJsonbTables(block: suspend () -> Unit) {
    transaction {
        SchemaUtils.create(*jsonbTables)
    }
    try {
        block()
    } finally {
        transaction {
            SchemaUtils.drop(*jsonbTables)
        }
    }
}

@OptIn(ExperimentalEvscApi::class)
suspend fun testJsonCrud(databaseClient: DatabaseClient<*>, rdbmsType: RdbmsType) {
    // Insert test data
    val userData1 = UserData("Alice", 30, "alice@example.com")
    val userData2 = UserData("Bob", 25)
    
    val address1 = Address("123 Main St", "New York", "10001")
    val address2 = Address("456 Oak Ave", "San Francisco", "94102")
    val complexData = ComplexData(
        user = userData1,
        addresses = listOf(address1, address2),
        metadata = mapOf("status" to "active", "role" to "admin")
    )

    // Test insert
    databaseClient.insert(JsonTable) { 
        it[data] = userData1
        it[JsonTable.complexData] = complexData
    }
    databaseClient.insert(JsonTable) { 
        it[data] = userData2
        it[JsonTable.complexData] = null
    }

    // Test select all
    val allRows = databaseClient.executeQuery(JsonTable.selectAll())
    assert(allRows.size() == 2) { "Expected 2 rows, got ${allRows.size()}" }

    // Test select specific columns with where clause
    val aliceRows: io.vertx.sqlclient.RowSet<UserData> = databaseClient.selectSingleColumn(JsonTable, JsonTable.data, { 
        where(JsonTable.id eq 1)
    })
    assert(aliceRows.size() == 1) { "Expected 1 row for Alice, got ${aliceRows.size()}" }
    
    val aliceData: UserData = aliceRows.single()
    assert(aliceData.name == "Alice") { "Expected name 'Alice', got '${aliceData.name}'" }
    assert(aliceData.age == 30) { "Expected age 30, got ${aliceData.age}" }
    assert(aliceData.email == "alice@example.com") { "Expected email 'alice@example.com', got '${aliceData.email}'" }

    // Test complex data retrieval
    val aliceComplexDataRows: io.vertx.sqlclient.RowSet<ComplexData?> = databaseClient.selectSingleColumn(JsonTable, JsonTable.complexData, {
        where(JsonTable.id eq 1)
    })
    val aliceComplexData: ComplexData? = aliceComplexDataRows.single()
    assert(aliceComplexData != null) { "Expected complex data to be non-null" }
    assert(aliceComplexData!!.user.name == "Alice") { "Expected user name 'Alice' in complex data" }
    assert(aliceComplexData.addresses.size == 2) { "Expected 2 addresses, got ${aliceComplexData.addresses.size}" }
    assert(aliceComplexData.metadata["status"] == "active") { "Expected status 'active'" }

    // Test update
    val updatedUserData = UserData("Alice Smith", 31, "alice.smith@example.com")
    val updateCount = databaseClient.update(JsonTable, { JsonTable.id eq 1 }) {
        it[data] = updatedUserData
    }
    assert(updateCount == 1) { "Expected 1 row updated, got $updateCount" }

    // Verify update
    val updatedData: UserData = databaseClient.selectSingleColumn(JsonTable, JsonTable.data, { 
        where(JsonTable.id eq 1)
    }).single()
    assert(updatedData.name == "Alice Smith") { "Expected updated name 'Alice Smith', got '${updatedData.name}'" }
    assert(updatedData.age == 31) { "Expected updated age 31, got ${updatedData.age}" }

    // Test delete
    val deleteCount = databaseClient.deleteWhere(JsonTable) { id eq 2 }
    assert(deleteCount == 1) { "Expected 1 row deleted, got $deleteCount" }

    // Verify delete
    val remainingRows = databaseClient.executeQuery(JsonTable.selectAll())
    assert(remainingRows.size() == 1) { "Expected 1 remaining row, got ${remainingRows.size()}" }
}

@OptIn(ExperimentalEvscApi::class)
suspend fun testJsonbCrud(databaseClient: DatabaseClient<*>) {
    // Test JSONB (PostgreSQL-specific)
    val userData1 = UserData("Charlie", 28, "charlie@example.com")
    val userData2 = UserData("Diana", 32)
    
    val address = Address("789 Pine Rd", "Boston", "02101")
    val complexData = ComplexData(
        user = userData1,
        addresses = listOf(address),
        metadata = mapOf("department" to "engineering")
    )

    // Test insert
    databaseClient.insert(JsonbTable) { 
        it[data] = userData1
        it[JsonbTable.complexData] = complexData
    }
    databaseClient.insert(JsonbTable) { 
        it[data] = userData2
        it[JsonbTable.complexData] = null
    }

    // Test select
    val allRows = databaseClient.executeQuery(JsonbTable.selectAll())
    assert(allRows.size() == 2) { "Expected 2 rows, got ${allRows.size()}" }

    val charlieData: UserData = databaseClient.selectSingleColumn(JsonbTable, JsonbTable.data, { 
        where(JsonbTable.id eq 1)
    }).single()
    assert(charlieData.name == "Charlie") { "Expected name 'Charlie', got '${charlieData.name}'" }
    assert(charlieData.age == 28) { "Expected age 28, got ${charlieData.age}" }

    // Test complex data
    val charlieComplexData: ComplexData? = databaseClient.selectSingleColumn(JsonbTable, JsonbTable.complexData, {
        where(JsonbTable.id eq 1)
    }).single()
    assert(charlieComplexData != null) { "Expected complex data to be non-null" }
    assert(charlieComplexData!!.addresses.size == 1) { "Expected 1 address, got ${charlieComplexData.addresses.size}" }
    assert(charlieComplexData.metadata["department"] == "engineering") { "Expected department 'engineering'" }

    // Test update
    val updatedUserData = UserData("Charlie Brown", 29, "charlie.brown@example.com")
    val updateCount = databaseClient.update(JsonbTable, { JsonbTable.id eq 1 }) {
        it[data] = updatedUserData
    }
    assert(updateCount == 1) { "Expected 1 row updated, got $updateCount" }

    // Verify update
    val updatedData: UserData = databaseClient.selectSingleColumn(JsonbTable, JsonbTable.data, { 
        where(JsonbTable.id eq 1)
    }).single()
    assert(updatedData.name == "Charlie Brown") { "Expected updated name 'Charlie Brown', got '${updatedData.name}'" }

    // Test delete
    val deleteCount = databaseClient.deleteWhere(JsonbTable) { id eq 2 }
    assert(deleteCount == 1) { "Expected 1 row deleted, got $deleteCount" }
}
