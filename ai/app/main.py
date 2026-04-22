"""
Production FastAPI service with two-stage ranking
"""
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from typing import List, Optional
import lightgbm as lgb
import pandas as pd
import numpy as np
from pathlib import Path
import json
from datetime import datetime

# Import custom modules
import sys
sys.path.append(str(Path(__file__).parent))

from data.feature_engineering import FeatureEngineer
from models.ranker import TwoStageRanker, CandidateGenerator

app = FastAPI(
    title="Social Pulse AI Ranking Service",
    version="2.0.0",
    description="Production-grade feed ranking with LightGBM Ranker"
)

# Global state
model = None
feature_engineer = None
ranker = None
model_metadata = None


# Request/Response models
class PostFeatures(BaseModel):
    post_id: int
    author_id: int
    topic: str
    created_at: str
    content_length: int
    has_image: bool
    has_video: bool
    author_follower_count: int
    author_avg_engagement_rate: float
    predicted_quality_score: float


class UserFeatures(BaseModel):
    user_id: int
    follower_count: int
    following_count: int
    post_count: int
    account_age_days: int
    engagement_rate: float
    avg_session_duration_minutes: float


class RelationshipFeatures(BaseModel):
    user_id: int
    author_id: int
    follows: bool = False
    interaction_count_7d: int = 0
    interaction_count_30d: int = 0
    hours_since_last_interaction: float = 999.0
    affinity_score: float = 0.0


class RankingRequest(BaseModel):
    user_id: int
    user_features: UserFeatures
    candidate_posts: List[PostFeatures]
    relationships: List[RelationshipFeatures]


class RankedPost(BaseModel):
    post_id: int
    ranking_score: float
    rank: int


class RankingResponse(BaseModel):
    user_id: int
    ranked_posts: List[RankedPost]
    total_candidates: int
    model_version: str
    timestamp: str


def load_model():
    """Load model on startup"""
    global model, feature_engineer, ranker, model_metadata

    model_path = Path(__file__).parent / "models" / "ranking_model.txt"
    metadata_path = Path(__file__).parent / "models" / "model_metadata.json"

    if not model_path.exists():
        print("⚠️  Model not found. Please train the model first.")
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
            print(f"✅ Model metadata loaded: version {model_metadata.get('version', 'unknown')}")

        # Initialize feature engineer
        feature_engineer = FeatureEngineer()
        feature_engineer.feature_columns = model_metadata.get('feature_names', [])

        # Initialize ranker
        ranker = TwoStageRanker(model, feature_engineer)

        print("✅ Ranking service ready!")

    except Exception as e:
        print(f"❌ Error loading model: {e}")
        raise


@app.on_event("startup")
def startup_event():
    load_model()


@app.get("/")
def read_root():
    return {
        "service": "Social Pulse AI Ranking Service",
        "version": "2.0.0",
        "status": "running",
        "model_loaded": model is not None,
        "model_type": "LightGBM Ranker (LambdaMART)"
    }


@app.get("/health")
def health_check():
    return {
        "status": "healthy" if model is not None else "model_not_loaded",
        "model_loaded": model is not None,
        "timestamp": datetime.now().isoformat()
    }


@app.get("/model/info")
def model_info():
    """Get model metadata"""
    if model_metadata is None:
        raise HTTPException(status_code=503, detail="Model metadata not available")

    return {
        "model_type": model_metadata.get('model_type'),
        "objective": model_metadata.get('objective'),
        "n_features": model_metadata.get('n_features'),
        "feature_names": model_metadata.get('feature_names'),
        "training_metrics": model_metadata.get('training_history', {}).get('val_metrics'),
        "trained_at": model_metadata.get('trained_at'),
        "version": model_metadata.get('version')
    }


@app.get("/model/feature-importance")
def feature_importance():
    """Get feature importance"""
    if model_metadata is None:
        raise HTTPException(status_code=503, detail="Model metadata not available")

    importance = model_metadata.get('training_history', {}).get('feature_importance', {})

    # Return top 20 features
    top_features = list(importance.items())[:20]

    return {
        "top_features": [
            {"feature": feat, "importance": imp}
            for feat, imp in top_features
        ]
    }


@app.post("/api/v1/rank/predict", response_model=RankingResponse)
def predict_ranking(request: RankingRequest):
    """
    Rank posts for a user
    Uses two-stage pipeline: candidate generation + ML ranking
    """
    if model is None:
        raise HTTPException(status_code=503, detail="Model not loaded")

    if not request.candidate_posts:
        return RankingResponse(
            user_id=request.user_id,
            ranked_posts=[],
            total_candidates=0,
            model_version=model_metadata.get('version', 'unknown'),
            timestamp=datetime.now().isoformat()
        )

    try:
        # Convert request to DataFrames
        posts_data = []
        for post in request.candidate_posts:
            posts_data.append({
                'post_id': post.post_id,
                'author_id': post.author_id,
                'topic': post.topic,
                'created_at': pd.to_datetime(post.created_at),
                'content_length': post.content_length,
                'has_image': post.has_image,
                'has_video': post.has_video,
                'author_follower_count': post.author_follower_count,
                'author_avg_engagement_rate': post.author_avg_engagement_rate,
                'predicted_quality_score': post.predicted_quality_score
            })
        posts_df = pd.DataFrame(posts_data)

        # User features
        user_data = {
            'user_id': [request.user_id],
            'follower_count': [request.user_features.follower_count],
            'following_count': [request.user_features.following_count],
            'post_count': [request.user_features.post_count],
            'account_age_days': [request.user_features.account_age_days],
            'engagement_rate': [request.user_features.engagement_rate],
            'avg_session_duration_minutes': [request.user_features.avg_session_duration_minutes],
            'preferred_topics': [[]],  # Placeholder
            'active_hours': [[]]  # Placeholder
        }
        users_df = pd.DataFrame(user_data)

        # Relationship features
        relationships_data = []
        for rel in request.relationships:
            relationships_data.append({
                'user_id': rel.user_id,
                'author_id': rel.author_id,
                'follows': rel.follows,
                'interaction_count_7d': rel.interaction_count_7d,
                'interaction_count_30d': rel.interaction_count_30d,
                'hours_since_last_interaction': rel.hours_since_last_interaction,
                'affinity_score': rel.affinity_score
            })
        relationships_df = pd.DataFrame(relationships_data) if relationships_data else pd.DataFrame()

        # Create mock interactions for feature extraction
        mock_interactions = pd.DataFrame({
            'user_id': [request.user_id] * len(posts_df),
            'post_id': posts_df['post_id'].values,
            'author_id': posts_df['author_id'].values,
            'topic': posts_df['topic'].values,
            'timestamp': [datetime.now()] * len(posts_df),
            'engaged': [False] * len(posts_df),
            'engagement_type': [0] * len(posts_df),
            'dwell_time_seconds': [0] * len(posts_df),
            'position': list(range(len(posts_df)))
        })

        # Extract features
        features_df = feature_engineer.extract_features(
            mock_interactions,
            users_df,
            posts_df,
            relationships_df
        )

        # Predict scores
        X = features_df[feature_engineer.feature_columns].values
        scores = model.predict(X)

        # Rank posts
        ranked_indices = np.argsort(scores)[::-1]
        ranked_posts = []

        for rank, idx in enumerate(ranked_indices, 1):
            ranked_posts.append(RankedPost(
                post_id=int(posts_df.iloc[idx]['post_id']),
                ranking_score=float(scores[idx]),
                rank=rank
            ))

        return RankingResponse(
            user_id=request.user_id,
            ranked_posts=ranked_posts,
            total_candidates=len(request.candidate_posts),
            model_version=model_metadata.get('version', 'unknown'),
            timestamp=datetime.now().isoformat()
        )

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Ranking failed: {str(e)}")


@app.post("/api/v1/rank/batch")
def batch_ranking(requests: List[RankingRequest]):
    """Batch ranking for multiple users"""
    results = []
    for req in requests:
        try:
            result = predict_ranking(req)
            results.append(result)
        except Exception as e:
            results.append({
                "user_id": req.user_id,
                "error": str(e)
            })
    return results


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001)
