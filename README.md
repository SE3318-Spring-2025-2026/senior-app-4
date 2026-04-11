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

## Backend Setup

The backend connects directly to Supabase PostgreSQL via JDBC. There is no Supabase REST API or service key involved.

Copy the example file:

```bash
cd backend
cp application.properties.example application.properties
```

Then open `backend/application.properties` and fill all values:

```properties
github.client-id=your-shared-team-client-id
github.client-secret=your-shared-team-client-secret
github.redirect-uri=http://localhost:3000/auth/callback

db.url=jdbc:postgresql://postgres:[PASSWORD]@db.[PROJECT-REF].supabase.co:5432/postgres
```

You can find the connection string in:
**Supabase Dashboard → Project Settings → Database → Connection string → URI**

Copy the URI value and prefix it with `jdbc:` before pasting into `db.url`.

Important notes:

- `backend/application.properties` is local-only and gitignored.
- Spring Boot auto-loads `application.properties` when you run the backend from the `backend/` directory.
- Share real credentials privately with teammates. Do not commit them to GitHub.
- The GitHub OAuth App must use `http://localhost:3000/auth/callback` as its callback URL.

Start the backend:

```bash
cd backend
mvn spring-boot:run
```

## API Documentation (Swagger)

Once the backend is running, you can access:

- **Swagger UI** → http://localhost:8080/swagger-ui.html
- **OpenAPI JSON** → http://localhost:8080/v3/api-docs

## Running The Full Project

1. Create `frontend/.env.local` with the API URL.
2. Copy `backend/application.properties.example` to `backend/application.properties`.
3. Fill all five values in `backend/application.properties` (3 GitHub + 2 database).
4. Start the backend with `mvn spring-boot:run` inside `backend/`.
5. Start the frontend with `npm run dev` inside `frontend/`.
6. Open `http://localhost:3000/auth/login`.

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

- `db.url`, `db.username`, or `db.password` is missing or incorrect in `backend/application.properties`
- The database tables (`users`, `valid_student_ids`) do not exist in the Supabase project

### Backend does not start on `8080`

Another process may already be using the port. Stop the old backend process and try again.

## Security Note

Before pushing this repository, make sure no real credentials remain inside tracked files.

`backend/application.properties` is intended to stay local and gitignored. If a real secret was ever written into a tracked file, rotate it and replace the committed value with a placeholder.
