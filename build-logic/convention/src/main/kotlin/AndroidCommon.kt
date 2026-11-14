import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.sidedoor.buildlogic.SidedoorSdk
import com.sidedoor.buildlogic.lib
import com.sidedoor.buildlogic.libs
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/** Shared Kotlin/Java settings so every module compiles identically. */
internal fun Project.configureKotlinJvm() {
    extensions.configure<KotlinAndroidProjectExtension> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            allWarningsAsErrors.set(false)
            freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
        }
    }
    dependencies {
        add("coreLibraryDesugaring", libs.lib("desugar-jdk-libs"))
    }
}

internal fun ApplicationExtension.applyCommonAndroid() {
    compileSdk = SidedoorSdk.COMPILE
    defaultConfig {
        minSdk = SidedoorSdk.MIN
        targetSdk = SidedoorSdk.TARGET
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
}

internal fun LibraryExtension.applyCommonAndroid() {
    compileSdk = SidedoorSdk.COMPILE
    defaultConfig {
        minSdk = SidedoorSdk.MIN
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
}
