param(
    [string] $BackendBaseUrl = "http://localhost:8080/api/v1",
    [string] $AiBaseUrl = "http://localhost:8000",
    [string] $AccessToken = "",
    [int] $Size = 10
)

$ErrorActionPreference = "Stop"

function Write-Step($Message) {
    Write-Host ""
    Write-Host "[test-ai-feed] $Message"
}

Write-Step "Checking AI health at $AiBaseUrl/health"
$health = Invoke-RestMethod -Method Get -Uri "$AiBaseUrl/health"
$health | ConvertTo-Json -Depth 5

if (-not $health.enabled) {
    throw "AI service is not enabled."
}
if (-not $health.model_available) {
    throw "AI model artifact is missing at the configured model path."
}

Write-Step "Checking direct AI predictions"
$payload = @{
    feature_schema_version = "v2"
    features = @(
        @{
            post_id = 900001
            post_features = @{
                content_length = 120
                has_multimedia = $true
                is_share_post = $false
                post_age_hours = 1.0
            }
            author_features = @{
                seniority_years = 2.0
                post_count = 40
                average_popularity = 6.5
            }
            interaction_features = @{
                interaction_count_7d = 3
                interaction_count_30d = 8
                hours_since_last_interaction = 5.0
                affinity_score = 0.4
            }
        },
        @{
            post_id = 900002
            post_features = @{
                content_length = 30
                has_multimedia = $false
                is_share_post = $false
                post_age_hours = 72.0
            }
            author_features = @{
                seniority_years = 0.1
                post_count = 1
                average_popularity = 0.2
            }
            interaction_features = @{
                interaction_count_7d = 0
                interaction_count_30d = 0
                hours_since_last_interaction = 999.0
                affinity_score = 0.0
            }
        }
    )
} | ConvertTo-Json -Depth 10

$predictionResponse = Invoke-RestMethod -Method Post -Uri "$AiBaseUrl/api/ranking/predict" -ContentType "application/json" -Body $payload
$predictions = if ($predictionResponse -is [System.Array]) { $predictionResponse } else { @($predictionResponse) }
if ($predictions.Count -ne 2) {
    throw "AI prediction endpoint returned $($predictions.Count) rows; expected 2."
}
$predictions | Format-Table post_id, score, feature_schema_version

Write-Step "Checking backend feed at $BackendBaseUrl/feed"
$headers = @{}
if ($AccessToken.Trim()) {
    $headers["Authorization"] = "Bearer $AccessToken"
}

$feedResponse = Invoke-RestMethod -Method Get -Uri "$BackendBaseUrl/feed?page=0&size=$Size" -Headers $headers
$items = if ($feedResponse.data -is [System.Array]) { $feedResponse.data } else { @($feedResponse.data) }
if ($items.Count -eq 0) {
    throw "Backend feed returned no posts. Seed or create posts before validating ranking order."
}

$ranked = for ($i = 0; $i -lt $items.Count; $i++) {
    [PSCustomObject]@{
        rank = $i
        postId = $items[$i].postId
        score = $items[$i].aiScore
        provider = $items[$i].rankingProvider
        source = $items[$i].source
        schema = $items[$i].featureSchemaVersion
        author = $items[$i].username
    }
}

$ranked | Format-Table -AutoSize

$aiCount = @($items | Where-Object { $_.rankingProvider -eq "AI" }).Count
$fallbackCount = @($items | Where-Object { $_.rankingProvider -eq "FALLBACK" }).Count

Write-Step "Provider summary"
Write-Host "AI rows:       $aiCount"
Write-Host "Fallback rows: $fallbackCount"

if ($aiCount -eq 0) {
    throw "Feed is not using AI predictions. Check backend AI_PIPELINE_ENABLED/base URL/schema/model health."
}

Write-Host ""
Write-Host "[test-ai-feed] OK: AI service predicts, backend feed returns ranked rows, and at least one feed row used AI."
