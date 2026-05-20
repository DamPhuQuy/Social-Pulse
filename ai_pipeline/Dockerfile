FROM python:3.12-slim

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends libgomp1 \
    && rm -rf /var/lib/apt/lists/*

RUN pip install --no-cache-dir uv

COPY . .
RUN uv pip install --system --no-cache -e .

EXPOSE 8000

CMD ["uvicorn", "ai_pipeline.server:app", "--host", "0.0.0.0", "--port", "8000"]
