package com.huanshankeji.exposedvertxsqlclient.integrated

import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import com.huanshankeji.exposedvertxsqlclient.JdbcTransactionExposedTransactionProvider
import com.huanshankeji.testcontainers.LatestPostgreSQLContainer
import io.kotest.core.extensions.install
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.testcontainers.TestContainerSpecExtension
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.core.Schema
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.sql.Connection
import java.sql.DriverManager

// This test file was completely created by Codex.
@OptIn(ExperimentalEvscApi::class)
class JdbcTransactionExposedTransactionProviderTests : FunSpec({
    val container = install(TestContainerSpecExtension(LatestPostgreSQLContainer()))
    val connections = mutableListOf<Connection>()
    afterTest {
        connections.forEach { it.close() }
        connections.clear()
    }

    fun database(defaultSchema: Schema? = null) = Database.connect(
        getNewConnection = {
            DriverManager.getConnection(container.jdbcUrl, container.username, container.password).also {
                connections.add(it)
            }
        },
        databaseConfig = DatabaseConfig { this.defaultSchema = defaultSchema }
    )

    val table = object : Table("example") {
        val id = integer("id")
    }

    test("database creation, an empty transaction, and provider construction leave connection acquisition lazy") {
        val database = database()
        connections.size shouldBe 0
        transaction(database, Connection.TRANSACTION_READ_UNCOMMITTED, true) {}
        connections.size shouldBe 0
        val provider = JdbcTransactionExposedTransactionProvider.fromDatabaseTransaction(database)
        @Suppress("DEPRECATION")
        val legacyProvider = JdbcTransactionExposedTransactionProvider(database)
        connections.size shouldBe 0

        for (currentProvider in listOf(provider, legacyProvider)) {
            repeat(2) {
                currentProvider.statementPreparationExposedTransaction {
                    this shouldBeSameInstanceAs currentProvider.jdbcTransaction
                    table.selectAll().prepareSQL(this) shouldBe "SELECT example.id FROM example"
                }
            }
        }
        connections.size shouldBe 1
        JdbcTransactionExposedTransactionProvider(provider.jdbcTransaction).jdbcTransaction shouldBeSameInstanceAs
                provider.jdbcTransaction
    }

    test("a configured default schema performs JDBC I/O during provider initialization and closes the connection") {
        val provider = JdbcTransactionExposedTransactionProvider.fromDatabaseTransaction(database(Schema("public")))
        (connections.size > 0).shouldBeTrue()
        connections.forEach { it.isClosed.shouldBeTrue() }
        val connectionCount = connections.size

        repeat(2) {
            provider.statementPreparationExposedTransaction {
                this shouldBeSameInstanceAs provider.jdbcTransaction
                table.selectAll().prepareSQL(this) shouldBe "SELECT example.id FROM example"
            }
        }
        connections.size shouldBe connectionCount
    }
})