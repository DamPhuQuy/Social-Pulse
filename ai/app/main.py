"""
FastAPI prediction service for Social Pulse feed ranking.

This service loads the trained LightGBM model and provides
a REST API for ranking posts based on extracted features.
"""

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Dict, Any
import lightgbm as lgb
import numpy as np
from pathlib import Path
import json
from datetime import datetime
import os
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

app = FastAPI(
    title="Social Pulse AI Ranking Service",
    version="1.0.0",
    description="AI-powered feed ranking using LightGBM"
)

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Global state
model = None
model_metadata = None
feature_names = None


class FeatureVector(BaseModel):
    """Feature vector for a single post"""
    # Post features
    hot_score: float = 0.0
    upvote_ratio: float = 0.0
    has_image: int = 0
    content_length: int = 0
    post_age_hours: float = 0.0
    upvote_count: int = 0
    downvote_count: int = 0
    cmt_count: int = 0
    share_count: int = 0
    view_count: int = 0

    # Interaction features
    interaction_count_7d: int = 0
    interaction_count_30d: int = 0
    affinity_score: float = 0.0
    last_interaction_hours: float = 999.0



class RankingRequest(BaseModel):
    """Request to rank posts for a user"""
    user_id: int
    posts: List[Dict[str, Any]]  # List of {post_id, features}


class RankedPost(BaseModel):
    """Ranked post with score"""
    post_id: int
    score: float
    rank: int


class RankingResponse(BaseModel):
    """Response with ranked posts"""
    user_id: int
    ranked_posts: List[RankedPost]
    total_posts: int
    model_version: str
    timestamp: str


def load_model():
    """Load trained model on startup"""
    global model, model_metadata, feature_names

    model_path = Path(os.getenv('MODEL_PATH', 'models/ranking_model.txt'))
    metadata_path = Path(os.getenv('MODEL_METADATA_PATH', 'models/model_metadata.json'))

    if not model_path.exists():
        print("⚠️  Model not found. Please train the model first.")
        print(f"   Expected path: {model_path.absolute()}")
        print("   Run: python app/training/train_ranker.py")
        return

    try:
        # Load LightGBM model
        model = lgb.Booster(model_file=str(model_path))
        print(f"✅ Model loaded from {model_path}")

        # Load metadata
        if metadata_path.exists():
            with open(metadata_path, 'r') as f:
                model_metadata = json.load(f)
            feature_names = model_metadata.get('feature_names', [])
            print(f"✅ Model metadata loaded")
            print(f"   Version: {model_metadata.get('version', 'unknown')}")
            print(f"   Features: {len(feature_names)}")
            print(f"   Trained: {model_metadata.get('trained_at', 'unknown')}")

        print("✅ Ranking service ready!")

    except Exception as e:
        print(f"❌ Error loading model: {e}")
        raise


@app.on_event("startup")
def startup_event():
    """Initialize service on startup"""
    print("=" * 60)
    print("Social Pulse AI Ranking Service")
    print("=" * 60)
    load_model()


@app.get("/")
def read_root():
    """Root endpoint"""
    return {
        "service": "Social Pulse AI Ranking Service",
        "version": "1.0.0",
        "status": "running",
        "model_loaded": model is not None,
        "model_type": "LightGBM Ranker (LambdaMART)"
    }


@app.get("/health")
def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy" if model is not None else "model_not_loaded",
        "model_loaded": model is not None,
        "timestamp": datetime.now().isoformat()
    }


@app.get("/model/info")
def model_info():
    """Get model information"""
    if model_metadata is None:
        raise HTTPException(status_code=503, detail="Model not loaded")

    return {
        "model_type": model_metadata.get('model_type'),
        "objective": model_metadata.get('objective'),
        "n_features": model_metadata.get('n_features'),
        "feature_names": model_metadata.get('feature_names'),
        "validation_metrics": model_metadata.get('training_history', {}).get('val_metrics'),
        "trained_at": model_metadata.get('trained_at'),
        "version": model_metadata.get('version')
    }


@app.get("/model/feature-importance")
def get_feature_importance():
    """Get feature importance from trained model"""
    if model_metadata is None:
        raise HTTPException(status_code=503, detail="Model not loaded")

    importance = model_metadata.get('training_history', {}).get('feature_importance', {})

    # Return top 20 features
    top_features = list(importance.items())[:20]

    return {
        "top_features": [
            {"feature": feat, "importance": imp}
            for feat, imp in top_features
        ],
        "total_features": len(importance)
    }


@app.post("/predict", response_model=RankingResponse)
def predict_ranking(request: RankingRequest):
    """
    Predict ranking scores for posts.

    The Java backend should send:
    - user_id: ID of the user
    - posts: List of {post_id, features} where features is a dict with all 26 feature values

    Returns ranked posts sorted by score (highest first).
    """
    if model is None:
        raise HTTPException(status_code=503, detail="Model not loaded")

    if not request.posts:
        return RankingResponse(
            user_id=request.user_id,
            ranked_posts=[],
            total_posts=0,
            model_version=model_metadata.get('version', 'unknown') if model_metadata else 'unknown',
            timestamp=datetime.now().isoformat()
        )

    try:
        # Extract features from posts
        post_ids = []
        feature_matrix = []

        for post_data in request.posts:
            post_ids.append(post_data['post_id'])
            features = post_data['features']

            # Build feature vector in correct order (matching FeatureSnapshot)
            feature_vector = [
                features.get('hot_score', 0.0),
                features.get('upvote_ratio', 0.0),
                features.get('has_image', 0),
                features.get('content_length', 0),
                features.get('post_age_hours', 0.0),
                features.get('upvote_count', 0),
                features.get('downvote_count', 0),
                features.get('cmt_count', 0),
                features.get('share_count', 0),
                features.get('view_count', 0),
                features.get('interaction_count_7d', 0),
                features.get('interaction_count_30d', 0),
                features.get('affinity_score', 0.0),
                features.get('last_interaction_hours', 999.0)
            ]

            feature_matrix.append(feature_vector)

        # Convert to numpy array
        X = np.array(feature_matrix, dtype=np.float32)

        # Predict scores
        scores = model.predict(X)

        # Rank posts by score (descending)
        ranked_indices = np.argsort(scores)[::-1]

        ranked_posts = []
        for rank, idx in enumerate(ranked_indices, start=1):
            ranked_posts.append(RankedPost(
                post_id=post_ids[idx],
                score=float(scores[idx]),
                rank=rank
            ))

        return RankingResponse(
            user_id=request.user_id,
            ranked_posts=ranked_posts,
            total_posts=len(request.posts),
            model_version=model_metadata.get('version', 'unknown') if model_metadata else 'unknown',
            timestamp=datetime.now().isoformat()
        )

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Prediction failed: {str(e)}")


@app.post("/predict/batch")
def batch_predict(requests: List[RankingRequest]):
    """Batch prediction for multiple users"""
    results = []

    for req in requests:
        try:
            result = predict_ranking(req)
            results.append(result.dict())
        except Exception as e:
            results.append({
                "user_id": req.user_id,
                "error": str(e),
                "timestamp": datetime.now().isoformat()
            })

    return {"results": results, "total": len(requests)}


if __name__ == "__main__":
    import uvicorn

    host = os.getenv('API_HOST', '0.0.0.0')
    port = int(os.getenv('API_PORT', '5000'))

    print(f"\nStarting server on {host}:{port}")
    uvicorn.run(app, host=host, port=port)
