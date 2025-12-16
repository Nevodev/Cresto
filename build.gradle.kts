// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.google.devtools.ksp") version "2.3.3" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.21" apply false
}