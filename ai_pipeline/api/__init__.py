"""
ai_pipeline.api
~~~~~~~~~~~~~~~
REST API layer for the ranking service.

Package structure (Java-style):
  config.py      – RankingServiceConfig  (@Configuration)
  dto.py         – Request / Response DTOs  (@RequestBody / @ResponseBody)
  controller.py  – RankingController  (@RestController)
  application.py – ApplicationFactory  (creates the FastAPI app)
"""
from .application import create_app

__all__ = ["create_app"]
