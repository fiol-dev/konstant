plugins {
    id("com.vanniktech.maven.publish")
    signing
}

// Release builds sign with the in-memory key from CI. Without one (publishToMavenLocal on a
// developer machine) publications are left unsigned instead of failing the build.
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
        artifactId = project.name,
        version = version.toString(),
    )

    pom {
        name.set("Konstant")
        description.set("Typed configuration for Kotlin Multiplatform apps, loaded from env vars, files and remote sources")
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
