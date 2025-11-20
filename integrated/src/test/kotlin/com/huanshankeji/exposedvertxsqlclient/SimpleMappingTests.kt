package com.huanshankeji.exposedvertxsqlclient

class SimpleMappingTests : TestsForAllRdbmsTypesAndAllClientTypesWithTestcontainers({ databaseClient, rdbmsType ->
    test("test CRUD mapper extensions") {
        withMappingTables { crudMapperExtensions(databaseClient) }
    }
})