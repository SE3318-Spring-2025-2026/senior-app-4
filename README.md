# SPMS Local Setup

This repository contains the frontend and backend for the Senior Project Management System (SPMS).

## Project Structure

- `frontend/`: Next.js application running on `http://localhost:3000`
- `backend/`: Spring Boot API running on `http://localhost:8080`
- `docs/`: project documents

## Prerequisites

- Node.js and npm
- Java 17 or newer
- Maven

## Frontend Setup

Create `frontend/.env.local` with this value:

```bash
NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1
```

Then start the frontend:

```bash
cd frontend
npm install
npm run dev
```

## Backend OAuth Setup

For backend local development, copy the example file:

```bash
cd backend
cp application.properties.example application.properties
```

Then open `backend/application.properties` and fill these values:

```properties
github.client-id=your-shared-team-client-id
github.client-secret=your-shared-team-client-secret
github.redirect-uri=http://localhost:3000/auth/callback
```

Important notes:

- `backend/application.properties` is local-only and gitignored.
- Spring Boot auto-loads `application.properties` when you run the backend from the `backend/` directory.
- Share real GitHub OAuth credentials privately with teammates. Do not commit them to GitHub.
- The GitHub OAuth App must use `http://localhost:3000/auth/callback` as its callback URL.
- This replaces the old `source .env.example` workflow.
- For the current setup, do not add placeholder `SUPABASE_URL` or `SUPABASE_SERVICE_KEY` values into `backend/application.properties`.

Start the backend from the terminal like this:

```bash
cd backend
mvn spring-boot:run
```

### Why This Works

Spring Boot automatically reads `application.properties` from the current working directory as part of its normal external configuration system.

That means:

- `backend/application.properties` is picked up automatically
- no manual `export` step is needed
- no `set -a`, `source`, or shell-specific setup is needed anymore

The old `.env.example` approach required manual shell loading because Spring Boot does not auto-read `.env.example`.

## Supabase Note

For the current project state, teammates do not need to manually set `SUPABASE_URL` and `SUPABASE_SERVICE_KEY` as long as the existing backend fallback configuration in `application.yml` remains unchanged.

If those fallback values are removed later, teammates will also need the private Supabase values before the backend can work correctly.

If you decide to add `SUPABASE_URL` and `SUPABASE_SERVICE_KEY` into your local backend config later, they must be real values. Placeholder values will break student validation and GitHub login.

## Running The Full Project

1. Start the frontend with `npm run dev` inside `frontend/`.
2. Copy `backend/application.properties.example` to `backend/application.properties`.
3. Fill the three GitHub values in `backend/application.properties`.
4. Start the backend with `mvn spring-boot:run` inside `backend/`.
5. Open `http://localhost:3000/auth/login`.

## Student OAuth Test Flow

Use the valid student ID `11111111111` for the current test flow.

Expected behavior:

1. Enter `11111111111` on `/auth/login`.
2. Click `Continue with GitHub`.
3. Confirm the redirect URL contains:
   - a real `client_id`
   - `redirect_uri=http://localhost:3000/auth/callback`
   - `state=11111111111`
4. Complete GitHub authorization.
5. Return to `/auth/callback` and then reach `/dashboard`.

## Troubleshooting

### `placeholder-client-id` appears in the GitHub URL

The local `backend/application.properties` file is missing, not filled, or still contains placeholder values.

### `http://localhost:8080/api/v1/auth/github/callback` appears in the GitHub URL

An old backend process is still running or the backend was started with old configuration.

### Student login returns `500`

Most likely causes:

- backend was started with broken or placeholder Supabase runtime values
- the local `backend/application.properties` file contains extra invalid values

For the current setup, keep only the three GitHub properties in `backend/application.properties` unless you also have real Supabase values.

### Backend does not start on `8080`

Another process may already be using the port. Stop the old backend process and try again.

## Security Note

Before pushing this repository, make sure no real GitHub OAuth secrets remain inside tracked files.

`backend/application.properties` is intended to stay local and gitignored. If a real secret was ever written into a tracked file, rotate it in GitHub and replace the committed value with a placeholder.
