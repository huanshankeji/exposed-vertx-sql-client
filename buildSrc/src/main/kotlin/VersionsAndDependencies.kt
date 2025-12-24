import com.huanshankeji.CommonDependencies
import com.huanshankeji.CommonGradleClasspathDependencies
import com.huanshankeji.CommonVersions

val projectVersion = "0.7.0-SNAPSHOT"

// don't use a snapshot version in a main branch
// Note that there is another Exposed version in the version catelog
val commonVersions = CommonVersions(kotlinCommon = "0.7.0", exposed = "1.0.0-rc-4", testcontainers = "2.0.3")
val commonDependencies = CommonDependencies(commonVersions)
val commonGradleClasspathDependencies = CommonGradleClasspathDependencies(commonVersions)

object DependencyVersions {
    val exposedGadtMapping = "0.4.0" // don't use a snapshot version in a main branch

    // https://github.com/mysql/mysql-connector-j/tags
    val mysqlConnectorJ = "9.5.0"

    // https://mvnrepository.com/artifact/com.oracle.database.jdbc/ojdbc11
    // https://repo1.maven.org/maven2/com/oracle/database/jdbc/ojdbc11/
    val oracleJdbc = "23.26.0.0.0"

    // https://mvnrepository.com/artifact/com.microsoft.sqlserver/mssql-jdbc
    // https://github.com/microsoft/mssql-jdbc/releases
    val mssqlJdbc = "13.2.1.jre11"

    val hikaricp = "7.0.2"
}
