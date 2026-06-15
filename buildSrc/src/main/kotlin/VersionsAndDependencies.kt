import com.huanshankeji.CommonDependencies
import com.huanshankeji.CommonGradleClasspathDependencies
import com.huanshankeji.CommonVersions

val projectBaseVersion = "0.8.2"

val gradleCommonPluginsVersion =
    "0.12.0-dev-commit-e2dcb9d3d327edd99d80a6dfa041fc47c2db7bbb-dirty-SNAPSHOT"

// Note that there is another Exposed version in the version catalog
val commonVersions =
    CommonVersions(kotlinCommon = "0.7.0", exposed = "1.1.1", testcontainers = "2.0.4", vertx = "5.0.10")
val commonDependencies = CommonDependencies(commonVersions)
val commonGradleClasspathDependencies = CommonGradleClasspathDependencies(commonVersions)

object DependencyVersions {
    val exposedGadtMapping = "0.4.0"

    // https://github.com/mysql/mysql-connector-j/tags
    val mysqlConnectorJ = "9.6.0"

    // https://mvnrepository.com/artifact/com.oracle.database.jdbc/ojdbc11
    // https://repo1.maven.org/maven2/com/oracle/database/jdbc/ojdbc11/
    val oracleJdbc = "23.26.1.0.0"

    // https://mvnrepository.com/artifact/com.microsoft.sqlserver/mssql-jdbc
    // https://github.com/microsoft/mssql-jdbc/releases
    val mssqlJdbc = "13.4.0.jre11"

    val hikaricp = "7.0.2"
}
