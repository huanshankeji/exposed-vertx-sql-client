package com.huanshankeji.exposedvertxsqlclient

class ExamplesAdditionalTests :
    TestsForAllRdbmsTypesAndAllClientTypesWithTestcontainers({ databaseClient, rdbmsType ->
        val crudSupportConfig = CrudSupportConfig.fromRdbmsType(rdbmsType)

        test("test batch operations") {
            withTables { batchOperations(databaseClient, crudSupportConfig) }
        }

        test("test insertSelect operations") {
            withTables { insertSelectOperations(databaseClient, crudSupportConfig) }
        }

        test("test batchInsertSelect operations") {
            withTables { batchInsertSelectOperations(databaseClient) }
        }

        test("test selectBatch operations") {
            withTables { selectBatchOperations(databaseClient) }
        }

        test("test select operation shortcuts") {
            withTables { selectOperationShortcuts(databaseClient) }
        }
    })
