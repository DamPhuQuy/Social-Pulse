"""
Feature engineering for feed ranking
Extract features WITHOUT target leakage
"""
import pandas as pd
import numpy as np
from datetime import datetime
from typing import Dict, List


class FeatureEngineer:
    """
    Extract features for ranking model
    CRITICAL: Only use features available at ranking time
    NO future data (post-engagement metrics)
    """

    def __init__(self):
        self.feature_columns = []

    def extract_features(
        self,
        interactions_df: pd.DataFrame,
        users_df: pd.DataFrame,
        posts_df: pd.DataFrame,
        relationships_df: pd.DataFrame
    ) -> pd.DataFrame:
        """
        Extract all features for training
        Returns: DataFrame with features + labels
        """

        # Merge all data
        df = interactions_df.copy()

        # Add post features
        df = df.merge(
            posts_df[['post_id', 'author_id', 'topic', 'created_at', 'content_length',
                      'has_image', 'has_video', 'author_follower_count',
                      'author_avg_engagement_rate', 'predicted_quality_score']],
            on='post_id',
            how='left',
            suffixes=('', '_post')
        )

        # Add user features
        df = df.merge(
            users_df[['user_id', 'follower_count', 'following_count', 'post_count',
                      'account_age_days', 'engagement_rate', 'avg_session_duration_minutes']],
            on='user_id',
            how='left',
            suffixes=('', '_user')
        )

        # Add relationship features
        df = df.merge(
            relationships_df[['user_id', 'author_id', 'follows', 'interaction_count_7d',
                             'interaction_count_30d', 'hours_since_last_interaction', 'affinity_score']],
            on=['user_id', 'author_id'],
            how='left'
        )

        # Fill missing relationships (user doesn't follow author)
        df['follows'] = df['follows'].fillna(False)
        df['interaction_count_7d'] = df['interaction_count_7d'].fillna(0)
        df['interaction_count_30d'] = df['interaction_count_30d'].fillna(0)
        df['hours_since_last_interaction'] = df['hours_since_last_interaction'].fillna(999)
        df['affinity_score'] = df['affinity_score'].fillna(0)

        # Extract temporal features
        df = self._add_temporal_features(df)

        # Extract interaction features
        df = self._add_interaction_features(df, users_df)

        # Normalize features
        df = self._normalize_features(df)

        # Select final feature columns
        self.feature_columns = self._get_feature_columns()

        return df

    def _add_temporal_features(self, df: pd.DataFrame) -> pd.DataFrame:
        """Add temporal features"""

        # Hours since post
        now = datetime.now()
        df['hours_since_post'] = df['created_at'].apply(
            lambda x: (now - x).total_seconds() / 3600
        )

        # Recency score (exponential decay)
        df['recency_score'] = np.exp(-0.05 * df['hours_since_post'])

        # Time of day features
        df['post_hour'] = df['created_at'].dt.hour
        df['is_peak_hour'] = df['post_hour'].isin([8, 12, 18, 19, 20]).astype(int)
        df['is_weekend'] = df['created_at'].dt.dayofweek.isin([5, 6]).astype(int)

        return df

    def _add_interaction_features(self, df: pd.DataFrame, users_df: pd.DataFrame) -> pd.DataFrame:
        """Add user-post interaction features"""

        # Topic affinity (does user prefer this topic?)
        user_topics_dict = users_df.set_index('user_id')['preferred_topics'].to_dict()

        df['topic_affinity'] = df.apply(
            lambda row: 1.0 if row['topic'] in user_topics_dict.get(row['user_id'], []) else 0.0,
            axis=1
        )

        # Content length match (log scale)
        df['content_length_log'] = np.log1p(df['content_length'])

        # Author popularity (log scale)
        df['author_follower_count_log'] = np.log1p(df['author_follower_count'])

        # Interaction velocity (if user interacts with author frequently)
        df['interaction_velocity'] = df['interaction_count_7d'] / 7.0

        # Recency of last interaction
        df['has_recent_interaction'] = (df['hours_since_last_interaction'] < 48).astype(int)

        return df

    def _normalize_features(self, df: pd.DataFrame) -> pd.DataFrame:
        """Normalize numerical features"""

        # Log transform heavy-tailed features
        log_features = ['follower_count', 'following_count', 'post_count', 'account_age_days']
        for feat in log_features:
            if feat in df.columns:
                df[f'{feat}_log'] = np.log1p(df[feat])

        return df

    def _get_feature_columns(self) -> List[str]:
        """Define final feature columns for model"""

        features = [
            # Post features (NO post-engagement metrics!)
            'content_length_log',
            'has_image',
            'has_video',
            'predicted_quality_score',

            # Author features
            'author_follower_count_log',
            'author_avg_engagement_rate',

            # Viewer features
            'follower_count_log',
            'following_count_log',
            'post_count_log',
            'account_age_days_log',
            'engagement_rate',
            'avg_session_duration_minutes',

            # 🔥 Relationship features (MOST IMPORTANT)
            'follows',
            'interaction_count_7d',
            'interaction_count_30d',
            'hours_since_last_interaction',
            'affinity_score',
            'interaction_velocity',
            'has_recent_interaction',

            # Temporal features
            'hours_since_post',
            'recency_score',
            'is_peak_hour',
            'is_weekend',

            # Interaction features
            'topic_affinity',
        ]

        return features

    def prepare_ranking_data(self, df: pd.DataFrame) -> tuple:
        """
        Prepare data for LightGBM Ranker
        Returns: X, y, groups
        """

        # Features
        X = df[self.feature_columns].values

        # Labels (engagement_type: 0-5)
        y = df['engagement_type'].values

        # Groups (number of posts per user)
        groups = df.groupby('user_id').size().values

        return X, y, groups


if __name__ == "__main__":
    # Load data
    users = pd.read_parquet('data/users.parquet')
    posts = pd.read_parquet('data/posts.parquet')
    relationships = pd.read_parquet('data/relationships.parquet')
    interactions = pd.read_parquet('data/interactions.parquet')

    # Extract features
    engineer = FeatureEngineer()
    df = engineer.extract_features(interactions, users, posts, relationships)

    print(f"\n✅ Features extracted: {len(engineer.feature_columns)} features")
    print(f"Feature columns: {engineer.feature_columns}")

    # Prepare for ranking
    X, y, groups = engineer.prepare_ranking_data(df)
    print(f"\nX shape: {X.shape}")
    print(f"y shape: {y.shape}")
    print(f"groups shape: {groups.shape}")
    print(f"Label distribution:\n{pd.Series(y).value_counts(normalize=True)}")
