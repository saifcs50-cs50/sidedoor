// Pure decision logic: what screen Instagram is showing and what the guard
// should do about it. No Android APIs in main code, so every rule is covered
// by fast JVM tests against real screen dumps from a device.
plugins {
    id("sidedoor.android.library")
}

android {
    namespace = "com.sidedoor.guard"
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
