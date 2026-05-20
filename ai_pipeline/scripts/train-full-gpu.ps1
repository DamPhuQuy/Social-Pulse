Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$submissions = Join-Path $root "data\RS_2019-04.zst"
$comments = Join-Path $root "data\RC_2019-04.zst"
$output = Join-Path $root "model\model.json"
$metrics = Join-Path $root "model\metrics.json"
$plots = Join-Path $root "model\plots"

if (-not (Test-Path $submissions)) {
    throw "Missing submissions archive: $submissions"
}
if (-not (Test-Path $comments)) {
    throw "Missing comments archive: $comments"
}

Write-Host "[train-full-gpu] Starting full GPU training..."
Write-Host "  submissions: $submissions"
Write-Host "  comments:    $comments"
Write-Host "  output:      $output"
Write-Host "  metrics:     $metrics"
Write-Host "  plots:       $plots"

uv run train `
  --submissions $submissions `
  --comments $comments `
  --output $output `
  --metrics-output $metrics `
  --plots-output-dir $plots `
  --sample-size 200000 `
  --scan-limit-posts 0 `
  --scan-limit-comments 0 `
  --negative-samples-per-post 3 `
  --validation-ratio 0.2 `
  --test-ratio 0.1 `
  --n-estimators 1200 `
  --learning-rate 0.05 `
  --max-depth 8 `
  --min-child-weight 8 `
  --subsample 0.85 `
  --colsample-bytree 0.8 `
  --reg-lambda 1.5 `
  --reg-alpha 0.05 `
  --max-bin 256 `
  --early-stopping-rounds 80 `
  --device cuda `
  --n-jobs 0
