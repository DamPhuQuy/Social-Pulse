PYTHON = ai_pipeline/.venv/bin/python
PYTHONPATH_ENV = PYTHONPATH=$(CURDIR)

.PHONY: train serve test lint docker clean

## Training pipeline (data ingestion → feature engineering → preprocessing → training → evaluation)
train:
	$(PYTHONPATH_ENV) $(PYTHON) -m ai_pipeline.training.main

## Inference server
serve:
	$(PYTHONPATH_ENV) $(PYTHON) -m uvicorn ai_pipeline.server:app --host 0.0.0.0 --port 8000 --reload

## Verify pipeline (syntax + imports + smoke test)
test:
	$(PYTHONPATH_ENV) $(PYTHON) -c "import ast; from pathlib import Path; [ast.parse(f.read_text()) for f in Path('ai_pipeline').rglob('*.py') if '.venv' not in str(f)]; from ai_pipeline.training import PushshiftTrainingPipeline, TrainingArguments; from ai_pipeline.inference import LightGbmRankingService, LightGbmFeatureVectorizer; from ai_pipeline.shared import LightGbmFeatureSchema, LightGbmModelScorer; print('✓ All checks passed')"

## Docker build
docker:
	docker build -t social-pulse-ai ./ai_pipeline

## Remove generated artifacts
clean:
	rm -f ai_pipeline/model/model.json ai_pipeline/model/metrics.json
