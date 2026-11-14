# Sidedoor

Instagram through the side door: your chats, the reels friends send you, and
your Saved. No Home feed, no Reels tab, no Explore, no stories.

Sidedoor is a small Android app for people who want Instagram's messaging
without its feed. It works only when you go in through Sidedoor. Open Instagram
from its own icon or from a notification and it is the normal app, untouched:
the guard does not read the screen at all.

Sidedoor does not modify Instagram and does not touch its network traffic. It
is an Android accessibility service that watches the real Instagram app,
recognises each screen, and closes every one that is not on an allowlist.
Messaging keeps full native quality: notifications, voice notes, calls,
reactions.

> **Not affiliated with Instagram or Meta.** "Instagram" is a trademark of its
> owner. This is an independent personal project that works alongside the
> official app and is not endorsed by, or connected to, Meta.

> **Fragile by design.** The guard recognises screens by Instagram's internal
> view IDs, which Instagram can rename in any update. The rules here were
> recorded against Instagram 448.0.0.52.84 on a single phone. After an
> Instagram update the guard may miss things until the rules are updated.
> Sidedoor has a canary that tells you when the inbox stops being recognised,
> but treat it as a helper for your own willpower, not a security boundary.

## What it does

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

Instagram's own notifications are left exactly as they are. The Saved button,
story covering and the reels list read English labels; the rest of the guard
works in any language.

## Permissions and why

Sidedoor asks for the following, and nothing else:

| Permission or access | Why |
|---|---|
| **Accessibility service** (`BIND_ACCESSIBILITY_SERVICE`) | Reads Instagram's on-screen structure to recognise the current screen, draws the cover and dead-zone overlays (accessibility overlays, so no "display over other apps" grant is needed), and performs the Back, tap and Home actions that correct a blocked screen. At install the service is limited to the Instagram package. During a guarded visit it also watches the home-screen apps and Sidedoor itself, only to tell "went home" from "popped into another app". |
| **Notification access** (`BIND_NOTIFICATION_LISTENER_SERVICE`) | Reads Instagram's DM notifications to fill the "Reels from friends" list. Android hands a notification listener every notification, so the code drops everything that is not from Instagram's direct-message channels straight away. Nothing is dismissed or changed. |
| **Post notifications** (`POST_NOTIFICATIONS`) | Sidedoor's single alert: Instagram changed and the guard may be misreading screens. |
| **Package visibility** (`<queries>`) | To launch Instagram, read its version for the canary, and find the home-screen apps that end a session. There is no broad "query all packages" permission. |

**No `INTERNET` permission.** Whatever Sidedoor reads cannot leave the phone.
This is not just a promise: every build runs
`:app:verify<Variant>NoNetworkPermissions`, which fails if a network,
Wi-Fi-state or query-all-packages permission reaches the merged manifest (a
transitive dependency can add one without any source file changing).

What it stores, on the phone only: the list of waiting reels (sender name and
message preview, from your inbox and notifications) and per-day counters for
the "This week" card. Backups are switched off (`allowBackup="false"`).

Debug builds can additionally write the current Instagram screen, including any
text on it, to the app's private files folder (a broadcast, or automatically
when a block happens), so that a false positive can become a test fixture.
Release builds do not. If you contribute a fixture, scrub personal text first:
the fixtures in this repo have every name, message and caption replaced with
`redacted`.

## Build

You need a JDK 17 or newer and the Android SDK (platform 37 and build tools),
with `ANDROID_HOME` set or an SDK path in `local.properties`
(`sdk.dir=/path/to/sdk`; this file is git-ignored). The Gradle wrapper
downloads Gradle itself.

```bash
./gradlew test                 # JVM unit tests (no device needed)
./gradlew :app:assembleDebug   # debug APK in app/build/outputs/apk/debug/
```

The versions of the Android Gradle Plugin, Gradle, Kotlin and the Compose BOM
are pinned together in `gradle/libs.versions.toml`; bump them as a set.

### Signing

Both build types use a signing key if you provide one, so that a debug build
can replace a release build on the same phone without uninstalling. Put these
in your user-level `~/.gradle/gradle.properties` (never in the repo):

```properties
sidedoor.keystore=/path/to/your-keystore.jks
sidedoor.keystore.password=your-password
```

The key alias must be `sidedoor`. Without them the debug build uses Android's
default debug key and the release build comes out **unsigned** (it will not
install until you sign it). Keep a backup of any key you use: every copy
already installed can only be updated by an APK signed with the same key.

### Install over adb

```bash
tools/install.sh [adb-serial] [debug|release]   # build, install, switch on guard + reels reader + alerts
tools/device-check.sh [adb-serial]              # on-device regression check
```

`install.sh` grants the notification and listener permissions and enables the
accessibility service through `adb`, so there is no tapping through Settings.
You can also enable everything by hand from the Sidedoor screen, which links to
each setting. `device-check.sh` is meant to be run after every Instagram
update; it only launches Instagram, taps the covered tabs, swipes between tabs
and reads Sidedoor's own log lines. It never opens a chat, taps a story, or
likes or follows anything.

Use Sidedoor's icon when you want the side door and Instagram's own icon when
you want the normal app. Long-press the Sidedoor icon for Chats / Saved
shortcuts.

## Layout

- `guard-core/` - all decisions, pure Kotlin with no Android APIs: screen
  classifier, allowlist policy, correction ladder, dead-zone planner, Saved
  trip, notification rules, Reel Inbox queue, window-stack safety, canary.
  Tested against screen dumps in `src/test/resources/fixtures/` (personal text
  replaced with `redacted`) plus hand-built snapshots.
- `app/` - the accessibility service that reads the screen and carries out the
  decisions, the overlays, the notification listener, and the Front Door UI.
- `build-logic/` - Gradle convention plugins, including the no-network
  permission audit.
- `tools/` - adb install and device-check scripts.

`guard-core/.../classify/IgIds.kt` holds every Instagram view ID and label the
guard depends on. When Instagram changes, that file and the fixtures change.

## Tech

Kotlin, Jetpack Compose (Material 3), Android `AccessibilityService` and
`NotificationListenerService`, Gradle Kotlin DSL with convention plugins,
JUnit 4 and Truth for the tests. Minimum Android 8.0 (API 26).

## Licence and credits

MIT - see [LICENSE](LICENSE). The Gradle wrapper (`gradlew`, `gradlew.bat`,
`gradle/wrapper/gradle-wrapper.jar`) is part of Gradle and is licensed under
the Apache License 2.0.
