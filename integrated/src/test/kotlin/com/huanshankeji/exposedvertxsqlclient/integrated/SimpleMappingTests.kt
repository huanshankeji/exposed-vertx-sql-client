package com.huanshankeji.exposedvertxsqlclient.integrated

class SimpleMappingTests : AllConfigurationsSpec({ databaseClient, rdbmsType, _ ->
    test("test CRUD mapper extensions") {
        withMappingTables {
            /*
            // https://stackoverflow.com/questions/7063501/how-to-turn-identity-insert-on-and-off-using-sql-server-2008
            // doesn't work
            if (rdbmsType == RdbmsType.Mssql)
                databaseClient.executePlainSqlUpdate(
                    "SET IDENTITY_INSERT ${Directors.tableName} ON"
                )
            */
            crudMapperExtensions(databaseClient, rdbmsType != RdbmsType.Mssql)
        }
    }
})