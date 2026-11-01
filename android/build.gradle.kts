// Top-level build file. Module configuration lives in app/build.gradle.kts.
plugins {
    alias(libs.plugins.android.application) apply false
    // AGP 9 compiles Kotlin itself; declaring the Kotlin plugin here only pins the Kotlin version.
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
