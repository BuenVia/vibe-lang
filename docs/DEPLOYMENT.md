# Deploying Vibe Lang (Supabase + Render)

This guide puts the three parts of the app on free hosting:

| Part | Where | Render service type |
|---|---|---|
| PostgreSQL database | **Supabase** (free project) | — |
| Spring Boot API (`backend/`) | **Render** | Web Service (Docker) |
| React app (`frontend/`) | **Render** | Static Site |

```
Browser ──► Render Static Site (React)  ──fetch──►  Render Web Service (Spring Boot)  ──JDBC──►  Supabase Postgres
            https://vibe-lang.onrender.com          https://vibe-lang-api.onrender.com          (session pooler)
```

The service names used below (`vibe-lang`, `vibe-lang-api`) are only examples. Render gives each service
its own `*.onrender.com` URL, so write down the real ones as you go.

> **Before you start, a security warning:** the API has **no authentication**. Admin mode in the web app only
> *hides* the editing buttons. Once the API is on the internet, anyone who finds its URL can create, change or
> delete your content. That is acceptable for a short-lived prototype, but add authentication before you put
> anything you care about online or share the link widely.

---

## 0. What's already prepared in the code

These are already done in this repository. You don't need to do anything for this step.

- `backend/Dockerfile` is needed because Render has no native Java runtime, so the API runs as a Docker container.
- `backend/src/main/resources/application.yml` reads all its settings from environment variables:

  | Variable | Purpose | Local default |
  |---|---|---|
  | `PORT` | HTTP port (Render sets this automatically) | `8080` |
  | `DB_URL` | JDBC URL of the database | `jdbc:postgresql://localhost:5432/vibelang` |
  | `DB_USERNAME` / `DB_PASSWORD` | Database credentials | `vibelang` / `vibelang` |
  | `DB_SCHEMA` | Postgres schema that holds the tables | `public` |
  | `DB_POOL_SIZE` | Max DB connections from the API | `5` |
  | `CORS_ALLOWED_ORIGINS` | Comma-separated browser origins allowed to call the API | `http://localhost:5173` |

- `frontend/src/api.js` reads `VITE_API_BASE_URL` **at build time**. When it is empty (local development), Vite's
  proxy forwards `/api` to `localhost:8080`.
- `frontend/.nvmrc` pins Node 22 for the Render build.

Flyway creates the tables and loads the sample content the first time the API connects to the new database,
so you never have to run SQL on Supabase by hand.

---

## 1. Push the project to GitHub

Render deploys from a Git repository. The project folder is not a Git repository yet.

1. Create an **empty** repository on GitHub (no README or .gitignore), e.g. `vibe-lang`. It can be private.
2. From the project root:

   ```bash
   cd ~/Projects/vibe-lang
   git init -b main
   git add .
   git commit -m "Initial commit"
   git remote add origin git@github.com:<your-user>/vibe-lang.git   # or the https URL
   git push -u origin main
   ```

   `.gitignore` already excludes `node_modules/`, `dist/` and `target/`. Check that `frontend/package-lock.json`
   **is** committed, because Render's build uses `npm ci`, which needs it.

---

## 2. Create the database on Supabase

1. Sign up at <https://supabase.com> and click **New project**.
2. Fill in:
   - **Name:** `vibe-lang`
   - **Database password:** generate a strong one and **save it**. You'll need it in step 3, and Supabase
     won't show it again (you can only reset it).
   - **Region:** choose the region closest to you, and pick the **same region for Render** in step 3
     (e.g. *Central EU (Frankfurt)*). Every API request crosses between the two, so being close together matters.
3. Wait a minute or two for the project to finish provisioning.
4. Click **Connect** (top of the project dashboard) and choose the **Session pooler** connection string.
   It looks like this:

   ```
   postgresql://postgres.abcdefghijklmnop:[YOUR-PASSWORD]@aws-0-eu-central-1.pooler.supabase.com:5432/postgres
   ```

   Pick out these parts:

   | Part | Example | Used as |
   |---|---|---|
   | user | `postgres.abcdefghijklmnop` | `DB_USERNAME` |
   | host | `aws-0-eu-central-1.pooler.supabase.com` | part of `DB_URL` |
   | port | `5432` | part of `DB_URL` |
   | database | `postgres` | part of `DB_URL` |

   Build the **JDBC URL** from it. Note the `jdbc:` prefix, and that the user and password are **not** in the URL:

   ```
   jdbc:postgresql://aws-0-eu-central-1.pooler.supabase.com:5432/postgres?sslmode=require
   ```

**Why the Session pooler?**
- The *Direct connection* host (`db.<ref>.supabase.co`) is IPv6-only on the free plan, and Render can't reach it.
- The *Transaction pooler* (port 6543) doesn't work well with the prepared statements that JDBC and Hibernate use.
- The Session pooler works over IPv4 and behaves like a normal Postgres connection.

**Why a separate schema?** In step 3 you'll set `DB_SCHEMA=vibelang`. Supabase automatically exposes the `public`
schema through its own public REST API (the "Data API"). Keeping the tables in a separate `vibelang` schema
keeps them out of that API, so the Spring Boot API stays the only way in. Flyway creates the schema automatically.

---

## 3. Deploy the API on Render (Web Service)

1. Sign up at <https://render.com> and connect your GitHub account.
2. Click **New → Web Service** and select the `vibe-lang` repository.
3. Settings:

   | Field | Value |
   |---|---|
   | Name | `vibe-lang-api` |
   | Language / Runtime | **Docker** |
   | Branch | `main` |
   | Region | same as Supabase (e.g. Frankfurt) |
   | Root Directory | `backend` |
   | Dockerfile Path | `./Dockerfile` (the default, relative to the root directory) |
   | Instance Type | **Free** |

4. **Environment variables:**

   | Key | Value |
   |---|---|
   | `DB_URL` | `jdbc:postgresql://<pooler-host>:5432/postgres?sslmode=require` |
   | `DB_USERNAME` | `postgres.<project-ref>` |
   | `DB_PASSWORD` | your Supabase database password |
   | `DB_SCHEMA` | `vibelang` |
   | `DB_POOL_SIZE` | `5` |
   | `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` for now. You'll change it in step 5. |

   Don't set `PORT`. Render provides it.

5. Under **Advanced**, set **Health Check Path** to `/api/courses`.
6. Click **Create Web Service**. The first build takes several minutes, because Maven downloads its dependencies
   inside Docker.
7. When the log shows `Started VibeLangApplication`, open
   `https://vibe-lang-api.onrender.com/api/courses` (use your real URL). You should see the Spanish B2 and
   Catalan A1 courses as JSON.

   In Supabase, **Table Editor** (switch the schema dropdown to `vibelang`) now shows the tables, plus
   `flyway_schema_history`.

---

## 4. Deploy the web app on Render (Static Site)

1. Click **New → Static Site** and select the same repository.
2. Settings:

   | Field | Value |
   |---|---|
   | Name | `vibe-lang` |
   | Branch | `main` |
   | Root Directory | `frontend` |
   | Build Command | `npm ci && npm run build` |
   | Publish Directory | `dist` |

3. **Environment variable:**

   | Key | Value |
   |---|---|
   | `VITE_API_BASE_URL` | `https://vibe-lang-api.onrender.com` (your API URL from step 3, no trailing `/api`) |

   Vite bakes this value into the JavaScript **when it builds**. If you change it later, you have to trigger a new
   deploy (**Manual Deploy → Clear build cache & deploy**) for the change to take effect.

4. Click **Create Static Site**.
5. After it deploys, open the site's **Redirects/Rewrites** tab and add this rule:

   | Source | Destination | Action |
   |---|---|---|
   | `/*` | `/index.html` | **Rewrite** |

   React Router handles URLs such as `/subjects/3` in the browser. Without this rule, refreshing such a page or
   opening it directly returns a 404 from Render.

---

## 5. Connect the two (CORS)

The browser only lets the web app call the API if the API lists the web app's origin.

1. Copy the static site's URL, e.g. `https://vibe-lang.onrender.com`.
2. In the **API** service, go to **Environment** and set:

   ```
   CORS_ALLOWED_ORIGINS=https://vibe-lang.onrender.com,http://localhost:5173
   ```

   Use the exact origin: `https`, no trailing slash, no path. Keeping `http://localhost:5173` lets you run the
   local frontend against the deployed API.
3. Save. Render redeploys the API with the new setting.

---

## 6. Check that it works

1. Open `https://vibe-lang.onrender.com`. The learning path for Spanish B2 should appear. The first request can
   take up to about a minute if the API was asleep (see below).
2. Open a subject, read the lesson and practise its flashcards.
3. Go to **Settings → Admin**, then create a test category and delete it again.
4. Refresh the browser on a subject page. It should reload the page, not show a 404.

---

## Things to know about the free tiers

- **The Render free Web Service sleeps** after about 15 minutes without requests. The next request wakes it up,
  which takes roughly 30–60 seconds for a Spring Boot app. Until then the web app shows "Loading…".
  Static Sites don't sleep.
- Render free instances have **512 MB RAM**. The Dockerfile limits the JVM to fit (`-XX:MaxRAMPercentage=75`).
- Free Render services have a monthly allowance of instance hours, which is enough for one always-sleeping
  prototype API.
- **Supabase free projects are paused** after a period of inactivity (about a week). You restore one from the
  Supabase dashboard, and your data is kept.
- Supabase free projects have a database size limit (a few hundred MB). That is far more than lessons and
  vocabulary will ever need.

Free-tier limits change from time to time. Check the current details on each provider's pricing page.

---

## Updating the app later

- **Code:** push to `main`. Render rebuilds and redeploys both services automatically (auto-deploy is on by default).
- **Database changes:** add a new Flyway migration file, e.g.
  `backend/src/main/resources/db/migration/V3__add_example_column.sql`. It runs automatically when the API starts.
  Never edit a migration that has already run (`V1`, `V2`), because Flyway would refuse to start.
- **Sample content:** `V2__seed_data.sql` runs only once per database, so later changes to your content are made
  through admin mode, not in that file.

---

## Troubleshooting

| Symptom | Likely cause and fix |
|---|---|
| Browser console: *blocked by CORS policy* | `CORS_ALLOWED_ORIGINS` doesn't exactly match the site's origin (check `https`, no trailing slash). The API must redeploy after you change it. |
| Web app shows *Failed to fetch*, or calls `/api/...` on its own domain | `VITE_API_BASE_URL` was missing or wrong **when the site was built**. Fix it, then run *Clear build cache & deploy*. |
| 404 when refreshing a page like `/subjects/3` | The `/*` → `/index.html` rewrite rule is missing (step 4.5). |
| API log: `Connection refused`, `Network is unreachable`, or `UnknownHostException` | You used the *Direct connection* host. Use the **Session pooler** host and the `postgres.<project-ref>` username. |
| API log: `password authentication failed` | Wrong `DB_PASSWORD`, or `DB_USERNAME` is missing the `.project-ref` suffix. |
| API log: `prepared statement "S_1" already exists` | You're on the transaction pooler (port **6543**). Switch to port **5432** (session pooler). |
| API log: `Max client connections reached` | Lower `DB_POOL_SIZE`, e.g. `3`. |
| Everything is very slow on the first request | The free API instance was asleep. Wait about a minute. |
| API log: Flyway *"PostgreSQL xx is newer than this version of Flyway"* warning | Harmless. Migrations still run. |
