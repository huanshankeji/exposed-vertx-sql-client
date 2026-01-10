package com.huanshankeji.exposedvertxsqlclient.integrated

class ExamplesAdditionalTests :
    TestsForAllRdbmsTypesAndAllClientTypesWithTestcontainers({ databaseClient, rdbmsType, _ ->
        val crudSupportConfig = CrudSupportConfig.fromRdbmsType(rdbmsType)

        test("test batch operations") {
            withTables { batchOperations(databaseClient, crudSupportConfig, rdbmsType) }
        }

        // There is no `AUTO_INCREMENT` in Oracle and so Exposed doesn't support this properly yet.
        if (rdbmsType != RdbmsType.Oracle) {
            test("test insertSelect operations") {
                withTables { insertSelectOperations(databaseClient, crudSupportConfig) }
            }

            test("test batchInsertSelect operations") {
                withTables { batchInsertSelectOperations(databaseClient) }
            }
        }

        /*
        Batch select seems not supported by the Oracle client.
        ```
        io.vertx.oracleclient.OracleException: ORA-17129: SQL string is not a DML Statement.
        https://docs.oracle.com/error-help/db/ora-17129/
        ```
         */
        if (rdbmsType != RdbmsType.Oracle)
            test("test selectBatch operations") {
                withTables { selectBatchOperations(databaseClient) }
            }

        test("test select operation shortcuts") {
            withTables { selectOperationShortcuts(databaseClient) }
        }
    })
