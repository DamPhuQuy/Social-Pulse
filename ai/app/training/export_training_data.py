"""
Export training data from PostgreSQL database to CSV for model training.

This script connects to the Social Pulse database and exports training data
collected by TrainingDataCollectionService.
"""

import os
import psycopg2
import pandas as pd
from datetime import datetime, timedelta
from pathlib import Path


def get_db_connection():
    """Create database connection from environment variables"""
    return psycopg2.connect(
        host=os.getenv('DB_HOST', 'localhost'),
        port=os.getenv('DB_PORT', '5432'),
        database=os.getenv('DB_NAME', 'social_pulse'),
        user=os.getenv('DB_USER', 'postgres'),
        password=os.getenv('DB_PASSWORD', 'postgres')
    )


def export_training_data(
    output_path: str,
    start_date: datetime = None,
    end_date: datetime = None,
    min_samples_per_user: int = 10
):
    """
    Export training data from database to CSV.

    Args:
        output_path: Path to save CSV file
        start_date: Start date for data export (default: 30 days ago)
        end_date: End date for data export (default: now)
        min_samples_per_user: Minimum samples per user to include
    """

    if start_date is None:
        start_date = datetime.now() - timedelta(days=30)
    if end_date is None:
        end_date = datetime.now()

    print(f"Exporting training data from {start_date} to {end_date}")

    conn = get_db_connection()

    # Query to extract all training data with feature JSON expanded
    query = """
    SELECT
        id,
        user_id,
        post_id,
        author_id,

        -- Extract features from JSONB
        (features->'contentFeatures'->>'keywordsRelevance')::DOUBLE PRECISION as keywords_relevance,
        (features->'contentFeatures'->>'hashtagsRelevance')::DOUBLE PRECISION as hashtags_relevance,
        (features->'contentFeatures'->>'mentionsRelevance')::DOUBLE PRECISION as mentions_relevance,
        (features->'contentFeatures'->>'contentLength')::INTEGER as content_length,
        (features->'contentFeatures'->>'hasHashtags')::INTEGER as has_hashtags,
        (features->'contentFeatures'->>'hasUrl')::INTEGER as has_url,
        (features->'contentFeatures'->>'hasMultimedia')::INTEGER as has_multimedia,

        (features->'authorFeatures'->>'followerCount')::INTEGER as author_follower_count,
        (features->'authorFeatures'->>'followingCount')::INTEGER as author_following_count,
        (features->'authorFeatures'->>'followersFollowingsRatio')::DOUBLE PRECISION as followers_followings_ratio,
        (features->'authorFeatures'->>'seniority')::DOUBLE PRECISION as author_seniority,
        (features->'authorFeatures'->>'postCount')::INTEGER as author_post_count,
        (features->'authorFeatures'->>'engagementRate')::DOUBLE PRECISION as author_engagement_rate,

        (features->'relationshipFeatures'->>'follows')::INTEGER as follows,
        (features->'relationshipFeatures'->>'interactionCount7d')::INTEGER as interaction_count_7d,
        (features->'relationshipFeatures'->>'interactionCount30d')::INTEGER as interaction_count_30d,
        (features->'relationshipFeatures'->>'hoursSinceLastInteraction')::DOUBLE PRECISION as hours_since_last_interaction,
        (features->'relationshipFeatures'->>'affinityScore')::DOUBLE PRECISION as affinity_score,

        (features->'engagementFeatures'->>'popularity')::BIGINT as popularity,
        (features->'engagementFeatures'->>'upvoteCount')::BIGINT as upvote_count,
        (features->'engagementFeatures'->>'downvoteCount')::BIGINT as downvote_count,
        (features->'engagementFeatures'->>'commentCount')::BIGINT as comment_count,
        (features->'engagementFeatures'->>'shareCount')::BIGINT as share_count,
        (features->'engagementFeatures'->>'viewCount')::BIGINT as view_count,

        -- Target variable
        relevance,

        -- Metadata
        impression_time,
        interaction_time,
        interaction_type,
        position_in_feed,
        created_at

    FROM training_data
    WHERE impression_time BETWEEN %s AND %s
    ORDER BY impression_time ASC
    """

    print("Querying database...")
    df = pd.read_sql_query(query, conn, params=(start_date, end_date))
    conn.close()

    print(f"Retrieved {len(df)} training samples")

    # Filter users with minimum samples
    user_counts = df['user_id'].value_counts()
    valid_users = user_counts[user_counts >= min_samples_per_user].index
    df_filtered = df[df['user_id'].isin(valid_users)]

    print(f"After filtering (min {min_samples_per_user} samples/user): {len(df_filtered)} samples from {len(valid_users)} users")

    # Calculate statistics
    positive_samples = df_filtered[df_filtered['relevance'] == 1].shape[0]
    negative_samples = df_filtered[df_filtered['relevance'] == 0].shape[0]
    positive_rate = positive_samples / len(df_filtered) if len(df_filtered) > 0 else 0

    print(f"\nDataset Statistics:")
    print(f"  Total samples: {len(df_filtered)}")
    print(f"  Positive samples: {positive_samples} ({positive_rate:.2%})")
    print(f"  Negative samples: {negative_samples} ({1-positive_rate:.2%})")
    print(f"  Unique users: {df_filtered['user_id'].nunique()}")
    print(f"  Unique posts: {df_filtered['post_id'].nunique()}")
    print(f"  Unique authors: {df_filtered['author_id'].nunique()}")

    # Check for missing values
    missing_counts = df_filtered.isnull().sum()
    if missing_counts.sum() > 0:
        print(f"\nWarning: Missing values detected:")
        print(missing_counts[missing_counts > 0])

    # Save to CSV
    output_path = Path(output_path)
    output_path.parent.mkdir(parents=True, exist_ok=True)

    df_filtered.to_csv(output_path, index=False)
    print(f"\nTraining data exported to: {output_path}")

    return df_filtered


def get_training_data_stats():
    """Get statistics about available training data"""
    conn = get_db_connection()
    cursor = conn.cursor()

    stats_query = """
    SELECT
        COUNT(*) as total_samples,
        COUNT(DISTINCT user_id) as unique_users,
        COUNT(DISTINCT post_id) as unique_posts,
        COUNT(DISTINCT author_id) as unique_authors,
        SUM(CASE WHEN relevance = 1 THEN 1 ELSE 0 END) as positive_samples,
        SUM(CASE WHEN relevance = 0 THEN 1 ELSE 0 END) as negative_samples,
        MIN(impression_time) as earliest_impression,
        MAX(impression_time) as latest_impression
    FROM training_data
    """

    cursor.execute(stats_query)
    result = cursor.fetchone()

    conn.close()

    if result:
        total, users, posts, authors, positive, negative, earliest, latest = result
        positive_rate = positive / total if total > 0 else 0

        print("Training Data Statistics:")
        print(f"  Total samples: {total}")
        print(f"  Unique users: {users}")
        print(f"  Unique posts: {posts}")
        print(f"  Unique authors: {authors}")
        print(f"  Positive samples: {positive} ({positive_rate:.2%})")
        print(f"  Negative samples: {negative} ({1-positive_rate:.2%})")
        print(f"  Date range: {earliest} to {latest}")

        return {
            'total_samples': total,
            'unique_users': users,
            'positive_rate': positive_rate
        }

    return None


if __name__ == '__main__':
    # Check available data
    print("Checking available training data...\n")
    stats = get_training_data_stats()

    if stats and stats['total_samples'] >= 1000:
        print(f"\n✓ Sufficient data available ({stats['total_samples']} samples)")
        print("Exporting training data...\n")

        # Export data
        export_training_data(
            output_path='../data/training_data.csv',
            min_samples_per_user=10
        )
    else:
        print(f"\n✗ Insufficient training data")
        print(f"  Current: {stats['total_samples'] if stats else 0} samples")
        print(f"  Required: 1000+ samples")
        print("\nPlease collect more training data before training models.")
