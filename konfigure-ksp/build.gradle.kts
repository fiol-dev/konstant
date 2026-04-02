plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = libs.versions.libGroup.get()
version = libs.versions.libVersion.get()

dependencies {
    implementation(project(":konfigure-annotations"))
    implementation(project(":konfigure-core"))
    implementation(libs.ksp.api)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
    testImplementation(libs.kotlin.test)
}

mavenPublishing {
    publishToMavenCentral()

    // If you have created signing for publishing, enable the next line
    //signAllPublications()

    coordinates(
        groupId = group.toString(),
        artifactId = "konfigure-ksp",
        version = version.toString()
    )

    pom {
        name = "Konfigure Library"
        description = "Pydantic Settings but for KMP"
        inceptionYear = "2026"
        url = "https://github.com/fiol-dev/kotlin-multiplatform-settings"
        licenses {
            license {
                name = "MIT"
                url = "https://opensource.org/licenses/MIT"
            }
        }
        developers {
            developer {
                id = "fioldev"
                name = "Nikita Balobanov"
                url = "https://github.com/fiol-dev"
                email = "fiolmailosu@gmail.com"
            }
        }
        scm {
            url = "https://github.com/fiol-dev/kotlin-multiplatform-settings"
            connection = "scm:git:https://github.com/fiol-dev/kotlin-multiplatform-settings.git"
            developerConnection = "scm:git:ssh://git@github.com/fiol-dev/kotlin-multiplatform-settings.git"
        }
    }
}