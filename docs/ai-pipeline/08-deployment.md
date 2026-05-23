# 08 - Deployment

Deploy backend, frontend, database, Redis, and AI service with matching
configuration.

## Required AI Artifacts

After training, the AI container needs:

```text
ai_pipeline/model/model.json
ai_pipeline/model/model.txt
```

Generated artifacts are ignored by Git. Copy or mount them explicitly in the
deployment environment.

## Environment

| Variable | Default | Purpose |
|---|---|---|
| `AI_PIPELINE_ENABLED` | `true` | Backend calls AI when enabled |
| `AI_PIPELINE_BASE_URL` | `http://ai-pipeline:8000` | AI service URL |
| `AI_PIPELINE_FEATURE_SCHEMA_VERSION` | `v2` | Backend/AI schema contract |
| `AI_PIPELINE_MODEL_LOCATION` | `/app/model/model.json` | AI metadata path |

## Health Check

Use:

```powershell
Invoke-RestMethod http://localhost:8000/health
```

Expected result should show the service is ready and the schema is `v2`.

## Deploy Checklist

- Backend config uses schema `v2`.
- AI model artifacts come from the same training run.
- Docker compose config renders without errors.
- Backend tests pass.
- Frontend build passes.
- AI import/syntax smoke test passes.
