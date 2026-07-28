# Social Pulse

Social Pulse is a social network prototype with a React frontend, Spring Boot backend, PostgreSQL, Redis, and a Python LightGBM feed-ranking service.

## Stack

- Frontend: React, Vite, TypeScript, Tailwind CSS.
- Backend: Java, Spring Boot, PostgreSQL, Redis, Flyway.
- AI: Python, FastAPI, LightGBM.
- Runtime: Docker Compose.

## Run With Docker

Create `.env` from `.env.example`, then run:

```powershell
docker compose up -d --build
```

Services:

- Frontend: `http://localhost:5173`
- Backend API: `http://localhost:8080/api/v1`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- AI health: `http://localhost:8001/health`

## Run Locally For Development

Infrastructure:

```powershell
docker compose up -d db redis
```

AI service:

```powershell
cd ai_pipeline
uv run uvicorn ai_pipeline.server:app --host 0.0.0.0 --port 8000 --reload
```

Backend:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Frontend:

```powershell
cd frontend
npm install
npm run dev
```
