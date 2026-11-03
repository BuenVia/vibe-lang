# Vibe Lang

Prototype for learning and practising grammar: Spanish B2 and Catalan A1 for now.

- `backend/` is a Java 21 / Spring Boot 3.5 REST API with PostgreSQL and Flyway migrations
- `frontend/` is React 18 (JavaScript) with Vite and React Router; lessons are rendered with react-markdown
- `android/` is a native Android app (Kotlin, Jetpack Compose) for studying on a phone; see [android/README.md](android/README.md)

## Data model

```
Course (language + level, e.g. Spanish B2)
 └─ Category        (e.g. Verbs: Past)            ordered within the course
     └─ Subject     (e.g. Pretérito imperfecto)   learning order within the course
         ├─ Lesson      (one per subject, Markdown)
         ├─ Vocabulary  (term, translation, optional notes; many per subject)
         └─ Sentence    (text with {{gaps}}, optional translation, hint, explanation; many per subject)
```

`Course` sits above `Category` so that Spanish B2, Catalan A1 and later Italian, English and so on stay
separate. Deleting a parent also deletes its children (`ON DELETE CASCADE`).

## Prerequisites

- JDK 21 and Maven 3.9+ (for example `sdk install java 21-tem && sdk install maven`)
- Node.js 20+ and npm
- PostgreSQL (running locally)

## 1. Database

```bash
sudo -u postgres psql -f db/setup.sql   # creates role + database "vibelang" (password "vibelang")
```

Flyway creates the tables when the API starts and loads sample content (`V2__seed_data.sql`).
To use other credentials, set `DB_URL`, `DB_USERNAME` and `DB_PASSWORD`.

## 2. API (http://localhost:8080)

```bash
cd backend
mvn spring-boot:run
```

## 3. Web app (http://localhost:5173)

```bash
cd frontend
npm install
npm run dev
```

Vite proxies `/api` to the backend.

## Using it

- **Learner mode** (the default) shows the learning path, categories, lessons, vocabulary lists and **Study**
  (flashcards). The Study keys are Space to flip, → for "got it" and ← for "again". The Android app also has
  **Practice**, where you see each word's English and write (or say, with 🎤) the Spanish.
- **Fill the gap** (web and Android) shows a subject's example sentences with the target word(s) blanked out,
  plus the translation and an optional hint (e.g. the infinitive). You type the missing word(s); the rule's
  explanation is shown after you check. Gaps are marked in the sentence text with double braces, and `/`
  separates accepted alternatives: `Si {{tuviera/tuviese}} tiempo, {{iría}}.`
- **Review** (web and Android) is spaced repetition across the whole course. Every answer in Study, Practice,
  Fill the gap and Review moves the item through Leitner boxes 1–5 (due again after 1, 2, 4, 8, 16 days). A
  wrong answer sends it back to box 1, and with the **speed rule** a right answer only moves it up when it was
  given within the time limit (5 s for a typed answer by default, 3 s to flip a flashcard). The learning path
  shows "Today's review" (due and new items) and a progress bar per subject. The time limit, the speed rule and
  the number of new items per day are in Settings.
- **Speaking** (web and Android): in Fill the gap, Practice and Review, 🎤 lets you say the Spanish (a word or the
  whole sentence) instead of typing. Each gap is checked against what the recogniser heard (filler words are ignored). 🔊 reads the
  Spanish aloud, and "Read answers aloud" in Settings does it after every Check. It uses the device's own speech
  recogniser and text-to-speech (Android) or the Web Speech API (Chrome/Edge), so there are no API costs.
- **Translate** (web and Android) shows a sentence's English translation and asks for the whole Spanish
  sentence, typed or spoken. A match with any accepted wording is right straight away; otherwise a word diff
  against the closest model answer shows what's missing, extra or mis-accented, and you grade it Right, Nearly
  (counts, but doesn't move up in Review) or Wrong.
- **Mistakes log** (web and Android): "+ Add mistake" records what you said and the corrected sentence, with the
  fixed words as `{{gaps}}` ("Mark differences" does this for you). Mistakes are ordinary sentences, so they
  turn up in Fill the gap, Translate and Review with "You once said: ~~…~~" in the feedback. **My mistakes**
  lists them newest first and practises them. On Android you can also share text from another app (e.g. a
  chat message) to Vibe Lang to start a mistake.
- **Admin mode** is turned on in **Settings**. It adds an *Admin* section where you create, edit, delete and
  reorder courses, categories and subjects. It also adds editors on each subject page: a Markdown lesson editor
  with live preview, inline vocabulary editing with bulk import (`term | translation | notes`, one per line), and
  a sentence editor with bulk import (`sentence | translation | hint | explanation`, one per line).

The mode and the other preferences are saved in the browser's localStorage. There is no authentication, so
admin mode only hides the editing features and does not protect them.

## Further docs

- [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md): deploying the database to Supabase and the API and web app to Render
- [docs/ANDROID_API_GUIDE.md](docs/ANDROID_API_GUIDE.md): app purpose and full API reference for a Kotlin/Android client
- [docs/ROADMAP.md](docs/ROADMAP.md): the designs behind spaced repetition, speaking, Translate and the mistakes
  log (all built)

## API

| Method | Path | |
|---|---|---|
| GET, POST | `/api/courses` | |
| GET, PUT, DELETE | `/api/courses/{id}` | |
| GET, POST | `/api/courses/{courseId}/categories` | |
| PUT | `/api/courses/{courseId}/categories/order` | body `{ "ids": [...] }` |
| GET, PUT, DELETE | `/api/categories/{id}` | |
| GET | `/api/courses/{courseId}/subjects` | in learning order |
| PUT | `/api/courses/{courseId}/subjects/order` | body `{ "ids": [...] }` |
| POST | `/api/subjects` | body includes `categoryId` |
| GET, PUT, DELETE | `/api/subjects/{id}` | |
| GET, PUT, DELETE | `/api/subjects/{id}/lesson` | PUT creates or replaces |
| GET, POST | `/api/subjects/{id}/vocabulary` | |
| POST | `/api/subjects/{id}/vocabulary/bulk` | body `{ "items": [...] }` |
| PUT, DELETE | `/api/vocabulary/{id}` | |
| GET, POST | `/api/subjects/{id}/sentences` | `text` must contain a `{{gap}}` |
| GET | `/api/courses/{courseId}/sentences` | `?source=MISTAKE` for the mistakes log, newest first |
| POST | `/api/subjects/{id}/sentences/bulk` | body `{ "items": [...] }` |
| PUT, DELETE | `/api/sentences/{id}` | |
| POST | `/api/reviews` | report one answer (spaced repetition) |
| GET | `/api/courses/{courseId}/due` | `?newLimit=10&zone=Europe/Madrid&limit=50` |
| GET | `/api/courses/{courseId}/progress` | boxes per subject; `?zone=…` |

Errors are returned as RFC 7807 problem details (`{ "status", "title", "detail" }`).
