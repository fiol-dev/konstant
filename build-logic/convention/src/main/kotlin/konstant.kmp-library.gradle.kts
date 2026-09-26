import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension

plugins {
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.kotlin.multiplatform")
    id("konstant.dokka")
}

group = libs.version("libGroup")
version = libs.version("libVersion")

// Pinned so a build on a newer JDK still produces jars that load on older runtimes
val jvmTargetVersion = libs.version("jvmTarget")

kotlin {
    explicitApi()

    // Public API dumps live in each module's api/ folder: checkKotlinAbi fails on unreviewed
    // changes, and updateKotlinAbi rewrites the dumps after an intended change
    // (Kotlin 2.4: calling abiValidation enables it, and klib dumps are always on)
    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation {}

    android {
        // Android namespaces must be valid Java packages, so the hyphen in the group becomes an underscore
        namespace = "${group.toString().replace('-', '_')}.${project.name.removePrefix("konstant-").replace('-', '_')}"
        compileSdk = libs.version("android-compileSdk").toInt()
        minSdk = libs.version("android-minSdk").toInt()
        withHostTest {}
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(jvmTargetVersion))
        }
    }
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(jvmTargetVersion))
            // Also checks that only JDK APIs of that version are used
            freeCompilerArgs.add("-Xjdk-release=$jvmTargetVersion")
        }
    }
    js {
        nodejs()
        browser { testTask { useKarma { useChromeHeadless() } } }
    }
    // A module can opt out with konstant.wasmJs=false in its gradle.properties when a dependency's wasm build is broken
    if (findProperty("konstant.wasmJs")?.toString() != "false") {
        @OptIn(ExperimentalWasmDsl::class)
        wasmJs {
            nodejs()
            browser { testTask { useKarma { useChromeHeadless() } } }
        }
    }
    linuxX64()
    macosArm64()
    iosArm64()
    iosSimulatorArm64()
    iosX64()
}

// Keep kotlin-js-store/yarn.lock current instead of failing when JS test tooling changes
rootProject.plugins.withType<YarnPlugin> {
    rootProject.the<YarnRootExtension>().apply {
        yarnLockMismatchReportProperty.set(YarnLockMismatchReport.WARNING)
        reportNewYarnLockProperty.set(false)
        yarnLockAutoReplaceProperty.set(true)
    }
}
