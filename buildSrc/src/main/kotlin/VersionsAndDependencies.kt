import com.huanshankeji.CommonDependencies
import com.huanshankeji.CommonGradleClasspathDependencies
import com.huanshankeji.CommonVersions

val projectVersion = "0.6.0-SNAPSHOT"

// TODO don't use a snapshot version in a main branch
val commonVersions = CommonVersions(kotlinCommon = "0.7.0", exposed = "1.0.0-rc-3")
val commonDependencies = CommonDependencies(commonVersions)
val commonGradleClasspathDependencies = CommonGradleClasspathDependencies(commonVersions)

object DependencyVersions {
    val exposedGadtMapping = "0.4.0" // don't use a snapshot version in a main branch

    // https://github.com/mysql/mysql-connector-j/tags
    val mysqlConnectorJ = "9.5.0"

    // https://mvnrepository.com/artifact/com.oracle.database.jdbc/ojdbc11
    val oracleJdbc = "23.6.0.24.10"

    // https://mvnrepository.com/artifact/com.microsoft.sqlserver/mssql-jdbc
    val mssqlJdbc = "12.10.0.jre11"
}
