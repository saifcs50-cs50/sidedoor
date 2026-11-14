// Plugins are declared once here with `apply false`; modules opt in through
// the `sidedoor.*` convention plugins in build-logic.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
