plugins {
    id("com.vanniktech.maven.publish")
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates(
        groupId = group.toString(),
        artifactId = project.name,
        version = version.toString(),
    )

    pom {
        name.set("Konfigure")
        description.set("Pydantic Settings but for KMP")
        inceptionYear.set("2026")
        url.set("https://github.com/fiol-dev/kotlin-multiplatform-settings")
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
            url.set("https://github.com/fiol-dev/kotlin-multiplatform-settings")
            connection.set("scm:git:https://github.com/fiol-dev/kotlin-multiplatform-settings.git")
            developerConnection.set("scm:git:ssh://git@github.com/fiol-dev/kotlin-multiplatform-settings.git")
        }
    }
}
