# Vibe Lang for Android

A native Kotlin / Jetpack Compose client for the Vibe Lang API. It does the same things as the React web app
(`../frontend`), with **Study mode** as the main experience and **Admin mode** for editing content.

## Open and run

1. In Android Studio choose **File › Open** and select this `android/` folder (not the repository root).
2. Let Gradle sync, pick an emulator or a connected phone, and press **Run**.

From the command line: `./gradlew installDebug` (needs `JAVA_HOME` set to JDK 17+; `source ../env.sh` works).

The app talks to `https://vibe-lang-api.onrender.com/` by default. To use a local backend instead:

```bash
./gradlew installDebug -Pvibelang.apiBaseUrl=http://10.0.2.2:8080/   # emulator → your computer's localhost
```

or change `vibelang.apiBaseUrl` in `gradle.properties`. Debug builds allow plain `http` to `10.0.2.2` and
`localhost` (`app/src/debug/res/xml/network_security_config.xml`). Add your LAN IP there to use a physical device.

Render's free tier sleeps when idle, so the first request can take up to a minute. The app shows a
"Waking up the server…" message while it waits.

## Features

- **Learning path**: subjects in course order, with category, lesson and word count. Pull to refresh.
- **Categories**: subjects grouped by category.
- **Subject**: Markdown lesson (GitHub tables supported, via Markwon) and vocabulary list.
- **Study** (flashcards): tap to flip, then swipe right for *Got it* and left for *Again*, or use the buttons.
  Missed cards go to the back of the queue, and the summary lets you study only the missed ones.
- **Practice** (writing): each term is shown and you type its translation. Wrong answers show the correct
  translation and go to the end of the set. They keep coming back until you get them right. Checking ignores
  case, punctuation and extra spaces, accepts any `/`- or `;`-separated alternative, treats `(bracketed)` words
  as optional, and accepts a missing accent but points it out. Runs entirely on the device.
- **Settings**: mode, course, which side is the Study card front, and shuffle (Study and Practice). Stored with
  DataStore.
- **Admin mode** (turn it on in Settings): an *Admin* tab for courses, categories and subjects (create, edit,
  delete, reorder), plus a Markdown lesson editor with preview, vocabulary editing and bulk import
  (`term | translation | notes`, one per line) on each subject page.

As with the web app, admin mode is only a UI switch. The API has no authentication.

## Code layout

```
app/src/main/java/com/vibelang/android/
├── data/            Models, Retrofit API, network setup and error messages, DataStore settings
├── ui/
│   ├── VibeLangApp.kt   Navigation (type-safe routes) and bottom bar
│   ├── AppViewModel.kt  Settings + courses shared by every screen
│   ├── ViewModels.kt    UiState, LoadViewModel / ActionViewModel base classes
│   ├── components/      Shared composables (loading, errors, dialogs, Markdown, course picker)
│   ├── path/ categories/ subject/ study/ practice/ settings/ admin/   One package per screen
│   └── theme/
```

Unit tests (`./gradlew test`) cover the Study and Practice session logic, answer checking and the bulk-import parser.

Stack: Kotlin 2.4, AGP 9.5, Gradle 9.6, Compose BOM 2026.09, Material 3, Navigation Compose, Retrofit 3 +
kotlinx.serialization, OkHttp, DataStore, Markwon. minSdk 26, targetSdk 37.
