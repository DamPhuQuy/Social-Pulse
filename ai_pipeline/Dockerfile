FROM python:3.12-alpine

WORKDIR /app

RUN pip install --no-cache-dir uv

COPY pyproject.toml .
RUN uv pip install --system --no-cache -e .

COPY . .

EXPOSE 8000

CMD ["uvicorn", "ai_pipeline.server:app", "--host", "0.0.0.0", "--port", "8000"]
