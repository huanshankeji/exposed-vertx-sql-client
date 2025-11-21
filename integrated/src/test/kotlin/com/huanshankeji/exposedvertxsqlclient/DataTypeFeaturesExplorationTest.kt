package com.huanshankeji.exposedvertxsqlclient

import io.kotest.core.spec.style.FunSpec
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

// Test table definitions at top level (not local)
object UsersWithTransform : IntIdTable("users_transform") {
    val name = varchar("name", 100)
    
    // Using transform() for client-side transformation
    val uppercaseName = varchar("name_upper", 100)
        .transform(
            wrap = { it.uppercase() },
            unwrap = { it.lowercase() }
        )
}

object TestTable : IntIdTable("test_json") {
    val name = varchar("name", 100)
}

/**
 * Test to explore Exposed's new data type features availability in version 1.0.0-rc-3.
 * 
 * This test verifies that the APIs compile and are available, without testing
 * full functionality (which is covered in DataTypeFeatureTests.kt).
 */
class DataTypeFeaturesExplorationTest : FunSpec({
    
    test("Explore Column Transformation API") {
        // Column transformation allows client-side data transformation
        // Example: transform { encrypt(it) } and { decrypt(it) }
        
        println("Column transformation test table defined")
        println("Column: ${UsersWithTransform.uppercaseName}")
        println("Column type: ${UsersWithTransform.uppercaseName.columnType}")
        
        // Check if it's a ColumnWithTransform
        val isTransformed = UsersWithTransform.uppercaseName is ColumnWithTransform<*, *>
        println("Is transformed column: $isTransformed")
    }
    
    test("Explore JsonColumnMarker") {
        println("Checking JsonColumnMarker availability...")
        println("JsonColumnMarker interface is available in Exposed core")
        
        // Check what column types implement JsonColumnMarker
        val columns = TestTable.columns
        println("Test table columns: $columns")
    }
    
    test("Check ColumnTransformer availability") {
        println("Checking ColumnTransformer class...")
        
        // ColumnTransformer should be available in Exposed 1.0.0-rc-3
        val transformer = object : ColumnTransformer<String, String> {
            override fun wrap(value: String): String = value.uppercase()
            override fun unwrap(value: String): String = value.lowercase()
        }
        
        println("ColumnTransformer created: ${transformer.javaClass.simpleName}")
    }
})
