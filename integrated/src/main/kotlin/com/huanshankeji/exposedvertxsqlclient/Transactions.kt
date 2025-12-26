@file:OptIn(ExperimentalEvscApi::class)

package com.huanshankeji.exposedvertxsqlclient

import com.huanshankeji.exposedvertxsqlclient.crud.insert
import com.huanshankeji.exposedvertxsqlclient.crud.select
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.SqlConnection
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll

suspend fun basicTransactionForPool(
    databaseClient: DatabaseClient<Pool>
) {
    // Test withTransaction for Pool
    databaseClient.withTransaction { transactionClient ->
        transactionClient.insert(Examples) { it[name] = "TxTest1" }
        transactionClient.insert(Examples) { it[name] = "TxTest2" }
    }

    // Verify data was committed
    val count = databaseClient.select(Examples, { select(Examples.name).where(Examples.name eq "TxTest1") }).count()
    assert(count == 1) { "Expected 1 row after transaction commit, got $count" }
}

suspend fun basicTransactionForConnection(
    databaseClient: DatabaseClient<SqlConnection>
) {
    // Test withTransaction for SqlConnection
    databaseClient.withTransaction { transactionClient ->
        transactionClient.insert(Examples) { it[name] = "ConnTxTest" }
    }

    // Verify data was committed
    val count = databaseClient.select(Examples, { select(Examples.name).where(Examples.name eq "ConnTxTest") }).count()
    assert(count == 1) { "Expected 1 row after connection transaction, got $count" }
}

suspend fun transactionRollbackOnException(
    databaseClient: DatabaseClient<SqlConnection>
) {
    // Test that exception causes rollback
    try {
        databaseClient.withTransaction { transactionClient ->
            transactionClient.insert(Examples) { it[name] = "ExceptionTest" }
            throw RuntimeException("Test exception")
        }
    } catch (e: RuntimeException) {
        // Expected exception
    }

    // Verify data was rolled back
    val count =
        databaseClient.select(Examples, { select(Examples.name).where(Examples.name eq "ExceptionTest") }).count()
    assert(count == 0) { "Expected 0 rows after exception rollback, got $count" }
}

suspend fun polymorphicTransaction(
    databaseClient: DatabaseClient<*>
) {
    // Test withTransactionPolymorphic which works with both Pool and SqlConnection
    databaseClient.withTransactionPolymorphic { transactionClient ->
        transactionClient.insert(Examples) { it[name] = "PolyTest" }
    }

    val count = databaseClient.select(Examples, { select(Examples.name).where(Examples.name eq "PolyTest") }).count()
    assert(count == 1) { "Expected 1 row after polymorphic transaction, got $count" }
}

suspend fun savepointOperations(
    databaseClient: DatabaseClient<SqlConnection>
) {
    // Test withSavepointAndRollbackIfThrows - successful case
    databaseClient.withTransaction { transactionClient ->
        transactionClient.insert(Examples) { it[name] = "BeforeSavepoint" }

        transactionClient.withSavepointAndRollbackIfThrows("sp1") { savepointClient ->
            savepointClient.insert(Examples) { it[name] = "InSavepoint" }
        }

        // Both inserts should be visible in transaction
        val count = transactionClient.select(Examples, { selectAll() }).count()
        assert(count == 2) { "Expected 2 rows in transaction, got $count" }
    }

    // Verify data was committed
    val commitCount = databaseClient.select(Examples, { selectAll() }).count()
    assert(commitCount == 2) { "Expected 2 rows after commit, got $commitCount" }
}

suspend fun savepointRollbackOnException(
    databaseClient: DatabaseClient<SqlConnection>
) {
    databaseClient.withTransaction { transactionClient ->
        transactionClient.insert(Examples) { it[name] = "BeforeSavepointRollback" }

        try {
            transactionClient.withSavepointAndRollbackIfThrows("sp2") { savepointClient ->
                savepointClient.insert(Examples) { it[name] = "RolledBackInSavepoint" }
                throw RuntimeException("Savepoint test exception")
            }
        } catch (e: RuntimeException) {
            // Expected exception
        }

        // Only the first insert should be visible
        val count = transactionClient.select(Examples, { selectAll() }).count()
        assert(count == 1) { "Expected 1 row after savepoint rollback, got $count" }
    }

    // Verify only first insert was committed
    val commitCount = databaseClient.select(Examples, { selectAll() }).count()
    assert(commitCount == 1) { "Expected 1 row after commit, got $commitCount" }
}

suspend fun savepointWithFalse(
    databaseClient: DatabaseClient<SqlConnection>
) {
    databaseClient.withTransaction { transactionClient ->
        transactionClient.insert(Examples) { it[name] = "BeforeSavepointFalse" }

        val result = transactionClient.withSavepointAndRollbackIfThrowsOrFalse("sp4") { savepointClient ->
            savepointClient.insert(Examples) { it[name] = "FalseTest" }
            false
        }

        assert(!result) { "Expected false, got $result" }

        // Only the first insert should be visible (savepoint rolled back)
        val count = transactionClient.select(Examples, { selectAll() }).count()
        assert(count == 1) { "Expected 1 row after savepoint false, got $count" }
    }
}
