// build.gradle.kts (app)

import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.mikepenz.aboutlibraries.plugin")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

android {
    namespace = "com.cleanify"
    compileSdk = 36

    kotlin {
        jvmToolchain(17)
    }
    signingConfigs {
        create("release") {
            val keystorePropsFile = rootProject.file("keystore.properties")
            val keystoreProps = if (keystorePropsFile.exists()) {
                Properties().apply { load(keystorePropsFile.inputStream()) }
            } else null

            val storeFileProp = keystoreProps?.getProperty("storeFile")
                ?: project.properties["CLEANIFY_RELEASE_STORE_FILE"] as? String
            val keyAliasProp = keystoreProps?.getProperty("keyAlias")
                ?: project.properties["CLEANIFY_RELEASE_KEY_ALIAS"] as? String
            val storePasswordProp = keystoreProps?.getProperty("storePassword")
                ?: project.properties["CLEANIFY_RELEASE_STORE_PASSWORD"] as? String
            val keyPasswordProp = keystoreProps?.getProperty("keyPassword")
                ?: project.properties["CLEANIFY_RELEASE_KEY_PASSWORD"] as? String

            if (storeFileProp != null && keyAliasProp != null) {
                storeFile = file(storeFileProp)
                keyAlias = keyAliasProp
                storePassword = storePasswordProp
                keyPassword = keyPasswordProp
            }
        }
    }

    sourceSets {
        getByName("main").java.srcDirs("src/main/java")
    }

    defaultConfig {
        applicationId = "com.cleanify"
        minSdk = 29
        targetSdk = 36
        versionCode = 16
        versionName = "2.9.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        val abiFilter = project.findProperty("abiFilter") as? String
        if (abiFilter != null) {
            val filter = abiFilter
            ndk {
                abiFilters.add(filter)
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
            isUniversalApk = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    ndkVersion = "30.0.14904198"

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    packaging {
        resources {
            resources.excludes.add("META-INF/AL2.0")
            resources.excludes.add("META-INF/LGPL2.1")
            resources.excludes.add("META-INF/DEPENDENCIES")
        }
    }

    lint {
        baseline = file("lint-baseline.xml")
        abortOnError = false
        warningsAsErrors = false
        xmlReport = true
        htmlReport = true
    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            val appName = "Cleanify"
            val versionName = output.versionName.get()
            val buildType = variant.buildType
            val customAbi: String? by project
            val abiSuffix = if (customAbi != null) "-$customAbi" else ""

            val baseName = "$appName-v$versionName$abiSuffix-$buildType"
            val filters = output.filters
            val abiFilter = filters.find { it.filterType.name.equals("ABI", ignoreCase = true) }?.identifier
            val finalName = if (abiFilter != null) "$baseName-$abiFilter.apk" else "$baseName.apk"

            (output as? com.android.build.api.variant.impl.VariantOutputImpl)?.outputFileName?.set(finalName)
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xreport-unused-for-optimization")
        freeCompilerArgs.add("-Xlint:all")
    }
}



aboutLibraries {

}

dependencies {
    val composeBomVersion = "2026.01.01"
    implementation(platform("androidx.compose:compose-bom:$composeBomVersion"))
    androidTestImplementation(platform("androidx.compose:compose-bom:$composeBomVersion"))

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.activity:activity-compose:1.12.3")
    implementation("androidx.lifecycle:lifecycle-service:2.10.0")
    implementation("androidx.core:core-splashscreen:1.2.0")

    val work_version = "2.11.1"
    implementation("androidx.work:work-runtime-ktx:$work_version")

    implementation("androidx.hilt:hilt-work:1.3.0")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.navigation:navigation-compose:2.9.7")
    implementation("androidx.compose.material3:material3-window-size-class:1.4.0")

    implementation("com.google.dagger:hilt-android:2.59")
    ksp("com.google.dagger:hilt-compiler:2.59")
    implementation("androidx.hilt:hilt-navigation-compose:1.3.0")

    implementation("androidx.room:room-runtime:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")

    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-video:2.7.0")
    implementation("io.coil-kt:coil-gif:2.7.0")

    implementation("androidx.media3:media3-exoplayer:1.9.2")
    implementation("androidx.media3:media3-ui:1.9.2")
    implementation("androidx.media3:media3-common:1.9.2")

    implementation("com.google.accompanist:accompanist-permissions:0.37.3")

    implementation("androidx.datastore:datastore-preferences:1.2.0")

    implementation("androidx.documentfile:documentfile:1.1.0")

    implementation("com.mikepenz:aboutlibraries-compose:12.2.4")

    implementation("com.tom-roush:pdfbox-android:2.0.27.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit-ktx:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.10.2")
}
