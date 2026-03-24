from fastapi import FastAPI

app = FastAPI(title="Social Pulse AI Service", version="0.1.0")


@app.get("/")
def read_root() -> dict[str, str]:
    return {"service": "ai", "status": "running"}


@app.get("/health")
def health_check() -> dict[str, str]:
    return {"status": "ok"}
