# 🌟 Social Pulse - Smart Social Media

> Nền tảng mạng xã hội hỗ trợ bởi AI.

---

## 🛠 Tech Stack
- **Frontend**: React 19, Vite 8, Tailwind 4.
- **Backend**: Java 25, Spring Boot 4.0.3, PostgreSQL, Redis.
- **AI**: Python 3.12, FastAPI, Scikit-learn.

---

## 🚀 Hướng dẫn chạy dự án

### 1. Cấu hình môi trường
Nếu chưa có file `.env`, hãy copy từ file mẫu:
```bash
cp .env.example .env
```

### 2. Chạy bằng Docker (Nhanh nhất)
```bash
# Khởi động toàn bộ dịch vụ
docker-compose up -d --build

# Dừng các dịch vụ
docker-compose down
```
**Truy cập:**
- Frontend: [http://localhost:5173](http://localhost:5173)
- API Docs: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- Database (pgAdmin): [http://localhost:5050](http://localhost:5050)
---

### 3. Chạy thủ công (Dành cho Dev)

#### A. Database & Redis
```bash
docker-compose up -d db redis
```

#### B. Backend
```bash
cd backend
./mvnw spring-boot:run
```

#### C. Frontend
```bash
cd frontend
npm install && npm run dev
```

#### D. AI Service
```bash
cd ai
pip install -r requirements.txt
python -m uvicorn app.main:app --reload --port 8000
```

---

## 📂 Cấu trúc
- `/frontend`: React App.
- `/backend`: Spring Boot API.
- `/ai`: AI Recommendation Service.
- `docker-compose.yaml`: Cấu hình hạ tầng.
