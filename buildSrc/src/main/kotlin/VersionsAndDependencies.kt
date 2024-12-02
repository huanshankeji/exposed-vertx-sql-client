import com.huanshankeji.CommonDependencies
import com.huanshankeji.CommonGradleClasspathDependencies
import com.huanshankeji.CommonVersions

val projectVersion = "0.5.1-kotlin-2.1.0-SNAPSHOT"

// don't use a snapshot version in a main branch
val commonVersions = CommonVersions(kotlinCommon = "0.6.0", exposed = "0.56.0")
val commonDependencies = CommonDependencies(commonVersions)
val commonGradleClasspathDependencies = CommonGradleClasspathDependencies(commonVersions)

object DependencyVersions {
    val exposedAdtMapping = "0.3.0" // don't use a snapshot version in a main branch
}
