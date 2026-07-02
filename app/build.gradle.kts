import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

val versionPropsProvider =
    providers.fileContents(layout.projectDirectory.file("version.properties"))

val vCodeProvider = versionPropsProvider.asText.map { text ->
    val props = Properties().apply { load(text.reader()) }
    props.getProperty("VERSION_CODE", "1").toInt()
}

val vNameProvider = versionPropsProvider.asText.map { text ->
    val props = Properties().apply { load(text.reader()) }
    props.getProperty("VERSION_NAME", "1.0.0")
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.aboutLibraries)
}

android {
    namespace = "com.nevoit.cresto"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.nevoit.cresto"
        minSdk = 33
        targetSdk = 37
        versionCode = vCodeProvider.get()
        versionName = vNameProvider.get()
        buildConfigField(
            "String",
            "UPDATE_MANIFEST_URL",
            "\"https://nevodev.github.io/cresto/update/latest.json\""
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters.add("arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

configurations.configureEach {
    exclude(group = "androidx.compose.material3")
}

base {
    archivesName.set(vCodeProvider.map { "cresto-alpha$it" })
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.addAll(
            "-XXLanguage:+PropertyParamAnnotationDefaultTargetMode",
            "-Xannotation-default-target=param-property"
        )
    }
}

dependencies {
    implementation(project(":glasense-ui"))
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.animation.graphics)
    implementation(libs.androidx.compose.foundation.layout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.mmkv)
    implementation(libs.shapes)
    implementation(libs.backdrop)
    implementation(libs.confetti.kit)
    implementation(libs.material.color.utilities)
    implementation(libs.aboutlibraries.core)
    implementation(libs.aboutlibraries.compose.core)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.androidx.compose)
}

tasks.register("incrementVersionCode") {
    description = "Increments the version code in version.properties after a release build."
    val versionPropsFile = layout.projectDirectory.file("version.properties").asFile

    outputs.upToDateWhen { false }

    doLast {
        if (versionPropsFile.exists()) {
            val props = Properties()

            FileInputStream(versionPropsFile).use { stream ->
                props.load(stream)
            }

            val currentCode = props.getProperty("VERSION_CODE", "1").toInt()
            val newCode = currentCode + 1
            props.setProperty("VERSION_CODE", newCode.toString())

            FileOutputStream(versionPropsFile).use { stream ->
                props.store(stream, "Version properties updated")
            }

            println("✅ Version code incremented to $newCode")
        } else {
            println("⚠️ version.properties not found, skipping increment.")
        }
    }
}

tasks.matching { task ->
    task.name == "assembleRelease" || task.name == "bundleRelease"
}.configureEach {
    finalizedBy("incrementVersionCode")
}

composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
    metricsDestination = layout.buildDirectory.dir("compose_compiler")
}
