PYTHON = ai_pipeline/.venv/bin/python

train:
	PYTHONPATH=$(CURDIR) $(PYTHON) -m ai_pipeline.training.main
