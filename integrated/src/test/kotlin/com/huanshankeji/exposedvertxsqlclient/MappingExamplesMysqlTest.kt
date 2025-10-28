@file:OptIn(ExperimentalEvscApi::class)

package com.huanshankeji.exposedvertxsqlclient

import com.huanshankeji.exposed.datamapping.classproperty.PropertyColumnMappingConfig
import com.huanshankeji.exposed.datamapping.classproperty.reflectionBasedClassPropertyDataMapper
import com.huanshankeji.exposedvertxsqlclient.crud.mapping.insertWithMapper
import com.huanshankeji.exposedvertxsqlclient.crud.mapping.selectWithMapper
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.inList
import kotlin.test.Test
import kotlin.test.assertEquals

class MappingExamplesMysqlTest : WithContainerizedMysqlDatabase() {
    
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

    @Test
    fun testMappingExamples() = runTest {
        createTables(Directors, Films)

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
