# SPMS — Senior Project Management System

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| **Backend** | Java | 17 |
| | Spring Boot | 3.3.4 |
| | Spring Data JPA | (via Boot) |
| | Spring Validation | (via Boot) |
| | Spring AOP | (via Boot) |
| | PostgreSQL Driver | (via Boot) |
| | Springdoc OpenAPI | 2.6.0 |
| | Lombok | (via Boot) |
| | REST Assured (test) | 5.4.0 |
| | JUnit Jupiter (test) | (via Boot) |
| | Maven | 3.x |
| **Frontend** | Next.js | 16.2.2 |
| | React | 19.2.4 |
| | TypeScript | 5.x |
| | Tailwind CSS | 4.x |
| | Axios | 1.x |
| | next-auth | 4.x |
| | Sonner (toasts) | 2.x |
| | Lucide React (icons) | 1.x |
| **Database** | PostgreSQL via Supabase | — |
| **Auth** | JWT (HS256) + GitHub OAuth | — |

## Project Structure

- `frontend/` — Next.js application running on `http://localhost:3000`
- `backend/` — Spring Boot API running on `http://localhost:8080`
- `docs/` — project documents, OpenAPI specs, DFDs

## Prerequisites

- Node.js 18+ and npm
- Java 17
- Maven 3.x

## Frontend Setup

Create `frontend/.env.local`:

```bash
NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1
```

Start the frontend:

```bash
cd frontend
npm install
npm run dev
```

## Backend Setup

The backend connects directly to Supabase PostgreSQL via JDBC.

Copy the example config:

```bash
cd backend
cp application.properties.example application.properties
```

Fill all values in `backend/application.properties`:

```properties
github.client-id=your-shared-team-client-id
github.client-secret=your-shared-team-client-secret
github.redirect-uri=http://localhost:3000/auth/callback

db.url=jdbc:postgresql://postgres:[PASSWORD]@db.[PROJECT-REF].supabase.co:5432/postgres
```

You can find the connection string in:
**Supabase Dashboard → Project Settings → Database → Connection string → URI**

Copy the URI value and prefix it with `jdbc:` before pasting into `db.url`.

> `backend/application.properties` is local-only and gitignored. Share real credentials privately with teammates — never commit them.

Start the backend:

```bash
cd backend
mvn spring-boot:run
```

## Running the Full Project

1. Create `frontend/.env.local` with the API URL.
2. Copy `backend/application.properties.example` → `backend/application.properties` and fill all values.
3. Start the backend: `cd backend && mvn spring-boot:run`
4. Start the frontend: `cd frontend && npm run dev`
5. Open `http://localhost:3000/auth/login`.

## API Documentation (Swagger)

Once the backend is running:

- **Swagger UI** → http://localhost:8080/swagger-ui.html
- **OpenAPI JSON** → http://localhost:8080/v3/api-docs
