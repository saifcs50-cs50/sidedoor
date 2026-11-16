# Sidedoor

I wanted Instagram only for my chats, the reels friends send me and my Saved, so I built Sidedoor. It's a small
Android app that keeps Instagram's Home feed, Reels tab, Explore and stories closed.

Sidedoor is an accessibility service that watches the real Instagram app, recognises each screen and closes any
screen that isn't on an allowlist. It doesn't modify Instagram or touch its network traffic, so messaging keeps full
native quality: notifications, voice notes, calls and reactions. When you open Instagram from its own icon or from a
notification, it's the normal app and the guard doesn't read the screen at all.

It needs Android 8.0 (API 26) or newer. Sidedoor is not affiliated with Instagram or Meta. "Instagram" is a trademark
of its owner. This is an independent personal project that works alongside the official app and is not endorsed by,
or connected to, Meta.

## What the guard does

| | |
|---|---|
| **Guarded visits** | A visit opened through Sidedoor (Chats, Saved, a reel from the list, a home-screen shortcut) is guarded until you go to the home screen or back to Sidedoor. A detour into another app under a minute (photo picker, sharing) keeps it, and so does locking the phone. |
| **Opens straight into DMs** | Chats lands in the inbox; if the feed appears anyway it is covered and you are sent back. |
| **Tabs vanish** | Home, Reels and Search are painted over in Instagram's own tab-bar colour and eat taps. |
| **One reel per tap** | A friend's reel plays, but the video cannot be swiped (volume keys still work). A swipe that starts on the button column is covered the instant the pager moves and backed out of. |
| **Saved, one tap** | The Saved button walks Profile → Options → Saved behind a cover. Saved reels open full screen and swipe through your Saved; anything you did not save is bounced. |
| **Default deny for media** | An unrecognised screen that shows media is blocked, so new Instagram surfaces are closed on day one. |
| **No stories** | Story avatars are covered, so a story never opens (and never tells a friend you watched it). |
| **Reels from friends** | Unread reels friends sent line up in a finite list on the Sidedoor screen, read from your inbox each time you open Chats (and from DM notifications, where Instagram's are on). Tapping one opens that chat. |
| **Canary** | Every Chats tap checks that the guard still recognises the inbox; a failure means Instagram changed and you get told. |

## Telling a saved reel apart

Saved reels and Instagram's own picks play in the same full-screen viewer, and the guard tells them apart by the
bookmark button. My first version read its label, which blocked my own saved reels: that viewer keeps the label
"Save" on a saved reel, and only post lists change it to "Remove from saved". The button is marked as selected,
though, so the guard now accepts either signal.

## Limits

- The guard recognises screens by Instagram's internal view IDs, which Instagram can rename in any update. I
  recorded the rules against Instagram 448.0.0.52.84 on a single phone, so after an update the guard may miss things
  until the rules are updated.
- Treat it as a helper for your own willpower. It is not a security boundary.
- The Saved button, story covering and the reels list read English labels. The rest of the guard works in any
  language.

## Permissions and why

Sidedoor asks for these and nothing else:

| Permission or access | Why |
|---|---|
| **Accessibility service** (`BIND_ACCESSIBILITY_SERVICE`) | Reads Instagram's on-screen structure to recognise the current screen, draws the cover and dead-zone overlays (accessibility overlays, so no "display over other apps" grant is needed), and performs the Back, tap and Home actions that correct a blocked screen. At install the service is limited to the Instagram package. During a guarded visit it also watches the home-screen apps and Sidedoor itself, only to tell "went home" from "popped into another app". |
| **Notification access** (`BIND_NOTIFICATION_LISTENER_SERVICE`) | Reads Instagram's DM notifications to fill the "Reels from friends" list. Android hands a notification listener every notification, so the code drops everything that is not from Instagram's direct-message channels straight away. Nothing is dismissed or changed. |
| **Post notifications** (`POST_NOTIFICATIONS`) | Sidedoor's single alert: Instagram changed and the guard may be misreading screens. |
| **Package visibility** (`<queries>`) | To launch Instagram, read its version for the canary, and find the home-screen apps that end a session. There is no broad "query all packages" permission. |

There is no `INTERNET` permission, so nothing Sidedoor reads can leave the phone. Every build runs
`:app:verify<Variant>NoNetworkPermissions`, which fails if a network, Wi-Fi-state or query-all-packages permission
reaches the merged manifest, because a transitive dependency can add one without any source file changing.

It stores, on the phone only, the list of waiting reels (sender name and message preview) and per-day counters for
the "This week" card. Backups are switched off (`allowBackup="false"`).

Debug builds can also dump the current Instagram screen, including any text on it, to the app's private files folder (on a
broadcast, or automatically when a block happens) so a false positive can become a test fixture. Release builds
don't. Scrub personal text from any fixture you contribute: the ones in this repo have every name, message and
caption replaced with `redacted`.

## Build

You need JDK 17 or newer and the Android SDK (platform 37 and build tools), with `ANDROID_HOME` set or
`sdk.dir=/path/to/sdk` in a git-ignored `local.properties`. The Gradle wrapper downloads Gradle itself.

```bash
./gradlew test                 # JVM unit tests (no device needed)
./gradlew :app:assembleDebug   # debug APK in app/build/outputs/apk/debug/
```

The versions of the Android Gradle Plugin, Gradle, Kotlin and the Compose BOM are pinned together in
`gradle/libs.versions.toml`, so bump them as a set.

### Signing

Both build types use your signing key if you set one, so a debug build can replace a release build on the same
phone without uninstalling. Put these in your user-level `~/.gradle/gradle.properties`, never in the repo:

```properties
sidedoor.keystore=/path/to/your-keystore.jks
sidedoor.keystore.password=your-password
```

The key alias must be `sidedoor`. Without a key, the debug build uses Android's default debug key and the release
build comes out unsigned, so it won't install until you sign it. Back up any key you use: an installed copy can only
be updated by an APK signed with the same key.

### Install over adb

```bash
tools/install.sh [adb-serial] [debug|release]   # build, install, switch on guard + reels reader + alerts
tools/device-check.sh [adb-serial]              # on-device regression check
```

`install.sh` grants the notification and listener permissions and turns on the accessibility service over `adb`.
You can also do it by hand from the Sidedoor screen, which links to each setting. Run `device-check.sh` after every
Instagram update. It only launches Instagram, taps the covered tabs, swipes between tabs and reads Sidedoor's own log
lines, and it never opens a chat, taps a story, or likes or follows anything.

Long-press the Sidedoor icon for Chats and Saved shortcuts.

## Layout

- `guard-core/` makes every decision, in pure Kotlin with no Android APIs: the screen classifier, allowlist policy,
  correction ladder, dead-zone planner, Saved trip, notification rules, Reel Inbox queue, window-stack safety and
  canary. Its tests run against screen dumps in `src/test/resources/fixtures/` and hand-built snapshots.
- `app/` is the accessibility service that reads the screen and carries out those decisions, plus the overlays, the
  notification listener and the Front Door UI.
- `build-logic/` has the Gradle convention plugins, including the no-network permission audit.
- `tools/` has the adb install and device-check scripts.

`guard-core/.../classify/IgIds.kt` holds every Instagram view ID and label the guard depends on. When Instagram
changes, that file and the fixtures change.

## Tech

Kotlin, Jetpack Compose (Material 3), Android `AccessibilityService` and `NotificationListenerService`, Gradle Kotlin
DSL with convention plugins, and JUnit 4 and Truth for the tests.

## Licence and credits

MIT, see [LICENSE](LICENSE). The Gradle wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`) is
part of Gradle and is licensed under the Apache License 2.0.
