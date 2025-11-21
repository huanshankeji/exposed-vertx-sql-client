package com.huanshankeji.exposedvertxsqlclient

import java.util.*

class SimpleMappingTests : TestsForAllRdbmsTypesAndAllClientTypesWithTestcontainers({ databaseClient, rdbmsType ->
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
            crudMapperExtensions(databaseClient)
        }
    }
}, enabledRdbmsTypes = EnumSet.of(RdbmsType.Mssql))