package com.huanshankeji.exposedvertxsqlclient

import com.huanshankeji.exposedvertxsqlclient.crud.insert
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.util.EnumSet

// Test table with column transformation
object ProductsWithTransform : IntIdTable("products_transform") {
    val name = varchar("name", 100)
    
    // Column transformation: store uppercase in DB, but work with regular case in code
    val normalizedName = varchar("normalized_name", 100)
        .transform(
            wrap = { it.uppercase() },  // Before storing in DB
            unwrap = { it.lowercase() }  // After reading from DB
        )
}

// Test table with custom column type
class UpperCaseVarcharColumnType(colLength: Int) : VarCharColumnType(colLength) {
    override fun notNullValueToDB(value: String): Any =
        super.notNullValueToDB(value.uppercase())
    
    override fun valueFromDB(value: Any): String =
        super.valueFromDB(value).lowercase()
}

object ProductsWithCustomType : IntIdTable("products_custom_type") {
    val name = varchar("name", 100)
    
    // Custom column type
    val customName = registerColumn<String>("custom_name", UpperCaseVarcharColumnType(100))
}

/**
 * Tests for Exposed data type features:
 * 1. Column Transformation
 * 2. Custom Column Types
 * 
 * These tests verify that exposed-vertx-sql-client correctly handles
 * these Exposed features without requiring special handling.
 */
class DataTypeFeatureTests : TestsForAllRdbmsTypesAndAllClientTypesWithTestcontainers({
    databaseClient, rdbmsType ->
    
    context("Column Transformation") {
        test("should handle transform() correctly") {
            databaseClient.exposedTransaction {
                SchemaUtils.create(ProductsWithTransform)
            }
            
            try {
                // Insert with regular case - should be stored as uppercase
                databaseClient.insert(ProductsWithTransform) {
                    it[name] = "Widget"
                    it[normalizedName] = "Widget"  // Will be transformed to "WIDGET" in DB
                }
                
                // Read back - should be transformed to lowercase
                val result = databaseClient.executeQuery(ProductsWithTransform.selectAll()).single()
                val nameValue = result[ProductsWithTransform.name]
                assert(nameValue == "Widget") { "Expected 'Widget' but got '$nameValue'" }
                
                // The normalizedName should be lowercase after unwrap
                // Note: This tests if Exposed's transform() works correctly with our library
                val normalizedValue = result[ProductsWithTransform.normalizedName]
                println("Normalized value from DB: $normalizedValue")
                
                // In DB it's uppercase, after unwrap it should be lowercase
                assert(normalizedValue == "widget") { "Expected 'widget' but got '$normalizedValue'" }
            } finally {
                databaseClient.exposedTransaction {
                    SchemaUtils.drop(ProductsWithTransform)
                }
            }
        }
    }
    
    context("Custom Column Types") {
        test("should handle custom column types correctly") {
            databaseClient.exposedTransaction {
                SchemaUtils.create(ProductsWithCustomType)
            }
            
            try {
                // Insert with mixed case
                databaseClient.insert(ProductsWithCustomType) {
                    it[name] = "Gadget"
                    it[customName] = "Gadget"  // Will be stored as "GADGET" via custom type
                }
                
                // Read back - custom type should convert to lowercase
                val result = databaseClient.executeQuery(ProductsWithCustomType.selectAll()).single()
                val nameValue = result[ProductsWithCustomType.name]
                assert(nameValue == "Gadget") { "Expected 'Gadget' but got '$nameValue'" }
                
                val customValue = result[ProductsWithCustomType.customName]
                println("Custom column value from DB: $customValue")
                
                // Custom type stores uppercase but returns lowercase
                assert(customValue == "gadget") { "Expected 'gadget' but got '$customValue'" }
            } finally {
                databaseClient.exposedTransaction {
                    SchemaUtils.drop(ProductsWithCustomType)
                }
            }
        }
    }
    
}, enabledRdbmsTypes = EnumSet.of(RdbmsType.Postgresql, RdbmsType.Mysql))
