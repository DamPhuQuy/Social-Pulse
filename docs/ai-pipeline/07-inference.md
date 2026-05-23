# 07 - Inference

The AI service is a FastAPI application in `ai_pipeline/server.py`.

## Startup

On startup, `RankingService` loads:

- `model.json`
- `model.txt`

The service rejects artifacts whose `model_backend` is not `lightgbm` or whose
feature schema does not match the configured schema.

## Request Contract

Backend sends:

```json
{
  "feature_schema_version": "v2",
  "features": [
    {
      "post_features": {},
      "author_features": {},
      "interaction_features": {}
    }
  ]
}
```

`FeatureVectorizer` converts DTOs into the exact feature order from
`RankingFeatureSchema.FEATURE_ORDER`.

## Response Contract

AI returns one score per post:

```json
[
  {
    "post_id": 123,
    "score": 1.42,
    "feature_schema_version": "v2"
  }
]
```

Backend accepts predictions only when every candidate has exactly one score and
the schema version matches.

## Fallback

If inference is unavailable or invalid, backend uses `FallbackRankingService`.
Fallback scoring can use live counters from the database, but those counters are
not part of the AI feature contract.
