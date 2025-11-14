package com.huanshankeji.exposedvertxsqlclient

import com.huanshankeji.exposedvertxsqlclient.mysql.MysqlDatabaseClientConfig
import com.huanshankeji.exposedvertxsqlclient.mysql.exposed.exposedDatabaseConnectMysql
import com.huanshankeji.exposedvertxsqlclient.mysql.vertx.mysqlclient.createMysqlPool
import io.vertx.core.Vertx
import io.vertx.sqlclient.Pool
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.utility.DockerImageName
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@OptIn(ExperimentalEvscApi::class)
abstract class WithContainerizedMysqlDatabase {
    private val mySQLContainer = MySQLContainer(DockerImageName.parse("mysql:latest"))
    protected lateinit var vertx: Vertx
    protected lateinit var exposedDatabase: Database
    protected lateinit var pool: Pool
    protected lateinit var databaseClient: DatabaseClient<Pool>

    private fun createEvscConfig() =
        with(mySQLContainer) {
            ConnectionConfig.Socket(host, firstMappedPort, username, password, databaseName)
                .toUniversalEvscConfig()
        }

    @BeforeTest
    fun setUp() {
        mySQLContainer.start()
        val evscConfig = createEvscConfig()
        
        vertx = Vertx.vertx()
        exposedDatabase = evscConfig.exposedConnectionConfig.exposedDatabaseConnectMysql()
        pool = createMysqlPool(vertx, evscConfig.vertxSqlClientConnectionConfig)
        databaseClient = DatabaseClient(pool, exposedDatabase, MysqlDatabaseClientConfig())
    }

    @AfterTest
    fun tearDown() {
        pool.close()
        vertx.close().toCompletionStage().toCompletableFuture().get(30, TimeUnit.SECONDS)
        mySQLContainer.stop()
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
