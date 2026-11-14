package com.sidedoor.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/** Sidedoor-wide SDK targets. Pinned to a known-good set of values. */
object SidedoorSdk {
    /** Compose BOM 2026.08.00 requires compileSdk 37. */
    const val COMPILE = 37

    /** Runtime behaviour contract. Bumped deliberately, never incidentally. */
    const val TARGET = 36

    /** Android 8.0: adaptive icons and java.time. */
    const val MIN = 26
}

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.lib(alias: String) = findLibrary(alias).get()
