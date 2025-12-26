package com.huanshankeji.exposedvertxsqlclient

import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.SqlConnection

class TransactionTests :
    TestsForAllRdbmsTypesAndAllClientTypesWithTestcontainers({ databaseClient, _ ->
        // Transaction tests for Pool
        context("Pool transactions") {
            test("test basic transaction for Pool") {
                if (databaseClient.vertxSqlClient is Pool) {
                    withTables {
                        @Suppress("UNCHECKED_CAST")
                        basicTransactionForPool(databaseClient as DatabaseClient<Pool>)
                    }
                }
            }
        }
        
        // Transaction tests for SqlConnection
        context("SqlConnection transactions") {
            test("test basic transaction for Connection") {
                if (databaseClient.vertxSqlClient is SqlConnection) {
                    withTables {
                        @Suppress("UNCHECKED_CAST")
                        basicTransactionForConnection(databaseClient as DatabaseClient<SqlConnection>)
                    }
                }
            }
            
            test("test transaction rollback on exception") {
                if (databaseClient.vertxSqlClient is SqlConnection) {
                    withTables {
                        @Suppress("UNCHECKED_CAST")
                        transactionRollbackOnException(databaseClient as DatabaseClient<SqlConnection>)
                    }
                }
            }
            
            test("test savepoint operations") {
                if (databaseClient.vertxSqlClient is SqlConnection) {
                    withTables {
                        @Suppress("UNCHECKED_CAST")
                        savepointOperations(databaseClient as DatabaseClient<SqlConnection>)
                    }
                }
            }
            
            test("test savepoint rollback on exception") {
                if (databaseClient.vertxSqlClient is SqlConnection) {
                    withTables {
                        @Suppress("UNCHECKED_CAST")
                        savepointRollbackOnException(databaseClient as DatabaseClient<SqlConnection>)
                    }
                }
            }
            
            test("test savepoint with false") {
                if (databaseClient.vertxSqlClient is SqlConnection) {
                    withTables {
                        @Suppress("UNCHECKED_CAST")
                        savepointWithFalse(databaseClient as DatabaseClient<SqlConnection>)
                    }
                }
            }
        }
        
        // Polymorphic transaction tests (work with both Pool and SqlConnection)
        test("test polymorphic transaction") {
            withTables { polymorphicTransaction(databaseClient) }
        }
    })
