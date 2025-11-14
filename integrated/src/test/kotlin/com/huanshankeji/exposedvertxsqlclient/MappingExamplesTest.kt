@file:OptIn(ExperimentalEvscApi::class)

package com.huanshankeji.exposedvertxsqlclient

import com.huanshankeji.exposed.datamapping.classproperty.PropertyColumnMappingConfig
import com.huanshankeji.exposed.datamapping.classproperty.reflectionBasedClassPropertyDataMapper
import com.huanshankeji.exposedvertxsqlclient.crud.mapping.insertWithMapper
import com.huanshankeji.exposedvertxsqlclient.crud.mapping.selectWithMapper
import com.huanshankeji.exposedvertxsqlclient.mysql.MysqlDatabaseClientConfig
import com.huanshankeji.exposedvertxsqlclient.mysql.exposed.exposedDatabaseConnectMysql
import com.huanshankeji.exposedvertxsqlclient.mysql.vertx.mysqlclient.createMysqlPool
import com.huanshankeji.exposedvertxsqlclient.postgresql.PgDatabaseClientConfig
import com.huanshankeji.exposedvertxsqlclient.postgresql.exposed.exposedDatabaseConnectPostgresql
import com.huanshankeji.exposedvertxsqlclient.postgresql.vertx.pgclient.createPgPool
import io.vertx.core.Vertx
import io.vertx.sqlclient.Pool
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals

// copied and adapted from https://github.com/huanshankeji/exposed-gadt-mapping/blob/main/lib/src/test/kotlin/com/huanshankeji/exposed/datamapping/classproperty/Examples.kt
// Update accordingly to keep the code consistent.
// TODO also consider publishing the "exposed-gadt-mapping" example code as a library and depend on it

object Directors : IntIdTable("directors") {
    val directorId = id
    val name = varchar("name", 50)
}

object Films : IntIdTable() {
    val filmId = id
    val sequelId = integer("sequel_id").uniqueIndex()
    val name = varchar("name", 50)
    val directorId = integer("director_id").references(Directors.directorId)
}

val filmsLeftJoinDirectors = Films leftJoin Directors

typealias DirectorId = Int

class Director(val directorId: DirectorId, val name: String)

class FilmDetails<DirectorT>(
    val sequelId: Int,
    val name: String,
    val director: DirectorT
)
typealias FilmDetailsWithDirectorId = FilmDetails<DirectorId>

typealias FilmId = Int

class Film<DirectorT>(val filmId: FilmId, val filmDetails: FilmDetails<DirectorT>)
typealias FilmWithDirectorId = Film<DirectorId>
typealias FullFilm = Film<Director>

object Mappers {
    val director = reflectionBasedClassPropertyDataMapper<Director>(Directors)
    val filmDetailsWithDirectorId = reflectionBasedClassPropertyDataMapper<FilmDetailsWithDirectorId>(
        Films,
        propertyColumnMappingConfigMapOverride = mapOf(
            // The default name is the property name "director", but there is no column property with such a name, therefore we need to pass a custom name.
            FilmDetailsWithDirectorId::director to PropertyColumnMappingConfig.create<DirectorId>(columnPropertyName = Films::directorId.name)
        )
    )
    val filmWithDirectorId = reflectionBasedClassPropertyDataMapper<FilmWithDirectorId>(
        Films,
        propertyColumnMappingConfigMapOverride = mapOf(
            FilmWithDirectorId::filmDetails to PropertyColumnMappingConfig.create<FilmDetailsWithDirectorId>(
                // You can pass a nested custom mapper.
                customMapper = filmDetailsWithDirectorId
            )
        )
    )
    val fullFilm = reflectionBasedClassPropertyDataMapper<FullFilm>(
        filmsLeftJoinDirectors,
        propertyColumnMappingConfigMapOverride = mapOf(
            FullFilm::filmDetails to PropertyColumnMappingConfig.create(
                adt = PropertyColumnMappingConfig.Adt.Product(
                    mapOf(
                        // Because `name` is a duplicate name column so a custom mapper has to be passed here, otherwise the `CHOOSE_FIRST` option maps the data property `Director::name` to the wrong column `Films::name`.
                        FilmDetails<Director>::director to PropertyColumnMappingConfig.create<Director>(customMapper = director)
                    )
                )
            )
        )
    )
}

class MappingExamplesTest {
    private lateinit var container: JdbcDatabaseContainer<*>
    private lateinit var vertx: Vertx
    private lateinit var exposedDatabase: Database
    private lateinit var pool: Pool
    private lateinit var databaseClient: DatabaseClient<Pool>

    private fun setUp(type: DbType) {
        container = when (type) {
            DbType.POSTGRESQL -> PostgreSQLContainer(DockerImageName.parse("postgres:latest"))
            DbType.MYSQL -> MySQLContainer(DockerImageName.parse("mysql:latest"))
        }
        container.start()

        val evscConfig = ConnectionConfig.Socket(
            container.host,
            container.firstMappedPort,
            container.username,
            container.password,
            container.databaseName
        ).toUniversalEvscConfig()

        vertx = Vertx.vertx()

        when (type) {
            DbType.POSTGRESQL -> {
                exposedDatabase = evscConfig.exposedConnectionConfig.exposedDatabaseConnectPostgresql()
                pool = createPgPool(vertx, evscConfig.vertxSqlClientConnectionConfig)
                databaseClient = DatabaseClient(pool, exposedDatabase, PgDatabaseClientConfig())
            }
            DbType.MYSQL -> {
                exposedDatabase = evscConfig.exposedConnectionConfig.exposedDatabaseConnectMysql()
                pool = createMysqlPool(vertx, evscConfig.vertxSqlClientConnectionConfig)
                databaseClient = DatabaseClient(pool, exposedDatabase, MysqlDatabaseClientConfig())
            }
        }

        transaction(exposedDatabase) {
            SchemaUtils.create(Directors, Films)
        }
    }

    private fun tearDown() {
        pool.close()
        vertx.close().toCompletionStage().toCompletableFuture().get(30, TimeUnit.SECONDS)
        container.stop()
    }

    @Test
    fun testMappingExamplesPostgreSQL() = runTest {
        setUp(DbType.POSTGRESQL)
        try {
            testMappingExamples()
        } finally {
            tearDown()
        }
    }

    @Test
    fun testMappingExamplesMySQL() = runTest {
        setUp(DbType.MYSQL)
        try {
            testMappingExamples()
        } finally {
            tearDown()
        }
    }

    private suspend fun testMappingExamples() {
        val directorId = 1
        val director = Director(directorId, "George Lucas")
        databaseClient.insertWithMapper(Directors, director, Mappers.director)

        val episodeIFilmDetails = FilmDetails(1, "Star Wars: Episode I – The Phantom Menace", directorId)
        // insert without the ID since it's `AUTO_INCREMENT`
        databaseClient.insertWithMapper(Films, episodeIFilmDetails, Mappers.filmDetailsWithDirectorId)

        val filmId = 2
        val episodeIIFilmDetails = FilmDetails(2, "Star Wars: Episode II – Attack of the Clones", directorId)
        val filmWithDirectorId = FilmWithDirectorId(filmId, episodeIIFilmDetails)
        databaseClient.insertWithMapper(Films, filmWithDirectorId, Mappers.filmWithDirectorId) // insert with the ID

        val fullFilmsRowSet = databaseClient.selectWithMapper(filmsLeftJoinDirectors, Mappers.fullFilm) {
            where(Films.filmId inList listOf(1, 2))
        }
        val fullFilms = fullFilmsRowSet.toList()

        assertEquals(2, fullFilms.size)
        assertEquals("George Lucas", fullFilms[0].filmDetails.director.name)
        assertEquals("Star Wars: Episode I – The Phantom Menace", fullFilms[0].filmDetails.name)
        assertEquals("George Lucas", fullFilms[1].filmDetails.director.name)
        assertEquals("Star Wars: Episode II – Attack of the Clones", fullFilms[1].filmDetails.name)
    }
}
