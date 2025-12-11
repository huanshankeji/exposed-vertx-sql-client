@file:OptIn(ExperimentalEvscApi::class)

package com.huanshankeji.exposedvertxsqlclient

import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.sql.Connection

/**
 * Tests for [DatabaseClient.exposedReadOnlyTransaction] functionality
 * with [DatabaseClientConfig.readOnlyTransactionIsolationLevel].
 */
class ReadOnlyTransactionTests :
    TestsForAllRdbmsTypesAndAllClientTypesWithTestcontainers({ databaseClient, rdbmsType ->
        test("exposedReadOnlyTransaction can read data") {
            withTables {
                // Insert test data
                databaseClient.insert(Examples) { it[name] = "ReadOnlyTest1" }
                databaseClient.insert(Examples) { it[name] = "ReadOnlyTest2" }

                // Read data using exposedReadOnlyTransaction
                val results = databaseClient.exposedReadOnlyTransaction {
                    Examples.selectAll().toList()
                }

                // Verify we can read the data
                results.size shouldBe 2
                results.map { it[Examples.name] }.toSet() shouldBe setOf("ReadOnlyTest1", "ReadOnlyTest2")
            }
        }

        test("exposedReadOnlyTransaction with select query") {
            withTables {
                // Insert test data
                databaseClient.insert(Examples) { it[name] = "QueryTest" }

                // Execute a select query in read-only transaction
                val result = databaseClient.exposedReadOnlyTransaction {
                    Examples.select(Examples.name).where(Examples.id eq 1).single()
                }

                result[Examples.name] shouldBe "QueryTest"
            }
        }

        test("exposedReadOnlyTransaction isolation level is applied") {
            withTables {
                // This test verifies that the transaction runs without errors
                // Different databases may handle isolation levels differently, but the
                // transaction should complete successfully with the configured isolation level
                
                databaseClient.insert(Examples) { it[name] = "IsolationTest" }

                val count = databaseClient.exposedReadOnlyTransaction {
                    Examples.selectAll().count()
                }

                count shouldBe 1L
            }
        }

        test("exposedReadOnlyTransaction with custom isolation level") {
            withTables {
                // Test with a database client configured with a different isolation level
                val customConfig = when (rdbmsType) {
                    RdbmsType.Postgresql -> com.huanshankeji.exposedvertxsqlclient.postgresql.PgDatabaseClientConfig(
                        readOnlyTransactionIsolationLevel = Connection.TRANSACTION_READ_COMMITTED
                    )
                    RdbmsType.Mysql -> com.huanshankeji.exposedvertxsqlclient.mysql.MysqlDatabaseClientConfig(
                        readOnlyTransactionIsolationLevel = Connection.TRANSACTION_READ_COMMITTED
                    )
                    RdbmsType.Oracle -> com.huanshankeji.exposedvertxsqlclient.oracle.OracleDatabaseClientConfig(
                        readOnlyTransactionIsolationLevel = Connection.TRANSACTION_READ_COMMITTED
                    )
                    RdbmsType.Mssql -> com.huanshankeji.exposedvertxsqlclient.mssql.MssqlDatabaseClientConfig(
                        readOnlyTransactionIsolationLevel = Connection.TRANSACTION_READ_COMMITTED
                    )
                }

                val customClient = DatabaseClient(
                    databaseClient.vertxSqlClient,
                    databaseClient.exposedDatabase,
                    customConfig
                )

                // Insert test data with the original client
                databaseClient.insert(Examples) { it[name] = "CustomIsolationTest" }

                // Read with custom client using different isolation level
                val result = customClient.exposedReadOnlyTransaction {
                    Examples.selectAll().single()
                }

                result[Examples.name] shouldBe "CustomIsolationTest"
            }
        }

        test("exposedReadOnlyTransaction with null isolation level") {
            withTables {
                // Test with null isolation level (should use database default)
                val customConfig = when (rdbmsType) {
                    RdbmsType.Postgresql -> com.huanshankeji.exposedvertxsqlclient.postgresql.PgDatabaseClientConfig(
                        readOnlyTransactionIsolationLevel = null
                    )
                    RdbmsType.Mysql -> com.huanshankeji.exposedvertxsqlclient.mysql.MysqlDatabaseClientConfig(
                        readOnlyTransactionIsolationLevel = null
                    )
                    RdbmsType.Oracle -> com.huanshankeji.exposedvertxsqlclient.oracle.OracleDatabaseClientConfig(
                        readOnlyTransactionIsolationLevel = null
                    )
                    RdbmsType.Mssql -> com.huanshankeji.exposedvertxsqlclient.mssql.MssqlDatabaseClientConfig(
                        readOnlyTransactionIsolationLevel = null
                    )
                }

                val customClient = DatabaseClient(
                    databaseClient.vertxSqlClient,
                    databaseClient.exposedDatabase,
                    customConfig
                )

                databaseClient.insert(Examples) { it[name] = "NullIsolationTest" }

                val result = customClient.exposedReadOnlyTransaction {
                    Examples.selectAll().single()
                }

                result[Examples.name] shouldBe "NullIsolationTest"
            }
        }
    })
