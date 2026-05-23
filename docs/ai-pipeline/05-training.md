# 05 - Training

Training uses LightGBM through `ai_pipeline/training/trainer.py`.

## Commands

Pilot run:

```powershell
cd e:\Projects\Social-Pulse\ai_pipeline
.\scripts\train-pilot.ps1
```

Full GPU run:

```powershell
cd e:\Projects\Social-Pulse\ai_pipeline
.\scripts\train-full-gpu.ps1
```

## Model Backend

The only supported backend is LightGBM. The pipeline writes:

- `model.json`: metadata, schema, preprocessing, metrics, diagnostics
- `model.txt`: LightGBM booster used by inference
- `metrics.json`: report-friendly training summary
- `plots/*.png`: training visualization

## GPU Use

`--device gpu` asks LightGBM to use GPU. If GPU training fails and CPU fallback
is enabled, the run continues on CPU and records the actual backend/device in
metrics. For a final report, record which device was actually used.

The `train-full-gpu.ps1` entry point disables CPU fallback so a missing GPU
configuration fails fast instead of silently producing a CPU-trained model.

## Overfit Checks

The training summary includes warnings for:

- train RMSE much lower than validation RMSE
- validation RMSE much lower than test RMSE
- suspiciously perfect NDCG
- `post_id` overlap across splits
- one feature dominating gain importance

Treat warnings as blockers unless there is a clear explanation.
