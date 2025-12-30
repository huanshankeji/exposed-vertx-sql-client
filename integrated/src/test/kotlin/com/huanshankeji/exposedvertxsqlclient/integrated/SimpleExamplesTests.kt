package com.huanshankeji.exposedvertxsqlclient.integrated

class SimpleExamplesTests :
    TestsForAllRdbmsTypesAndAllClientTypesWithTestcontainers({ databaseClient, rdbmsType ->
        val crudSupportConfig = CrudSupportConfig.fromRdbmsType(rdbmsType)
        test("test CRUD with Statements") {
            withTables { crudWithStatements(databaseClient, crudSupportConfig) }
        }
        test("test CRUD extensions") {
            withTables { crudExtensions(databaseClient, crudSupportConfig) }
        }
    })