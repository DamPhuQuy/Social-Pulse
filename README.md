# Social Pulse

Social Pulse is a topic-based social network that helps users discover and discuss content through explicit topic follows, user follows, and transparent rule-based feed ranking.

> **Định vị sản phẩm**: Social Pulse là mạng xã hội khám phá nội dung theo chủ đề, trong đó người dùng chủ động lựa chọn chủ đề và người dùng muốn theo dõi. Bảng tin được xây dựng bằng các quy tắc minh bạch dựa trên mối quan hệ, thời gian đăng và mức độ tương tác.

## 🚀 Architecture

```text
Frontend (React + Vite + TypeScript)
        ↓
Spring Boot Backend (Java 21)
        ↓
PostgreSQL 17 (Core Business Data) + Redis 7 (Cache / Rate Limiting)
        ↓
Transparent Rule-Based Feed Engine
```

## 🛠️ Stack

- **Frontend**: React, Vite, TypeScript, Tailwind CSS (Meta Design System).
- **Backend**: Java 21, Spring Boot 3, PostgreSQL, Redis, Flyway.
- **Feed Engine**: Rule-Based Ranking (`Recency * 0.6 + Engagement * 0.4`) & Chronological Feed.
- **Runtime**: Docker Compose.

## ⚡ Quick Start (Docker)

Create `.env` from `.env.example`, then run:

```bash
docker compose up -d --build
```

Services:
- **Frontend**: `http://localhost:5173` (or port 80 in prod)
- **Backend API**: `http://localhost:8080/api/v1`
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`

## 💻 Run Locally For Development

1. Start Infrastructure (Database & Cache):

```bash
docker compose up -d db redis
```

2. Start Backend:

```bash
cd backend
./mvnw spring-boot:run
```

3. Start Frontend:

```bash
cd frontend
npm install
npm run dev
```

## 🧪 Testing & Verification

- Backend unit tests: `cd backend && ./mvnw test`
- Frontend build: `cd frontend && npm run build`
