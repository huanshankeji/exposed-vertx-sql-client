package com.huanshankeji.exposedvertxsqlclient.integrated

import com.huanshankeji.exposedvertxsqlclient.*
import io.kotest.core.spec.style.scopes.FunSpecContainerScope
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.SqlConnection
import java.util.*

@OptIn(ExperimentalEvscApi::class)
@Suppress("UNCHECKED_CAST")
class TransactionTests :
    TestsForAllRdbmsTypesAndAllClientTypesWithTestcontainers({ databaseClient, rdbmsType, sqlClientType ->
        suspend fun FunSpecContainerScope.contextTestsForTOREither(
            withTestEnv: suspend (test: suspend TransactionOrRollbackEitherTests<*>.() -> Unit) -> Unit,
            contextName: String
        ) =
            context(contextName) {
                test("test success") { withTestEnv { testSuccess() } }
                test("test explicit rollback") { withTestEnv { testExplicitRollback() } }
                test("test on exception") { withTestEnv { testOnException() } }
            }

        suspend fun FunSpecContainerScope.contextTestsForTOR(
            withTestEnv: suspend (test: suspend TransactionOrRollbackTests<*>.() -> Unit) -> Unit,
            contextName: String
        ) =
            context(contextName) {
                test("test success") { withTestEnv { testSuccess() } }
                test("test on exception") { withTestEnv { testOnException() } }
            }

        context("withTransaction") {
            suspend fun FunSpecContainerScope.contextTestsFor(
                tests: TransactionOrRollbackEitherTests<*>, contextName: String
            ) =
                contextTestsForTOREither({ test -> withTables { tests.test() } }, contextName)

            suspend fun FunSpecContainerScope.contextTestsFor(
                tests: TransactionOrRollbackTests<*>, contextName: String
            ) =
                contextTestsForTOR({ test -> withTables { tests.test() } }, contextName)

            when (sqlClientType) {
                SqlClientType.SqlConnection -> {
                    databaseClient as DatabaseClient<SqlConnection>
                    contextTestsFor(
                        TransactionOrRollbackEitherTests(
                            databaseClient, DatabaseClient<SqlConnection>::withTransactionEither
                        ),
                        "withTransactionEither"
                    )
                    contextTestsFor(
                        TransactionOrRollbackTests(databaseClient, DatabaseClient<SqlConnection>::withTransaction),
                        "withTransaction"
                    )
                }

                SqlClientType.Pool -> {
                    databaseClient as DatabaseClient<Pool>
                    contextTestsFor(
                        TransactionOrRollbackEitherTests(databaseClient, DatabaseClient<Pool>::withTransactionEither),
                        "withTransactionEither"
                    )
                    contextTestsFor(
                        TransactionOrRollbackTests(databaseClient, DatabaseClient<Pool>::withTransaction),
                        "withTransaction"
                    )

                    /*
                    contextTestsFor(
                        TransactionOrRollbackEitherTests(databaseClient, DatabaseClient<Pool>::withTypedTransactionEither),
                        "withTyped(Connection)TransactionEither"
                    )
                    */
                    contextTestsFor(
                        TransactionOrRollbackTests(databaseClient, DatabaseClient<Pool>::withTypedTransaction),
                        "withTyped(Connection)Transaction"
                    )
                    // Subtypes such as `PgConnection` have to be passed as reified parameters. They are only accessible at compile time and thus can't be tested generally here.
                }

                SqlClientType.Client -> throw IllegalArgumentException()
            }

            // polymorphic
            contextTestsFor(
                TransactionOrRollbackEitherTests(databaseClient, DatabaseClient<*>::withTransactionEitherPolymorphic),
                "withTransactionEitherPolymorphic"
            )
            contextTestsFor(
                TransactionOrRollbackTests(databaseClient, DatabaseClient<*>::withTransactionPolymorphic),
                "withTransactionPolymorphic"
            )
        }

        // Savepoints are only supported with PostgreSQL now. And we only test with `SqlConnection` here for simplicity.
        if (rdbmsType == RdbmsType.Postgresql && sqlClientType == SqlClientType.SqlConnection)
            context("withSavepoint") {
                databaseClient as DatabaseClient<SqlConnection>
                // For rigorousness, `TransactionOrRollbackEitherTests` and `TransactionOrRollbackTests` should be created from the `DatabaseClient` from `withTransaction`.
                suspend fun withTablesAndTransaction(function: suspend (DatabaseClient<SqlConnection>) -> Unit) =
                    withTables { databaseClient.withTransaction(function) }
                contextTestsForTOREither({ test ->
                    withTablesAndTransaction { databaseClient ->
                        TransactionOrRollbackEitherTests(
                            databaseClient, { function -> withSavepointEither("sp", function) }
                        ).test()
                    }
                }, "withSavepointEither")
                contextTestsForTOR({ test ->
                    withTablesAndTransaction { databaseClient ->
                        TransactionOrRollbackTests(
                            databaseClient, { function -> withSavepoint("sp", function) }
                        ).test()
                    }
                }, "withSavepoint")
            }
    }, enabledSqlClientTypes = EnumSet.of(SqlClientType.Pool, SqlClientType.SqlConnection))