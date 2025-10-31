@file:OptIn(ExperimentalEvscApi::class)

package com.huanshankeji.exposedvertxsqlclient

import com.huanshankeji.exposedvertxsqlclient.crud.*
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.exists
import org.jetbrains.exposed.v1.core.statements.buildStatement
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import kotlin.test.Test
import kotlin.test.assertEquals

class ExamplesPostgresqlTest : WithContainerizedPostgresqlDatabase() {
    
    object Examples : IntIdTable("examples") {
        val name = varchar("name", 64)
    }

    @Test
    fun testExamplesWithBuildStatement() = runTest {
        createTables(Examples)

        // The Exposed `Table` extension functions `insert`, `update`, and `delete` execute eagerly so `insertStatement`, `updateStatement`, `deleteStatement` have to be used.

        val insertRowCount = databaseClient.executeUpdate(buildStatement { Examples.insert { it[name] = "A" } })
        assertEquals(1, insertRowCount)
        
        // `executeSingleUpdate` function requires that there is only 1 row updated and returns `Unit`.
        databaseClient.executeSingleUpdate(buildStatement { Examples.insert { it[name] = "B" } })
        
        // `executeSingleOrNoUpdate` requires that there is 0 or 1 row updated and returns `Boolean`.
        val isInserted =
            databaseClient.executeSingleOrNoUpdate(buildStatement { Examples.insertIgnore { it[name] = "B" } })
        assertEquals(true, isInserted)

        val updateRowCount =
            databaseClient.executeUpdate(buildStatement { Examples.update({ Examples.id eq 1 }) { it[name] = "AA" } })
        assertEquals(1, updateRowCount)

        // The Exposed `Table` extension function `select` doesn't execute eagerly so it can also be used directly.
        val exampleName = databaseClient.executeQuery(Examples.select(Examples.name).where(Examples.id eq 1))
            .single()[Examples.name]
        assertEquals("AA", exampleName)

        databaseClient.executeSingleUpdate(buildStatement { Examples.deleteWhere { id eq 1 } })
        databaseClient.executeSingleUpdate(buildStatement { Examples.deleteWhere { id eq 2 } })
    }

    @Test
    fun testExamplesWithCrudExtensions() = runTest {
        createTables(Examples)

        databaseClient.insert(Examples) { it[name] = "A" }
        val isInserted = databaseClient.insertIgnore(Examples) { it[name] = "B" }
        assertEquals(true, isInserted)

        val updateRowCount = databaseClient.update(Examples, { Examples.id eq 1 }) { it[name] = "AA" }
        assertEquals(1, updateRowCount)

        val exampleName1 =
            databaseClient.select(Examples) { select(Examples.name).where(Examples.id eq 1) }.single()[Examples.name]
        assertEquals("AA", exampleName1)
        
        val exampleName2 =
            databaseClient.selectSingleColumn(Examples, Examples.name) { where(Examples.id eq 2) }.single()
        assertEquals("B", exampleName2)

        val examplesExist = databaseClient.selectExpression(exists(Examples.selectAll()))
        assertEquals(true, examplesExist)

        val deleteRowCount1 = databaseClient.deleteWhere(Examples) { id eq 1 }
        assertEquals(1, deleteRowCount1)
        
        val deleteRowCount2 = databaseClient.deleteWhere(Examples) { id eq 2 }
        assertEquals(1, deleteRowCount2)
    }
}
