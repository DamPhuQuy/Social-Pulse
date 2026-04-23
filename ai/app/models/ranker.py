"""
Two-stage ranking pipeline: Candidate Generation + ML Ranking
"""
import numpy as np
import pandas as pd
from typing import List, Dict, Tuple
from datetime import datetime, timedelta


class CandidateGenerator:
    """
    Stage 1: Candidate Generation
    Retrieve ~500-1000 posts using simple rules (fast)
    """

    def __init__(self):
        self.strategies = {
            'recent': 0.3,      # 30% recent posts
            'following': 0.25,  # 25% from followed authors
            'popular': 0.25,    # 25% popular posts
            'explore': 0.2      # 20% exploration (random/diverse)
        }

    def generate_candidates(
        self,
        user_id: int,
        posts_df: pd.DataFrame,
        relationships_df: pd.DataFrame,
        n_candidates: int = 500
    ) -> pd.DataFrame:
        """
        Generate candidate posts for ranking
        Fast retrieval using simple rules
        """

        candidates = []
        now = datetime.now()

        # Get user's followed authors
        user_relationships = relationships_df[relationships_df['user_id'] == user_id]
        followed_authors = set(user_relationships['author_id'].values)

        # 1. Recent posts (30%)
        n_recent = int(n_candidates * self.strategies['recent'])
        recent_posts = posts_df[
            posts_df['created_at'] >= now - timedelta(days=3)
        ].nlargest(n_recent, 'created_at')
        candidates.append(recent_posts)

        # 2. Following posts (25%)
        n_following = int(n_candidates * self.strategies['following'])
        if followed_authors:
            following_posts = posts_df[
                posts_df['author_id'].isin(followed_authors)
            ].nlargest(n_following, 'created_at')
            candidates.append(following_posts)

        # 3. Popular posts (25%)
        n_popular = int(n_candidates * self.strategies['popular'])
        popular_posts = posts_df[
            posts_df['created_at'] >= now - timedelta(days=7)
        ].nlargest(n_popular, 'predicted_quality_score')
        candidates.append(popular_posts)

        # 4. Explore (20%) - random for diversity
        n_explore = n_candidates - n_recent - n_following - n_popular
        explore_posts = posts_df.sample(n=min(n_explore, len(posts_df)))
        candidates.append(explore_posts)

        # Combine and deduplicate
        all_candidates = pd.concat(candidates, ignore_index=True)
        all_candidates = all_candidates.drop_duplicates(subset=['post_id'])

        # Limit to n_candidates
        if len(all_candidates) > n_candidates:
            all_candidates = all_candidates.sample(n=n_candidates)

        return all_candidates


class TwoStageRanker:
    """
    Complete two-stage ranking pipeline
    Stage 1: Candidate Generation (fast, rule-based)
    Stage 2: ML Ranking (accurate, model-based)
    """

    def __init__(self, model, feature_engineer, candidate_generator=None):
        self.model = model
        self.feature_engineer = feature_engineer
        self.candidate_generator = candidate_generator or CandidateGenerator()

    def rank_feed(
        self,
        user_id: int,
        users_df: pd.DataFrame,
        posts_df: pd.DataFrame,
        relationships_df: pd.DataFrame,
        top_k: int = 20
    ) -> List[Dict]:
        """
        Full ranking pipeline
        Returns: Top K ranked posts with scores
        """

        # Stage 1: Candidate Generation
        candidates = self.candidate_generator.generate_candidates(
            user_id, posts_df, relationships_df, n_candidates=500
        )

        if len(candidates) == 0:
            return []

        # Create mock interactions for feature extraction
        mock_interactions = pd.DataFrame({
            'user_id': [user_id] * len(candidates),
            'post_id': candidates['post_id'].values,
            'author_id': candidates['author_id'].values,
            'topic': candidates['topic'].values,
            'timestamp': [datetime.now()] * len(candidates),
            'engaged': [False] * len(candidates),  # Placeholder
            'engagement_type': [0] * len(candidates),  # Placeholder
            'dwell_time_seconds': [0] * len(candidates),  # Placeholder
            'position': list(range(len(candidates)))
        })

        # Stage 2: Feature Extraction
        features_df = self.feature_engineer.extract_features(
            mock_interactions,
            users_df,
            posts_df,
            relationships_df
        )

        # Stage 3: ML Ranking
        X = features_df[self.feature_engineer.feature_columns].values
        scores = self.model.predict(X)

        # Add scores to candidates
        candidates['ranking_score'] = scores

        # Sort by score and take top K
        ranked_posts = candidates.nlargest(top_k, 'ranking_score')

        # Format output
        results = []
        for idx, row in ranked_posts.iterrows():
            results.append({
                'post_id': int(row['post_id']),
                'author_id': int(row['author_id']),
                'topic': row['topic'],
                'ranking_score': float(row['ranking_score']),
                'predicted_quality_score': float(row['predicted_quality_score']),
                'created_at': row['created_at'].isoformat(),
                'has_image': bool(row['has_image']),
                'has_video': bool(row['has_video'])
            })

        return results


if __name__ == "__main__":
    import lightgbm as lgb
    from data.feature_engineering import FeatureEngineer

    # Load model
    print("Loading model...")
    model = lgb.Booster(model_file='models/ranking_model.txt')

    # Load data
    print("Loading data...")
    users = pd.read_parquet('data/users.parquet')
    posts = pd.read_parquet('data/posts.parquet')
    relationships = pd.read_parquet('data/relationships.parquet')

    # Initialize components
    feature_engineer = FeatureEngineer()
    ranker = TwoStageRanker(model, feature_engineer)

    # Test ranking for a user
    test_user_id = 0
    print(f"\nRanking feed for user {test_user_id}...")

    ranked_feed = ranker.rank_feed(
        user_id=test_user_id,
        users_df=users,
        posts_df=posts,
        relationships_df=relationships,
        top_k=20
    )

    print(f"\n✅ Top 20 ranked posts:")
    for i, post in enumerate(ranked_feed, 1):
        print(f"{i:2d}. Post {post['post_id']:5d} | Score: {post['ranking_score']:.4f} | "
              f"Topic: {post['topic']:15s} | Author: {post['author_id']:4d}")
