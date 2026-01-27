plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.kotlin.serialization)
}

fun runCmd(cmd: String, workDir: File = file("./")): String {
    val out = providers.exec {
        workingDir = workDir
        commandLine = cmd.split("\\s".toRegex())
    }
    return out.standardOutput.asText.get().trim()
}

fun getTagVersion(): String {
    val tag = try {
        runCmd("git describe --tags")
    } catch (e: Exception) {
        println(e)
        "v0.0"
    }
    if (tag.startsWith("v"))
        return tag.substring(1)
    return tag
}

android {
    namespace = "com.magic.maw"
    compileSdk = 36

    val currentSigning = if (project.hasProperty("STORE_FILE")) {
        signingConfigs.create("release") {
            storeFile = file(project.properties["STORE_FILE"] as String)
            storePassword = project.properties["STORE_PASSWORD"] as String
            keyPassword = project.properties["KEY_PASSWORD"] as String
            keyAlias = project.properties["KEY_ALIAS"] as String
        }
    } else {
        signingConfigs.getByName("debug")
    }

    defaultConfig {
        applicationId = "com.magic.maw"
        minSdk = 21
        targetSdk = 36
        versionCode = 1
        versionName = getTagVersion()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        all {
            signingConfig = currentSigning
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            versionNameSuffix = "-debug"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

ksp {
    arg("room.generateKotlin", "true")
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.exifinterface)
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.zoomable.image.coil)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.toaster)
    implementation(libs.atomicfu)
    implementation(libs.kermit)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.jsoup)
    implementation(libs.androidx.datastore)
    implementation(libs.kotlinx.collections.immutable.jvm)
    implementation(libs.kotlinx.datetime)
    implementation(libs.androidx.paging.common)
    implementation(libs.androidx.paging.compose)
    implementation(libs.coil.video)
    implementation(libs.coil.network.ktor3)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.activity)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material3.window)
    implementation(libs.compose.icons)
    implementation(libs.compose.preview)
    debugImplementation(libs.compose.tooling)
    androidTestImplementation(libs.compose.junit4)
    androidTestImplementation(composeBom)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
}