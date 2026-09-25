plugins {
    id("com.vanniktech.maven.publish")
    signing
}

signing {
    val signingKey = project.findProperty("signingKey") as String?
    val signingKeyId = project.findProperty("signingKeyId") as String?
    val signingKeyPassword = project.findProperty("signingKeyPassword") as String?
    useInMemoryPgpKeys(
        signingKeyId,
        signingKey,
        signingKeyPassword,
    )
}

mavenPublishing {
    publishToMavenCentral(false)
    signAllPublications()

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
