package com.huanshankeji.exposedvertxsqlclient

import io.kotest.core.spec.style.scopes.FunSpecContainerScope
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.SqlConnection

@OptIn(ExperimentalEvscApi::class)
class TransactionTests : TestsForAllRdbmsTypesAndAllClientTypesWithTestcontainers({ databaseClient, _ ->
    suspend fun FunSpecContainerScope.contextTestsFor(
        tests: TransactionOrRollbackEitherTests<*>,
        transactionOrRollback: String
    ) {
        context(transactionOrRollback) {
            test("test success") { tests.testSuccess() }
            test("test explicit rollback") { tests.testExplicitRollback() }
            test("test on exception") { tests.testOnException() }
        }
    }

    suspend fun FunSpecContainerScope.contextTestsFor(tests: TransactionOrRollbackTests<*>, name: String) {
        context(name) {
            test("test success") { tests.testSuccess() }
            test("test on exception") { tests.testOnException() }
        }
    }

    // `SqlConnection`
    databaseClient.withVertxSqlClientCheckedCastToOrNull<SqlConnection>()?.let { databaseClient ->
        contextTestsFor(
            TransactionOrRollbackEitherTests(databaseClient, DatabaseClient<SqlConnection>::withTransactionEither),
            "withTransactionEither"
        )
        contextTestsFor(
            TransactionOrRollbackTests(databaseClient, DatabaseClient<SqlConnection>::withTransaction),
            "withTransaction"
        )
    }

    // `Pool`
    databaseClient.withVertxSqlClientCheckedCastToOrNull<Pool>()?.let { databaseClient ->
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
            "withTypedConnectionTransactionEither"
        )
        */
        contextTestsFor(
            TransactionOrRollbackTests(databaseClient, DatabaseClient<Pool>::withTypedTransaction),
            "withTypedConnectionTransaction"
        )
        // Subtypes such as `PgConnection` have to be passed as reified parameters. They are only accessible at compile time and thus can't be tested generally here.
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
})