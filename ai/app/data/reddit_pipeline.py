"""
Pipeline for converting real Reddit data into Social Pulse FeatureSnapshot format.
Assumes a CSV input from a Reddit dataset (e.g., from Kaggle/Pushshift).
"""

import pandas as pd
import numpy as np
from datetime import datetime
import argparse
from pathlib import Path

def parse_reddit_csv(input_path: str, output_path: str):
    print(f"Loading Reddit dataset from {input_path}...")
    
    # Handle potential missing files during MVP
    if not Path(input_path).exists():
        print(f"File {input_path} not found. Please download a Reddit CSV dataset and place it here.")
        return

    df = pd.read_csv(input_path)
    print(f"Loaded {len(df)} rows.")

    # We need to map the Reddit columns to our FeatureSnapshot columns:
    # 'hot_score', 'upvote_ratio', 'has_image', 'content_length', 'post_age_hours',
    # 'upvote_count', 'downvote_count', 'cmt_count', 'share_count', 'view_count',
    # 'interaction_count_7d', 'interaction_count_30d', 'affinity_score', 'last_interaction_hours'

    out_df = pd.DataFrame()

    # Map scores and counts
    out_df['upvote_count'] = df.get('ups', df.get('score', 0)).fillna(0).astype(int)
    out_df['downvote_count'] = df.get('downs', 0).fillna(0).astype(int)
    out_df['cmt_count'] = df.get('num_comments', 0).fillna(0).astype(int)
    
    # Reddit doesn't provide share or view count reliably in basic dumps, simulate if missing
    out_df['share_count'] = (out_df['upvote_count'] * 0.05).astype(int)
    out_df['view_count'] = (out_df['upvote_count'] * 15).astype(int)

    # Ratios
    total_votes = out_df['upvote_count'] + out_df['downvote_count']
    out_df['upvote_ratio'] = df.get('upvote_ratio', np.where(total_votes > 0, out_df['upvote_count'] / total_votes, 0.0))

    # Age and Hot Score
    # Assuming 'created_utc' is available
    if 'created_utc' in df.columns:
        current_time = datetime.utcnow().timestamp()
        out_df['post_age_hours'] = (current_time - df['created_utc']) / 3600.0
    else:
        out_df['post_age_hours'] = np.random.uniform(1, 72, len(df))

    out_df['hot_score'] = df.get('score', out_df['upvote_count']) # Simplified hot score

    # Content features
    if 'url' in df.columns:
        out_df['has_image'] = df['url'].str.contains(r'\.jpg|\.png|\.gif', case=False, na=False).astype(int)
    else:
        out_df['has_image'] = df.get('is_video', 0).astype(int) # fallback to video/image flag

    if 'selftext' in df.columns:
        out_df['content_length'] = df['selftext'].str.len().fillna(0).astype(int)
    else:
        out_df['content_length'] = df.get('title', '').str.len().fillna(0).astype(int)

    # Interaction features (Simulated for real datasets since we don't have individual user histories)
    out_df['interaction_count_7d'] = np.random.poisson(lam=2, size=len(df))
    out_df['interaction_count_30d'] = np.random.poisson(lam=5, size=len(df))
    out_df['affinity_score'] = np.random.exponential(scale=2, size=len(df))
    out_df['last_interaction_hours'] = np.random.uniform(0, 720, size=len(df))

    # Simulated relevance (Engagement label)
    out_df['relevance'] = (out_df['upvote_count'] > out_df['upvote_count'].median()).astype(int)

    # Save to CSV
    output_dir = Path(output_path).parent
    output_dir.mkdir(parents=True, exist_ok=True)
    
    out_df.to_csv(output_path, index=False)
    print(f"Processed Reddit dataset saved to {output_path}")
    print("Feature columns mapped successfully!")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Process Reddit dataset into FeatureSnapshot format.')
    parser.add_argument('--input', type=str, default='reddit_posts.csv', help='Path to raw Reddit CSV')
    parser.add_argument('--output', type=str, default='training/reddit_training_data.csv', help='Output path')
    
    args = parser.parse_args()
    parse_reddit_csv(args.input, args.output)
