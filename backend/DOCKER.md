# Social-Pulse Backend Docker Setup

This directory contains the Docker configuration to run the Social-Pulse backend along with its databases: **PostgreSQL** and **Neo4j**.

## Prerequisites

- [Docker](https://docs.docker.com/get-docker/)
- [Docker Compose](https://docs.docker.com/compose/install/)

---

## Configuration

The containers are configured using environment variables defined in the `.env` file. A default `.env` is created for you, but you can copy `.env.example` to restore defaults or customize values:

```bash
cp .env.example .env
```

Default Database Settings:
- **PostgreSQL Port:** `5432`
- **PostgreSQL Database:** `postgres`
- **PostgreSQL Credentials:** `postgres`/`postgres`
- **Neo4j Bolt Port:** `7687`
- **Neo4j HTTP UI Port:** `7474`
- **Neo4j Credentials:** `neo4j`/`password`

---

## Running the Services

### 1. Run Everything (Databases + Backend Application)

To build the Spring Boot application and start all services (PostgreSQL, Neo4j, and the Backend):

```bash
docker compose up --build -d
```

- **Backend API:** Available at `http://localhost:8080`
- **Swagger / OpenAPI Documentation:** Available at `http://localhost:8080/swagger-ui.html`
- **Neo4j Browser Console:** Available at `http://localhost:7474`
- **PostgreSQL:** Available at `localhost:5432`

### 2. Run Databases Only (For Local Development)

If you are developing/running the Spring Boot application locally on your host machine (e.g., via IDE or `./gradlew bootRun`) and only want the databases to run in Docker:

```bash
docker compose up postgres neo4j -d
```

The Spring Boot backend will connect to them automatically using the defaults defined in `application.yaml`.

---

## Common Commands

### Stop All Services
To stop and remove containers while preserving your database volumes:
```bash
docker compose down
```

### Stop All Services and Delete Volumes
To reset your databases and delete all stored data:
```bash
docker compose down -v
```

### View Logs
To check the logs of all running services or a specific service:
```bash
# All logs
docker compose logs -f

# Backend only
docker compose logs -f backend

# Neo4j only
docker compose logs -f neo4j
```

---

## Troubleshooting

### APOC Plugins in Neo4j
The Neo4j service includes the **APOC** library out of the box (via `NEO4J_PLUGINS: '["apoc"]'`). 

### Verifying DB Health
The `docker-compose.yml` utilizes health checks:
- **PostgreSQL** uses `pg_isready` to verify that the server is ready to accept connections.
- **Neo4j** uses `wget` against its HTTP endpoint to wait until the interface is operational.

The `backend` service will wait until both databases report `healthy` before booting up.
