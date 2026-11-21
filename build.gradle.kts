// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.aboutLibraries) apply false
}

subprojects {
    configurations.configureEach {
        // Backdrop's transitive shapes dependency is supplied by the local fork.
        exclude(group = "io.github.kyant0", module = "shapes")
        exclude(group = "io.github.kyant0", module = "shapes-android")
    }
}
