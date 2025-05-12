import org.gradle.internal.extensions.stdlib.capitalized

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
}

// val packageApp= tasks.register<GenerateCountriesMinDistTask>("generateCountriesMinDist") {
//     rootProject.set(File("."))
//     outputDirectory.set(File("build/outputs"))
//     //from(layout.projectDirectory.file("run.sh"))
// }