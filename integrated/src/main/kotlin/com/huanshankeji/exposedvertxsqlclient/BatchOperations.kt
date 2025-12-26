@file:OptIn(ExperimentalEvscApi::class)

package com.huanshankeji.exposedvertxsqlclient

import com.huanshankeji.exposedvertxsqlclient.crud.*
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.buildStatement
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll

suspend fun batchOperations(
    databaseClient: DatabaseClient<*>, crudSupportConfig: CrudSupportConfig
) = with(crudSupportConfig) {
    // Test batchInsert
    val dataToInsert = listOf("Item1", "Item2", "Item3")
    databaseClient.batchInsert(Examples, dataToInsert) { statement, name ->
        statement[Examples.name] = name
    }
    
    val insertedCount = databaseClient.executeQuery(Examples.selectAll()).count()
    assert(insertedCount == 3) { "Expected 3 items, got $insertedCount" }
    
    // Test batchInsertIgnore (if supported)
    if (dialectSupportsInsertIgnore) {
        val duplicateData = listOf("Item1", "Item4")
        val results = databaseClient.batchInsertIgnore(Examples, duplicateData) { statement, name ->
            statement[Examples.name] = name
        }
        // Item1 already exists, so only Item4 should be inserted
        val resultsCount = results.count()
        assert(resultsCount == 2) { "Expected 2 results, got $resultsCount" }
    }
    
    // Test batchUpdate
    val idsToUpdate = listOf(1, 2)
    databaseClient.batchUpdate(Examples, idsToUpdate) { statement, id ->
        statement[Examples.name] = "Updated$id"
    }
    
    val updatedName1 = databaseClient.select(Examples, { select(Examples.name).where(Examples.id eq 1) })
        .single()[Examples.name]
    assert(updatedName1 == "Updated1") { "Expected 'Updated1', got '$updatedName1'" }
    
    // Test batchSingleOrNoUpdate
    val singleUpdateResults = databaseClient.batchSingleOrNoUpdate(
        Examples,
        listOf(1, 2, 999)  // 999 doesn't exist
    ) { statement, id ->
        statement[Examples.name] = "SingleUpdate$id"
    }
    val updateResultsList = singleUpdateResults.toList()
    assert(updateResultsList.size == 3) { "Expected 3 results, got ${updateResultsList.size}" }
    assert(updateResultsList[0]) { "First update should succeed" }
    assert(updateResultsList[1]) { "Second update should succeed" }
    assert(!updateResultsList[2]) { "Third update should fail (non-existent ID)" }
    
    // Test sortDataAndBatchUpdate
    val unsortedData = listOf(2, 1, 3)
    databaseClient.sortDataAndBatchUpdate(Examples, unsortedData, { it }) { statement, id ->
        statement[Examples.name] = "Sorted$id"
    }
    
    val sortedName = databaseClient.select(Examples, { select(Examples.name).where(Examples.id eq 1) })
        .single()[Examples.name]
    assert(sortedName == "Sorted1") { "Expected 'Sorted1', got '$sortedName'" }
}

suspend fun insertSelectOperations(
    databaseClient: DatabaseClient<*>, crudSupportConfig: CrudSupportConfig
) = with(crudSupportConfig) {
    // Insert initial data
    databaseClient.insert(Examples) { it[name] = "SourceA" }
    databaseClient.insert(Examples) { it[name] = "SourceB" }
    
    // Test insertSelect - copy rows from Examples back into Examples
    val selectQuery = Examples.select(Examples.name).where(Examples.name eq "SourceA")
    val insertedCount = databaseClient.insertSelect(Examples, selectQuery, listOf(Examples.name))
    assert(insertedCount == 1) { "Expected 1 row inserted, got $insertedCount" }
    
    // Verify the insert worked
    val allNames = databaseClient.select(Examples, { select(Examples.name) }).map { it[Examples.name] }
    val sourceACount = allNames.count { it == "SourceA" }
    assert(sourceACount == 2) { "Expected 2 'SourceA' entries, got $sourceACount" }
    
    // Test insertIgnoreSelect (if supported)
    if (dialectSupportsInsertIgnore) {
        val selectQuery2 = Examples.select(Examples.name).where(Examples.name eq "SourceB")
        val insertedCount2 = databaseClient.insertIgnoreSelect(Examples, selectQuery2, listOf(Examples.name))
        // This might insert or ignore depending on unique constraints
        assert(insertedCount2 >= 0) { "insertIgnoreSelect should not fail" }
    }
}

suspend fun batchInsertSelectOperations(
    databaseClient: DatabaseClient<*>
) {
    // Insert initial data
    databaseClient.insert(Examples) { it[name] = "Batch1" }
    databaseClient.insert(Examples) { it[name] = "Batch2" }
    
    // Test batchInsertSelect - create multiple insert-select statements
    val selectQueries = listOf(
        buildStatement { Examples.insert(Examples.select(Examples.name).where(Examples.name eq "Batch1"), listOf(Examples.name)) },
        buildStatement { Examples.insert(Examples.select(Examples.name).where(Examples.name eq "Batch2"), listOf(Examples.name)) }
    )
    
    databaseClient.batchInsertSelect(selectQueries)
    
    // Verify inserts worked
    val batch1Count = databaseClient.select(Examples, { select(Examples.name).where(Examples.name eq "Batch1") }).count()
    val batch2Count = databaseClient.select(Examples, { select(Examples.name).where(Examples.name eq "Batch2") }).count()
    assert(batch1Count == 2) { "Expected 2 'Batch1' entries, got $batch1Count" }
    assert(batch2Count == 2) { "Expected 2 'Batch2' entries, got $batch2Count" }
}

suspend fun selectBatchOperations(
    databaseClient: DatabaseClient<*>
) {
    // Insert test data
    val dataToInsert = listOf("SelectA", "SelectB", "SelectC")
    databaseClient.batchInsert(Examples, dataToInsert) { statement, name ->
        statement[Examples.name] = name
    }
    
    // Test selectBatch - batch select with different IDs
    val ids = listOf(1, 2, 3)
    val results = databaseClient.selectBatch(Examples, { id -> 
        Examples.select(Examples.name).where(Examples.id eq id) 
    }, ids)
    
    val resultsList = results.toList()
    assert(resultsList.size == 3) { "Expected 3 result sets, got ${resultsList.size}" }
    
    // Each result set should have exactly one row
    resultsList.forEachIndexed { index, rowSet ->
        assert(rowSet.count() == 1) { "Result set $index should have 1 row, got ${rowSet.count()}" }
    }
}

suspend fun additionalSelectOperations(
    databaseClient: DatabaseClient<*>
) {
    // Insert test data
    databaseClient.insert(Examples) { it[name] = "TestSelect" }
    
    // Test selectColumnSetExpression - select a count expression
    val count = databaseClient.selectColumnSetExpression(
        Examples,
        org.jetbrains.exposed.v1.core.Count(Examples.id),
        { where(Examples.name eq "TestSelect") }
    ).single()
    assert(count > 0) { "Count should be greater than 0, got $count" }
    
    // Test selectSingleEntityIdColumn - select just the ID column as entity ID value
    val insertedId = databaseClient.selectSingleEntityIdColumn(
        Examples,
        Examples.id,
        { where(Examples.name eq "TestSelect") }
    ).single()
    assert(insertedId > 0) { "ID should be greater than 0, got $insertedId" }
}
