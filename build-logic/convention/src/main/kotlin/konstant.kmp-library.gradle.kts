import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension

plugins {
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.kotlin.multiplatform")
}

group = libs.version("libGroup")
version = libs.version("libVersion")

kotlin {
    explicitApi()

    // Public API dumps live in each module's api/ folder; checkLegacyAbi fails on unreviewed changes
    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation {
        enabled.set(true)
        klib { enabled.set(true) }
    }

    android {
        namespace = "$group.${project.name.removePrefix("konstant-")}"
        compileSdk = libs.version("android-compileSdk").toInt()
        minSdk = libs.version("android-minSdk").toInt()
        withHostTest {}
    }
    jvm()
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
        yarnLockMismatchReport = YarnLockMismatchReport.WARNING
        reportNewYarnLock = false
        yarnLockAutoReplace = true
    }
}
