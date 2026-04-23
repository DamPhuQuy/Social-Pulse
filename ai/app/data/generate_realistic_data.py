"""
Generate realistic training data with proper ground truth labels
NO TARGET LEAKAGE - labels come from real user behavior simulation
"""
import numpy as np
import pandas as pd
from datetime import datetime, timedelta
from typing import List, Tuple
import random


class RealisticDataGenerator:
    """
    Generate realistic social media data with:
    - Power-law distributions
    - User preferences and behavior patterns
    - Temporal dynamics
    - Real ground truth from simulated user behavior
    """

    def __init__(self, n_users=1000, n_posts=10000, n_interactions=50000, seed=42):
        self.n_users = n_users
        self.n_posts = n_posts
        self.n_interactions = n_interactions
        self.seed = seed
        np.random.seed(seed)
        random.seed(seed)

        # Topic categories
        self.topics = ['technology', 'sports', 'entertainment', 'politics', 'science',
                       'food', 'travel', 'fashion', 'gaming', 'music']

    def generate_users(self) -> pd.DataFrame:
        """Generate realistic user profiles with preferences"""

        # Power-law distribution for followers (most users have few followers)
        follower_counts = np.random.zipf(a=2.0, size=self.n_users).clip(1, 100000)
        following_counts = np.random.zipf(a=2.5, size=self.n_users).clip(1, 5000)

        # Account age: exponential (most accounts are relatively new)
        account_age_days = np.random.exponential(scale=365, size=self.n_users).clip(1, 3650).astype(int)

        # Post count: correlated with account age
        post_counts = (account_age_days * np.random.uniform(0.01, 0.5, self.n_users)).astype(int)

        # Engagement rate: beta distribution (bounded 0-1)
        engagement_rates = np.random.beta(a=2, b=8, size=self.n_users)  # Most users low engagement

        # Session duration: log-normal
        avg_session_duration = np.random.lognormal(mean=2, sigma=1, size=self.n_users).clip(1, 120)

        # Each user has topic preferences (personalization!)
        user_topics = []
        for _ in range(self.n_users):
            n_interests = np.random.choice([1, 2, 3, 4], p=[0.3, 0.4, 0.2, 0.1])
            interests = random.sample(self.topics, n_interests)
            user_topics.append(interests)

        # Active hours (when user is online)
        user_active_hours = []
        for _ in range(self.n_users):
            # Most users active during certain hours
            peak_hour = np.random.choice([8, 12, 18, 20])
            hours = list(range(max(0, peak_hour-2), min(24, peak_hour+3)))
            user_active_hours.append(hours)

        users_df = pd.DataFrame({
            'user_id': range(self.n_users),
            'follower_count': follower_counts,
            'following_count': following_counts,
            'post_count': post_counts,
            'account_age_days': account_age_days,
            'preferred_topics': user_topics,
            'engagement_rate': engagement_rates,
            'avg_session_duration_minutes': avg_session_duration,
            'active_hours': user_active_hours
        })

        return users_df

    def generate_posts(self, users_df: pd.DataFrame) -> pd.DataFrame:
        """Generate realistic posts with early signals (NO future data)"""

        # Authors: power-law (few users create most content)
        author_ids = np.random.choice(
            users_df['user_id'].values,
            size=self.n_posts,
            p=self._power_law_weights(self.n_users)
        )

        # Topics: some topics more popular
        topic_weights = [0.15, 0.12, 0.15, 0.08, 0.10, 0.08, 0.07, 0.08, 0.10, 0.07]
        topics = np.random.choice(self.topics, size=self.n_posts, p=topic_weights)

        # Created time: exponential (most posts are recent)
        now = datetime.now()
        hours_ago = np.random.exponential(scale=24, size=self.n_posts).clip(0, 168)  # Last 7 days
        created_at = [now - timedelta(hours=float(h)) for h in hours_ago]

        # Content features
        content_length = np.random.lognormal(mean=5, sigma=1, size=self.n_posts).astype(int).clip(10, 5000)
        has_image = np.random.choice([True, False], size=self.n_posts, p=[0.6, 0.4])
        has_video = np.random.choice([True, False], size=self.n_posts, p=[0.2, 0.8])

        # Author features (available at ranking time)
        author_follower_counts = users_df.set_index('user_id').loc[author_ids, 'follower_count'].values
        author_engagement_rates = users_df.set_index('user_id').loc[author_ids, 'engagement_rate'].values

        # Predicted quality score (pre-computed, NOT from future engagement)
        # Based on author reputation + content features
        predicted_quality = (
            0.3 * np.log1p(author_follower_counts) / 10 +
            0.2 * author_engagement_rates +
            0.2 * (content_length / 1000).clip(0, 1) +
            0.15 * has_image.astype(float) +
            0.15 * has_video.astype(float) +
            np.random.normal(0, 0.1, self.n_posts)
        ).clip(0, 1)

        posts_df = pd.DataFrame({
            'post_id': range(self.n_posts),
            'author_id': author_ids,
            'topic': topics,
            'created_at': created_at,
            'content_length': content_length,
            'has_image': has_image,
            'has_video': has_video,
            'author_follower_count': author_follower_counts,
            'author_avg_engagement_rate': author_engagement_rates,
            'predicted_quality_score': predicted_quality
        })

        return posts_df

    def generate_relationships(self, users_df: pd.DataFrame) -> pd.DataFrame:
        """Generate user-author relationships (follows, past interactions)"""

        relationships = []

        for user_id in range(self.n_users):
            # Each user follows some authors
            n_following = int(users_df.loc[user_id, 'following_count'])
            n_following = min(n_following, self.n_users - 1)

            # More likely to follow popular users
            followed_authors = np.random.choice(
                [uid for uid in range(self.n_users) if uid != user_id],
                size=min(n_following, 100),  # Cap at 100 for performance
                replace=False,
                p=self._power_law_weights(self.n_users - 1)
            )

            for author_id in followed_authors:
                # Past interaction counts (Poisson)
                interaction_7d = np.random.poisson(lam=2)
                interaction_30d = interaction_7d + np.random.poisson(lam=5)

                # Hours since last interaction (exponential)
                hours_since = np.random.exponential(scale=48).clip(0, 168)

                # Affinity score (higher for followed users)
                affinity = np.random.beta(a=5, b=2)  # Skewed towards high affinity

                relationships.append({
                    'user_id': user_id,
                    'author_id': author_id,
                    'follows': True,
                    'interaction_count_7d': interaction_7d,
                    'interaction_count_30d': interaction_30d,
                    'hours_since_last_interaction': hours_since,
                    'affinity_score': affinity
                })

        return pd.DataFrame(relationships)

    def simulate_user_behavior(
        self,
        users_df: pd.DataFrame,
        posts_df: pd.DataFrame,
        relationships_df: pd.DataFrame
    ) -> pd.DataFrame:
        """
        Simulate real user behavior to generate ground truth labels
        NO CHEATING - engagement is based on realistic user preferences
        """

        interactions = []

        # For each user, simulate feed viewing sessions
        for user_id in range(min(self.n_users, 500)):  # Sample users for performance
            user = users_df.loc[user_id]

            # User preferences
            preferred_topics = user['preferred_topics']
            user_engagement_rate = user['engagement_rate']

            # Get user's relationships
            user_relationships = relationships_df[relationships_df['user_id'] == user_id]
            followed_authors = set(user_relationships['author_id'].values)

            # Sample candidate posts (simulate candidate generation)
            n_candidates = np.random.randint(50, 200)
            candidate_posts = posts_df.sample(n=min(n_candidates, len(posts_df)))

            for idx, post in candidate_posts.iterrows():
                post_id = post['post_id']
                author_id = post['author_id']
                topic = post['topic']
                hours_since_post = (datetime.now() - post['created_at']).total_seconds() / 3600

                # Calculate engagement probability based on REAL factors
                engagement_prob = 0.01  # Base rate

                # 1. Topic affinity (PERSONALIZATION)
                if topic in preferred_topics:
                    engagement_prob *= 5.0

                # 2. Relationship (CRITICAL)
                if author_id in followed_authors:
                    engagement_prob *= 10.0
                    # Get affinity score
                    rel = user_relationships[user_relationships['author_id'] == author_id]
                    if not rel.empty:
                        affinity = rel.iloc[0]['affinity_score']
                        engagement_prob *= (1 + affinity)

                # 3. Freshness (TEMPORAL)
                recency_factor = np.exp(-0.05 * hours_since_post)
                engagement_prob *= recency_factor

                # 4. Content quality
                engagement_prob *= (1 + post['predicted_quality_score'])

                # 5. User's general engagement rate
                engagement_prob *= (1 + user_engagement_rate)

                # 6. Content type preference
                if post['has_video']:
                    engagement_prob *= 1.5
                if post['has_image']:
                    engagement_prob *= 1.2

                # Cap probability
                engagement_prob = min(engagement_prob, 0.95)

                # Simulate engagement
                engaged = np.random.random() < engagement_prob

                if engaged:
                    # Determine engagement type (deeper engagement is rarer)
                    engagement_type = np.random.choice(
                        [1, 2, 3, 4, 5],  # view, click, upvote, comment, share
                        p=[0.4, 0.3, 0.2, 0.08, 0.02]
                    )
                    dwell_time = np.random.lognormal(mean=2, sigma=1).clip(1, 300)
                else:
                    engagement_type = 0  # skip
                    dwell_time = np.random.exponential(scale=0.5).clip(0, 3)

                interactions.append({
                    'user_id': user_id,
                    'post_id': post_id,
                    'author_id': author_id,
                    'topic': topic,
                    'timestamp': datetime.now(),
                    'engaged': engaged,
                    'engagement_type': engagement_type,
                    'dwell_time_seconds': dwell_time,
                    'position': len(interactions) % 20  # Simulated rank
                })

                # Stop if enough interactions
                if len(interactions) >= self.n_interactions:
                    break

            if len(interactions) >= self.n_interactions:
                break

        return pd.DataFrame(interactions)

    def _power_law_weights(self, n: int) -> np.ndarray:
        """Generate power-law probability weights"""
        ranks = np.arange(1, n + 1)
        weights = 1.0 / ranks
        return weights / weights.sum()

    def generate_full_dataset(self) -> Tuple[pd.DataFrame, pd.DataFrame, pd.DataFrame, pd.DataFrame]:
        """Generate complete realistic dataset"""
        print("Generating users...")
        users_df = self.generate_users()

        print("Generating posts...")
        posts_df = self.generate_posts(users_df)

        print("Generating relationships...")
        relationships_df = self.generate_relationships(users_df)

        print("Simulating user behavior (ground truth)...")
        interactions_df = self.simulate_user_behavior(users_df, posts_df, relationships_df)

        print(f"Generated {len(users_df)} users, {len(posts_df)} posts, "
              f"{len(relationships_df)} relationships, {len(interactions_df)} interactions")

        return users_df, posts_df, relationships_df, interactions_df


if __name__ == "__main__":
    generator = RealisticDataGenerator(
        n_users=1000,
        n_posts=10000,
        n_interactions=50000
    )

    users, posts, relationships, interactions = generator.generate_full_dataset()

    # Save to disk
    users.to_parquet('data/users.parquet')
    posts.to_parquet('data/posts.parquet')
    relationships.to_parquet('data/relationships.parquet')
    interactions.to_parquet('data/interactions.parquet')

    print("\n✅ Realistic data generated successfully!")
    print(f"Engagement rate: {interactions['engaged'].mean():.2%}")
    print(f"Engagement type distribution:\n{interactions['engagement_type'].value_counts(normalize=True)}")
