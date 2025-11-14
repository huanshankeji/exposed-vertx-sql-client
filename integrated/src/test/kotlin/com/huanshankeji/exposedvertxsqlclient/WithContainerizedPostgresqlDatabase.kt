package com.huanshankeji.exposedvertxsqlclient

import com.huanshankeji.exposedvertxsqlclient.postgresql.PgDatabaseClientConfig
import com.huanshankeji.exposedvertxsqlclient.postgresql.exposed.exposedDatabaseConnectPostgresql
import com.huanshankeji.exposedvertxsqlclient.postgresql.vertx.pgclient.createPgPool
import io.vertx.core.Vertx
import io.vertx.sqlclient.Pool
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@OptIn(ExperimentalEvscApi::class)
abstract class WithContainerizedPostgresqlDatabase {
    private val postgreSQLContainer = PostgreSQLContainer(DockerImageName.parse("postgres:latest"))
    protected lateinit var vertx: Vertx
    protected lateinit var exposedDatabase: Database
    protected lateinit var pool: Pool
    protected lateinit var databaseClient: DatabaseClient<Pool>

    private fun createEvscConfig() =
        with(postgreSQLContainer) { 
            ConnectionConfig.Socket(host, firstMappedPort, username, password, databaseName)
                .toUniversalEvscConfig()
        }

    @BeforeTest
    fun setUp() {
        postgreSQLContainer.start()
        val evscConfig = createEvscConfig()
        
        vertx = Vertx.vertx()
        exposedDatabase = evscConfig.exposedConnectionConfig.exposedDatabaseConnectPostgresql()
        pool = createPgPool(vertx, evscConfig.vertxSqlClientConnectionConfig)
        databaseClient = DatabaseClient(pool, exposedDatabase, PgDatabaseClientConfig())
    }

    @AfterTest
    fun tearDown() {
        pool.close()
        vertx.close().toCompletionStage().toCompletableFuture().get(30, TimeUnit.SECONDS)
        postgreSQLContainer.stop()
    }

    protected suspend fun createTables(vararg tables: org.jetbrains.exposed.v1.core.Table) {
        databaseClient.exposedTransaction {
            SchemaUtils.create(*tables)
        }
    }

    protected fun createTablesBlocking(vararg tables: org.jetbrains.exposed.v1.core.Table) {
        transaction(exposedDatabase) {
            SchemaUtils.create(*tables)
        }
    }
}
