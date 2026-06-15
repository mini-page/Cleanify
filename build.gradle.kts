plugins {
    id("com.android.application") version "8.11.2" apply false
    id("org.jetbrains.kotlin.android") version "2.2.20" apply false
    id("com.google.dagger.hilt.android") version "2.57.1" apply false
    id("com.google.devtools.ksp") version "2.2.20-2.0.2" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20" apply false
    id("com.mikepenz.aboutlibraries.plugin") version "12.2.4" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
