plugins {
    id("com.android.application") version "9.4.0" apply false
    // AGP 9 ships built-in Kotlin: do NOT apply org.jetbrains.kotlin.android.
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10" apply false
    id("com.google.devtools.ksp") version "2.3.4" apply false
}
