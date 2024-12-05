import com.huanshankeji.CommonDependencies
import com.huanshankeji.CommonGradleClasspathDependencies
import com.huanshankeji.CommonVersions

val projectVersion = "0.5.1-SNAPSHOT"

// don't use a snapshot version in a main branch
val commonVersions = CommonVersions(kotlinCommon = "0.6.1")
val commonDependencies = CommonDependencies(commonVersions)
val commonGradleClasspathDependencies = CommonGradleClasspathDependencies(commonVersions)

object DependencyVersions {
    val exposedAdtMapping = "0.3.1-SNAPSHOT" // TODO don't use a snapshot version in a main branch
}
