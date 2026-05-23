from __future__ import annotations

import logging

from fastapi import FastAPI

from .config import RankingServiceConfig
from .controller import RankingController

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

_APP_TITLE = "AI Pipeline Ranking Service"
_APP_DESCRIPTION = "LightGBM-based personalised feed ranking over HTTP."
_APP_VERSION = "2.0.0"


def create_app() -> FastAPI:
    app = FastAPI(
        title=_APP_TITLE,
        description=_APP_DESCRIPTION,
        version=_APP_VERSION,
    )

    ranking_service = RankingServiceConfig().build()
    ranking_controller = RankingController(ranking_service)

    app.include_router(ranking_controller.router)
    _register_infrastructure_routes(app)

    logger.info("ApplicationFactory: application wired and ready.")
    return app


def _register_infrastructure_routes(app: FastAPI) -> None:
    @app.get("/health", tags=["ops"], summary="Liveness probe")
    def health() -> dict[str, str]:
        return {"status": "ok"}
