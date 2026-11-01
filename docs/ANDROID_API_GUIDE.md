# Vibe Lang: App & API Guide for an Android (Kotlin) Client

This document describes what Vibe Lang does, how its data is organised, and every endpoint of the REST API. Use it
as the reference for building an Android client in Kotlin that works like the existing React web app
(`frontend/`).

---

## 1. Purpose of the app

Vibe Lang helps a learner **study and practise grammar** in a foreign language. It currently has two courses,
**Spanish B2** and **Catalan A1**, and is designed so more languages and levels (Italian, English, …) can be
added later without code changes.

For each grammar topic the learner can:

1. **Follow a learning path.** Topics ("subjects") are studied in a fixed, curated order.
2. **Read a lesson.** This is an explanation of the topic written in Markdown (headings, tables, bold, quotes…).
3. **Practise vocabulary with flashcards.** These are the words and expressions related to the topic.

The app has two modes:

| Mode | Who it's for | What it shows |
|---|---|---|
| **Study** (default) | The learner | Learning path, categories, lessons, vocabulary lists, flashcards. Read-only. |
| **Admin** | The content author | Everything in Study mode, plus create/edit/delete/reorder for all content. |

The user switches modes on a **Settings** screen. The mode is a **client-side preference only**: the API has no
authentication and no concept of users or roles (see [§8](#8-current-limitations)).

---

## 2. Data model

```
Course              e.g. "Spanish B2"
 └── Category       e.g. "Verbs: Past"              (many per course; ordered)
      └── Subject   e.g. "Pretérito imperfecto"     (many per category; learning order is per COURSE)
           ├── Lesson       (0 or 1 per subject; Markdown text)
           └── Vocabulary   (0..n per subject; term + translation + optional notes)
```

Rules the client should know:

- A **subject belongs to exactly one category**, and through it to one course.
- **Learning order is course-wide.** `Subject.position` orders subjects across the whole course, not within
  their category, so the path can go e.g. *Past → Subjunctive → Past*. Always show subjects in the order the
  API returns them.
- **Categories have their own order** (`Category.position`), which is used when content is grouped by category.
- A subject has **at most one lesson**. A missing lesson is normal and not an error.
- **Deleting cascades.** Deleting a course removes its categories, subjects, lessons and vocabulary, and deleting
  a category removes its subjects and so on. Warn the user before deleting.
- `position` values are managed by the server. New items go to the end. Clients change order only through the
  reorder endpoints.

---

## 3. API basics

| | |
|---|---|
| **Base URL (production)** | `https://<your-api>.onrender.com` (see `DEPLOYMENT.md`) |
| **Base URL (local, Android emulator)** | `http://10.0.2.2:8080`, which is the emulator's alias for the host machine's `localhost` |
| **Base URL (local, physical device)** | `http://<your-computer-LAN-IP>:8080` |
| **Format** | JSON (`Content-Type: application/json`), UTF-8 |
| **Auth** | None |
| **IDs** | 64-bit integers, so use `Long` in Kotlin |
| **Timestamps** | ISO-8601 UTC strings, e.g. `"2026-11-01T11:54:31.022876894Z"` |
| **Nullable fields** | `description` and `notes` may be `null`. Other fields are always present. |

### Status codes

| Code | Meaning |
|---|---|
| `200 OK` | Success with a body |
| `201 Created` | Resource created. The body is the created resource. |
| `204 No Content` | Deleted. No body. |
| `400 Bad Request` | Validation failed, or the reorder list was invalid |
| `404 Not Found` | The resource doesn't exist, or the subject has no lesson |
| `409 Conflict` | Duplicate, e.g. a second "Spanish B2" course or two categories with the same name in one course |
| `500` | Unexpected server error |

### Error body (RFC 7807 "problem details")

Every error response has this shape. `detail` is human-readable and can be shown to the user directly.

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "title must not be blank; categoryId must not be null",
  "instance": "/api/subjects"
}
```

### Validation rules (enforced by the server)

| Resource | Field | Rule |
|---|---|---|
| Course | `language` | required, not blank, max 100 chars |
| Course | `level` | required, not blank, max 20 chars |
| Course | `language` + `level` | unique together |
| Category | `name` | required, not blank, max 200, unique within its course |
| Subject | `title` | required, not blank, max 200 |
| Subject | `categoryId` | required, must exist |
| Lesson | `content` | required (may be an empty string) |
| Vocabulary | `term`, `translation` | required, not blank, max 500 each |

The server trims whitespace from names and terms. Blank `notes` are stored as `null`.

### Cold starts

On Render's free plan the API **sleeps after about 15 minutes idle**, and the first request can take
**30–60 seconds**. Set generous timeouts (e.g. 60–90 s read timeout in OkHttp) and show a loading state, not an
error.

---

## 4. Resources (JSON shapes)

### Course
```json
{ "id": 1, "language": "Spanish", "level": "B2", "description": "Upper-intermediate Spanish grammar.", "position": 1 }
```

### Category
```json
{ "id": 1, "courseId": 1, "name": "Verbs: Past", "description": "Past tenses of the indicative.", "position": 1 }
```

### Subject
```json
{
  "id": 1,
  "categoryId": 1,
  "categoryName": "Verbs: Past",
  "courseId": 1,
  "title": "Pretérito imperfecto",
  "description": "Habits, descriptions and background in the past.",
  "position": 1,
  "hasLesson": true,
  "vocabularyCount": 8
}
```
`categoryName`, `courseId`, `hasLesson` and `vocabularyCount` are read-only summary fields, so lists can be shown
without extra requests.

### Lesson
```json
{ "id": 1, "subjectId": 1, "content": "# Pretérito imperfecto\n\nThe *imperfecto* …", "updatedAt": "2026-11-01T11:54:31.022Z" }
```
`content` is **Markdown with GitHub extensions**, most importantly **tables**. Make sure the Markdown renderer
supports them.

### Vocabulary
```json
{ "id": 1, "subjectId": 1, "term": "a menudo", "translation": "often", "notes": null, "position": 3 }
```
`notes` is an optional example sentence or usage hint. The web app shows it on the back of the flashcard.

---

## 5. Endpoints

All paths are relative to the base URL.

### 5.1 Courses

| Method | Path | Body | Response |
|---|---|---|---|
| `GET` | `/api/courses` | — | `200` `Course[]`, ordered by position |
| `GET` | `/api/courses/{id}` | — | `200` `Course` · `404` |
| `POST` | `/api/courses` | `CourseRequest` | `201` `Course` · `400` · `409` |
| `PUT` | `/api/courses/{id}` | `CourseRequest` | `200` `Course` · `400` · `404` · `409` |
| `DELETE` | `/api/courses/{id}` | — | `204` · `404` (**cascades**) |

```json
// CourseRequest
{ "language": "Italian", "level": "A2", "description": "Elementary Italian." }
```

### 5.2 Categories

| Method | Path | Body | Response |
|---|---|---|---|
| `GET` | `/api/courses/{courseId}/categories` | — | `200` `Category[]`, ordered · `404` if course missing |
| `POST` | `/api/courses/{courseId}/categories` | `CategoryRequest` | `201` `Category` · `400` · `404` · `409` |
| `PUT` | `/api/courses/{courseId}/categories/order` | `ReorderRequest` | `200` `Category[]` in the new order · `400` |
| `GET` | `/api/categories/{id}` | — | `200` `Category` · `404` |
| `PUT` | `/api/categories/{id}` | `CategoryRequest` | `200` `Category` · `400` · `404` · `409` |
| `DELETE` | `/api/categories/{id}` | — | `204` · `404` (**cascades**) |

```json
// CategoryRequest
{ "name": "Verbs: Past", "description": "Past tenses of the indicative." }
```

### 5.3 Subjects

| Method | Path | Body | Response |
|---|---|---|---|
| `GET` | `/api/courses/{courseId}/subjects` | — | `200` `Subject[]` **in learning order** · `404` |
| `PUT` | `/api/courses/{courseId}/subjects/order` | `ReorderRequest` | `200` `Subject[]` in the new order · `400` |
| `GET` | `/api/subjects/{id}` | — | `200` `Subject` · `404` |
| `POST` | `/api/subjects` | `SubjectRequest` | `201` `Subject` (added at the end of the course's learning order) · `400` · `404` |
| `PUT` | `/api/subjects/{id}` | `SubjectRequest` | `200` `Subject` · `400` · `404` |
| `DELETE` | `/api/subjects/{id}` | — | `204` · `404` (also deletes its lesson and vocabulary) |

```json
// SubjectRequest
{ "categoryId": 1, "title": "Pretérito imperfecto", "description": "Habits and descriptions in the past." }
```

If `PUT` moves a subject to a category in a **different course**, the subject goes to the end of that course's
learning order.

There's no "subjects of one category" endpoint. Fetch the course's subjects and filter by `categoryId`, which is
what the web app does.

### 5.4 Lesson (one per subject)

| Method | Path | Body | Response |
|---|---|---|---|
| `GET` | `/api/subjects/{subjectId}/lesson` | — | `200` `Lesson` · `404` if there's no lesson (or no subject) |
| `PUT` | `/api/subjects/{subjectId}/lesson` | `LessonRequest` | `200` `Lesson`. **Creates or replaces** the lesson. |
| `DELETE` | `/api/subjects/{subjectId}/lesson` | — | `204` · `404` |

```json
// LessonRequest
{ "content": "# Title\n\nMarkdown text…" }
```

Treat `404` on `GET` as "no lesson yet", not as an error. You can avoid the request completely by checking
`Subject.hasLesson`.

### 5.5 Vocabulary

| Method | Path | Body | Response |
|---|---|---|---|
| `GET` | `/api/subjects/{subjectId}/vocabulary` | — | `200` `Vocabulary[]`, ordered · `404` |
| `POST` | `/api/subjects/{subjectId}/vocabulary` | `VocabularyRequest` | `201` `Vocabulary` · `400` · `404` |
| `POST` | `/api/subjects/{subjectId}/vocabulary/bulk` | `BulkVocabularyRequest` | `201` `Vocabulary[]` · `400` · `404` |
| `PUT` | `/api/vocabulary/{id}` | `VocabularyRequest` | `200` `Vocabulary` · `400` · `404` |
| `DELETE` | `/api/vocabulary/{id}` | — | `204` · `404` |

```json
// VocabularyRequest
{ "term": "a menudo", "translation": "often", "notes": "Voy al cine a menudo." }

// BulkVocabularyRequest (all-or-nothing: if any item is invalid, nothing is saved)
{ "items": [
    { "term": "mientras", "translation": "while", "notes": null },
    { "term": "antes", "translation": "before; in the past" }
] }
```

### 5.6 Reordering

```json
// ReorderRequest: the COMPLETE list of ids of that course's categories (or subjects), in the new order
{ "ids": [3, 1, 2] }
```

The list must contain **every** id exactly once, with nothing missing, extra or duplicated. Otherwise the
response is `400`. To move one item up or down, swap it in your local list and send the whole list. The response
is the re-ordered list with updated `position` values, so use it as the new state.

---

## 6. Screens to build (matching the web app)

### Study mode

| Screen | Data | Behaviour |
|---|---|---|
| **Learning path** (home) | `GET /api/courses/{id}/subjects` | Numbered list in the order returned. Each row shows the title, description, a category chip, and "Lesson / No lesson · N words". Tapping a row opens the subject. |
| **Course picker** (app bar dropdown) | `GET /api/courses` | Shows "language level", e.g. "Spanish B2". Selecting one reloads the path. Remember the choice. If the saved course no longer exists, fall back to the first course. |
| **Categories** | `GET /api/courses/{id}/categories` + `…/subjects` | One card per category (in category order) listing its subjects, filtered by `categoryId`. |
| **Subject** | `GET /api/subjects/{id}`, `…/lesson`, `…/vocabulary` | Header with the title, description and category. Two tabs: **Lesson** (rendered Markdown, or "no lesson yet") and **Vocabulary (n)** (term / translation / notes list). A **Practise n flashcards** button, disabled when n = 0. |
| **Flashcards** | `GET /api/subjects/{id}/vocabulary` | See below. |
| **Settings** | local only | Mode (Study/Admin), course, flashcard front side, shuffle on/off. |

### Flashcard session (logic used by the web app)

```
deck  = shuffleSetting ? shuffled(vocabulary) : vocabulary
queue = deck;  missed = {};  total = deck.size

show front of queue[0]  (term or translation, depending on the "front" setting)
tap card            → flip to the back (the other side + notes if present)
"Got it"            → remove queue[0]
"Again"             → move queue[0] to the end of the queue; add its id to `missed`
progress            = (total - queue.size) / total

when queue is empty → summary: "You went through {total} cards and missed {missed.size}"
                      [Practise missed cards] → restart with only the missed items
                      [Start again]           → restart with the full vocabulary
```

The web app uses Space to flip and ←/→ for Again/Got it. On Android, consider swipe left = Again and
swipe right = Got it.

### Admin mode (shown only when Mode = Admin)

| Screen | Operations |
|---|---|
| **Courses** | List; create/edit form (language, level, description); delete with a strong warning that it cascades. After creating a course, select it. |
| **Categories** (for the selected course) | List in order with ↑/↓ reorder; create/edit (name, description); delete (cascades to subjects). |
| **Subjects** (for the selected course) | List in learning order with ↑/↓ reorder; create/edit (title, category picker, description); delete; link to the subject screen. If the course has no categories, ask the user to create one first. |
| **Subject → Lesson tab** | Markdown editor with live preview, Save (`PUT …/lesson`), Delete. Show "Unsaved changes" when the text differs from the server. |
| **Subject → Vocabulary tab** | Inline edit and delete per row, an add row, and **bulk import**: one entry per line, `term \| translation \| notes` (a tab also works as a separator; notes optional). Validate before sending (every line needs a term and a translation). |

### Local preferences (React uses localStorage; on Android use DataStore)

| Key | Values | Default |
|---|---|---|
| `mode` | `study` \| `admin` | `study` |
| `courseId` | `Long?` | first course |
| `cardFront` | `term` \| `translation` | `term` |
| `shuffleCards` | `Boolean` | `true` |

---

## 7. Kotlin starter code

Suggested stack: **Retrofit + OkHttp + kotlinx.serialization**, Coroutines, Jetpack Compose and DataStore.
For Markdown with tables, use **Markwon** (`io.noties.markwon:core` + `ext-tables`, rendered through
`AndroidView`) or a Compose Markdown library that supports GFM tables.

### Models

```kotlin
@Serializable data class Course(val id: Long, val language: String, val level: String,
                                val description: String? = null, val position: Int)

@Serializable data class Category(val id: Long, val courseId: Long, val name: String,
                                  val description: String? = null, val position: Int)

@Serializable data class Subject(val id: Long, val categoryId: Long, val categoryName: String,
                                 val courseId: Long, val title: String, val description: String? = null,
                                 val position: Int, val hasLesson: Boolean, val vocabularyCount: Long)

@Serializable data class Lesson(val id: Long, val subjectId: Long, val content: String,
                                val updatedAt: String) // ISO-8601; parse with Instant.parse() if needed

@Serializable data class Vocabulary(val id: Long, val subjectId: Long, val term: String,
                                    val translation: String, val notes: String? = null, val position: Int)

@Serializable data class CourseRequest(val language: String, val level: String, val description: String? = null)
@Serializable data class CategoryRequest(val name: String, val description: String? = null)
@Serializable data class SubjectRequest(val categoryId: Long, val title: String, val description: String? = null)
@Serializable data class LessonRequest(val content: String)
@Serializable data class VocabularyRequest(val term: String, val translation: String, val notes: String? = null)
@Serializable data class BulkVocabularyRequest(val items: List<VocabularyRequest>)
@Serializable data class ReorderRequest(val ids: List<Long>)

@Serializable data class ApiProblem(val title: String? = null, val status: Int? = null, val detail: String? = null)
```

### Retrofit interface

```kotlin
interface VibeLangApi {
    // Courses
    @GET("api/courses") suspend fun courses(): List<Course>
    @POST("api/courses") suspend fun createCourse(@Body body: CourseRequest): Course
    @PUT("api/courses/{id}") suspend fun updateCourse(@Path("id") id: Long, @Body body: CourseRequest): Course
    @DELETE("api/courses/{id}") suspend fun deleteCourse(@Path("id") id: Long): Response<Unit>

    // Categories
    @GET("api/courses/{courseId}/categories") suspend fun categories(@Path("courseId") courseId: Long): List<Category>
    @POST("api/courses/{courseId}/categories")
    suspend fun createCategory(@Path("courseId") courseId: Long, @Body body: CategoryRequest): Category
    @PUT("api/courses/{courseId}/categories/order")
    suspend fun reorderCategories(@Path("courseId") courseId: Long, @Body body: ReorderRequest): List<Category>
    @PUT("api/categories/{id}") suspend fun updateCategory(@Path("id") id: Long, @Body body: CategoryRequest): Category
    @DELETE("api/categories/{id}") suspend fun deleteCategory(@Path("id") id: Long): Response<Unit>

    // Subjects
    @GET("api/courses/{courseId}/subjects") suspend fun subjects(@Path("courseId") courseId: Long): List<Subject>
    @PUT("api/courses/{courseId}/subjects/order")
    suspend fun reorderSubjects(@Path("courseId") courseId: Long, @Body body: ReorderRequest): List<Subject>
    @GET("api/subjects/{id}") suspend fun subject(@Path("id") id: Long): Subject
    @POST("api/subjects") suspend fun createSubject(@Body body: SubjectRequest): Subject
    @PUT("api/subjects/{id}") suspend fun updateSubject(@Path("id") id: Long, @Body body: SubjectRequest): Subject
    @DELETE("api/subjects/{id}") suspend fun deleteSubject(@Path("id") id: Long): Response<Unit>

    // Lesson: Response<> so that 404 ("no lesson yet") can be handled without an exception
    @GET("api/subjects/{id}/lesson") suspend fun lesson(@Path("id") subjectId: Long): Response<Lesson>
    @PUT("api/subjects/{id}/lesson") suspend fun saveLesson(@Path("id") subjectId: Long, @Body body: LessonRequest): Lesson
    @DELETE("api/subjects/{id}/lesson") suspend fun deleteLesson(@Path("id") subjectId: Long): Response<Unit>

    // Vocabulary
    @GET("api/subjects/{id}/vocabulary") suspend fun vocabulary(@Path("id") subjectId: Long): List<Vocabulary>
    @POST("api/subjects/{id}/vocabulary")
    suspend fun addVocabulary(@Path("id") subjectId: Long, @Body body: VocabularyRequest): Vocabulary
    @POST("api/subjects/{id}/vocabulary/bulk")
    suspend fun addVocabularyBulk(@Path("id") subjectId: Long, @Body body: BulkVocabularyRequest): List<Vocabulary>
    @PUT("api/vocabulary/{id}") suspend fun updateVocabulary(@Path("id") id: Long, @Body body: VocabularyRequest): Vocabulary
    @DELETE("api/vocabulary/{id}") suspend fun deleteVocabulary(@Path("id") id: Long): Response<Unit>
}
```

### Client setup

```kotlin
val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

val okHttp = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(90, TimeUnit.SECONDS)   // Render free tier cold start
    .build()

val api: VibeLangApi = Retrofit.Builder()
    .baseUrl(BuildConfig.API_BASE_URL)   // must end with "/", e.g. "https://vibe-lang-api.onrender.com/"
    .client(okHttp)
    .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
    .build()
    .create(VibeLangApi::class.java)

/** Turns an HttpException into the server's human-readable message. */
fun Throwable.userMessage(): String =
    (this as? HttpException)?.response()?.errorBody()?.string()
        ?.let { runCatching { json.decodeFromString<ApiProblem>(it).detail }.getOrNull() }
        ?: message ?: "Something went wrong"
```

Notes:
- `ignoreUnknownKeys = true` lets the app keep working if the API adds fields later.
- Put the base URL in a `buildConfigField` per build type: `http://10.0.2.2:8080/` for debug and the Render URL
  for release.
- Android blocks plain `http://` by default. For local development, allow cleartext only in the **debug**
  build, e.g. with a `network_security_config.xml` that permits `10.0.2.2`.
- **CORS doesn't apply** to native apps, so the Android app needs no change to `CORS_ALLOWED_ORIGINS`.

---

## 8. Current limitations

- **No authentication or authorisation.** Anyone who can reach the API can modify content, and "Admin mode" is
  only a UI switch. Before publishing an app that points at a public API, add authentication on the server
  (for example an admin API key or user accounts with roles). The Android client will then need to send
  credentials on write requests.
- **No per-user progress.** Flashcard results aren't stored on the server. A "learned" or spaced-repetition
  feature would need new endpoints. Until then it could be stored locally, e.g. with Room.
- **No pagination.** List endpoints return everything. That's fine for the current content size.
- **No offline support.** Every screen loads from the API. Consider caching courses, subjects, lessons and
  vocabulary locally for offline study.
