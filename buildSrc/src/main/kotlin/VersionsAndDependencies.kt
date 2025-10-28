import com.huanshankeji.CommonDependencies
import com.huanshankeji.CommonGradleClasspathDependencies
import com.huanshankeji.CommonVersions

val projectVersion = "0.6.0-SNAPSHOT"

// TODO don't use a snapshot version in a main branch
val commonVersions = CommonVersions(kotlinCommon = "0.7.0")
val commonDependencies = CommonDependencies(commonVersions)
val commonGradleClasspathDependencies = CommonGradleClasspathDependencies(commonVersions)

object DependencyVersions {
    val exposedGadtMapping = "0.4.0" // don't use a snapshot version in a main branch

    // https://github.com/mysql/mysql-connector-j/tags
    val mysqlConnectorJ = "9.5.0"
    
    // https://github.com/kohlschutter/junixsocket/releases
    val junixsocket = "2.11.0"
}
