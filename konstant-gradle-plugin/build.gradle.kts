// Gradle plugin "io.github.fiol-dev.konstant". A separate included build so the main build's
// own modules (konstant-ksp-tests) can apply it. Published to Maven Central next to the
// libraries, so apps resolve it with mavenCentral() in pluginManagement.
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
    alias(libs.plugins.vanniktech.mavenPublish)
    signing
}

group = libs.versions.libGroup.get()
version = libs.versions.libVersion.get()

// Loadable by any Gradle 9 build, whatever JDK builds the plugin
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xjdk-release=17")
    }
}

dependencies {
    testImplementation(libs.kotlin.test)
    testImplementation(gradleTestKit())
}

gradlePlugin {
    website.set("https://github.com/fiol-dev/konstant")
    vcsUrl.set("https://github.com/fiol-dev/konstant")
    plugins {
        create("konstant") {
            id = "io.github.fiol-dev.konstant"
            displayName = "Konstant"
            description = "Bakes config files into a Kotlin Multiplatform app as Konstant sources"
            implementationClass = "io.github.fiol_dev.konstant.gradle.KonstantPlugin"
            tags.set(listOf("kotlin-multiplatform", "configuration"))
        }
    }
}

// Same signing and POM as the library modules (build-logic konstant.publishing): unsigned
// without a key, so publishToMavenLocal works on a developer machine
val signingKey = (findProperty("signingKey") as String?)?.takeIf { it.isNotBlank() }
if (signingKey != null) {
    signing {
        useInMemoryPgpKeys(
            findProperty("signingKeyId") as String?,
            signingKey,
            findProperty("signingKeyPassword") as String?,
        )
    }
}

mavenPublishing {
    publishToMavenCentral(false)
    if (signingKey != null) signAllPublications()

    coordinates(
        groupId = group.toString(),
        artifactId = "konstant-gradle-plugin",
        version = version.toString(),
    )

    pom {
        name.set("Konstant Gradle plugin")
        description.set("Bakes per-environment config files into Kotlin Multiplatform apps that use Konstant")
        inceptionYear.set("2026")
        url.set("https://github.com/fiol-dev/konstant")
        licenses {
            license {
                name.set("MIT")
                url.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                id.set("fiol-dev")
                name.set("fiol-dev")
                url.set("https://github.com/fiol-dev")
                email.set("fiolmailosu@gmail.com")
                organization.set("fiol-dev")
                organizationUrl.set("https://github.com/fiol-dev")
            }
        }
        scm {
            url.set("https://github.com/fiol-dev/konstant")
            connection.set("scm:git:https://github.com/fiol-dev/konstant.git")
            developerConnection.set("scm:git:ssh://git@github.com/fiol-dev/konstant.git")
        }
    }
}
