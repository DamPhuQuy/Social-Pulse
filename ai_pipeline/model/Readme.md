Model training output lives here.

Expected files after training:
- `model.json`: metadata artifact
- `model.ubj`: XGBoost booster sidecar
- `metrics.json`: training summary and metrics
- `plots/label_distribution.png`
- `plots/training_curves.png`
- `plots/feature_importance.png`

Recommended commands:

```powershell
cd e:\Projects\Social-Pulse\ai_pipeline
.\scripts\train-pilot.ps1
```

After the pilot looks reasonable:

```powershell
cd e:\Projects\Social-Pulse\ai_pipeline
.\scripts\train-full-gpu.ps1
```

What to check after training:
- `evaluation_warnings`
- `validation_ndcg_k` and `test_ndcg_k`
- `validation_rmse` vs `test_rmse`
- `evaluation_diagnostics.mean_label_baseline`
- `evaluation_diagnostics.<split>.ranking.ranked_group_count`
- `feature_importances`
- `plots/training_curves.png`
- `scan_stats.filter_reasons`
