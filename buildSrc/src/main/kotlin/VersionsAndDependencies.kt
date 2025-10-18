import com.huanshankeji.CommonDependencies
import com.huanshankeji.CommonGradleClasspathDependencies
import com.huanshankeji.CommonVersions

val projectVersion = "0.6.0-SNAPSHOT"

// TODO don't use a snapshot version in a main branch
// TODO remove Exposed's explicit version when migration to Exposed 1.0.0 is complete
val commonVersions = CommonVersions(kotlinCommon = "0.7.0-SNAPSHOT", exposed = "0.61.0")
val commonDependencies = CommonDependencies(commonVersions)
val commonGradleClasspathDependencies = CommonGradleClasspathDependencies(commonVersions)

object DependencyVersions {
    val exposedAdtMapping = "0.4.0-SNAPSHOT" // TODO don't use a snapshot version in a main branch

    val mysqlConnectorJ = "9.4.0"
}
