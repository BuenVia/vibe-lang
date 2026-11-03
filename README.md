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
  **Practice**, where you write the translation of each term.
- **Fill the gap** (web and Android) shows a subject's example sentences with the target word(s) blanked out,
  plus the translation and an optional hint (e.g. the infinitive). You type the missing word(s); the rule's
  explanation is shown after you check. Gaps are marked in the sentence text with double braces, and `/`
  separates accepted alternatives: `Si {{tuviera/tuviese}} tiempo, {{iría}}.`
- **Admin mode** is turned on in **Settings**. It adds an *Admin* section where you create, edit, delete and
  reorder courses, categories and subjects. It also adds editors on each subject page: a Markdown lesson editor
  with live preview, inline vocabulary editing with bulk import (`term | translation | notes`, one per line), and
  a sentence editor with bulk import (`sentence | translation | hint | explanation`, one per line).

The mode and the other preferences are saved in the browser's localStorage. There is no authentication, so
admin mode only hides the editing features and does not protect them.

## Further docs

- [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md): deploying the database to Supabase and the API and web app to Render
- [docs/ANDROID_API_GUIDE.md](docs/ANDROID_API_GUIDE.md): app purpose and full API reference for a Kotlin/Android client
- [docs/ROADMAP.md](docs/ROADMAP.md): designs for the next learning features (spaced repetition, speaking mode,
  sentence translation and a mistakes log)

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
| POST | `/api/subjects/{id}/sentences/bulk` | body `{ "items": [...] }` |
| PUT, DELETE | `/api/sentences/{id}` | |

Errors are returned as RFC 7807 problem details (`{ "status", "title", "detail" }`).
