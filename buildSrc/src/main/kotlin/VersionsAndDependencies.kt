import com.huanshankeji.CommonDependencies
import com.huanshankeji.CommonGradleClasspathDependencies
import com.huanshankeji.CommonVersions

val projectBaseVersion = "0.8.2"
val isRelease = false

// Note that there is another Exposed version in the version catalog
val commonVersions =
    CommonVersions(kotlinCommon = "0.8.0-dev-commit-ca41bc43126155e03b26bf84db0eb62fcb504be7", exposed = "1.1.1", testcontainers = "2.0.4", vertx = "5.0.10")
val commonDependencies = CommonDependencies(commonVersions)
val commonGradleClasspathDependencies = CommonGradleClasspathDependencies(commonVersions)

object DependencyVersions {
    val exposedGadtMapping = "0.4.0-dev-commit-b12f5813124a22f8aff12b5c8d51989826ba7cd8"

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
