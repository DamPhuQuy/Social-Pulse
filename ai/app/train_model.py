import pandas as pd
import numpy as np
from sklearn.ensemble import GradientBoostingRegressor
from sklearn.model_selection import train_test_split
import joblib
from pathlib import Path

def generate_synthetic_data(n_samples=10000):
    """Generate synthetic training data for feed ranking"""
    np.random.seed(42)

    data = {
        # Post features
        'upvote_count': np.random.poisson(50, n_samples),
        'downvote_count': np.random.poisson(5, n_samples),
        'cmt_count': np.random.poisson(20, n_samples),
        'view_count': np.random.poisson(500, n_samples),
        'share_count': np.random.poisson(10, n_samples),
        'hot_score': np.random.uniform(-10, 100, n_samples),
        'recency_score': np.random.uniform(0, 1, n_samples),
        'is_original': np.random.choice([0, 1], n_samples, p=[0.3, 0.7]),

        # Author features
        'author_follower_count': np.random.poisson(1000, n_samples),
        'author_following_count': np.random.poisson(500, n_samples),
        'author_engagement_rate': np.random.uniform(0, 1, n_samples),
        'author_post_count': np.random.poisson(100, n_samples),

        # Viewer features
        'viewer_follower_count': np.random.poisson(500, n_samples),
        'viewer_following_count': np.random.poisson(300, n_samples),
        'viewer_engagement_rate': np.random.uniform(0, 1, n_samples),
        'viewer_post_count': np.random.poisson(50, n_samples),
    }

    df = pd.DataFrame(data)

    # Generate target: engagement score (0-1)
    # Higher score = more likely user will engage
    df['engagement_score'] = (
        0.3 * (df['upvote_count'] / (df['upvote_count'] + df['downvote_count'] + 1)) +
        0.2 * np.log1p(df['cmt_count']) / 5 +
        0.15 * df['recency_score'] +
        0.15 * (df['hot_score'] / 100).clip(0, 1) +
        0.1 * np.log1p(df['author_follower_count']) / 10 +
        0.1 * df['author_engagement_rate'] +
        np.random.normal(0, 0.1, n_samples)
    ).clip(0, 1)

    return df

def train_model():
    """Train the ranking model"""
    print("Generating synthetic training data...")
    df = generate_synthetic_data(n_samples=10000)

    feature_columns = [
        'upvote_count', 'downvote_count', 'cmt_count', 'view_count', 'share_count',
        'hot_score', 'recency_score', 'is_original',
        'author_follower_count', 'author_following_count', 'author_engagement_rate', 'author_post_count',
        'viewer_follower_count', 'viewer_following_count', 'viewer_engagement_rate', 'viewer_post_count'
    ]

    X = df[feature_columns]
    y = df['engagement_score']

    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

    print("Training model...")
    model = GradientBoostingRegressor(
        n_estimators=100,
        learning_rate=0.1,
        max_depth=5,
        random_state=42
    )

    model.fit(X_train, y_train)

    train_score = model.score(X_train, y_train)
    test_score = model.score(X_test, y_test)

    print(f"Train R² score: {train_score:.4f}")
    print(f"Test R² score: {test_score:.4f}")

    model_path = Path(__file__).parent / "models" / "ranking_model.pkl"
    model_path.parent.mkdir(parents=True, exist_ok=True)

    joblib.dump(model, model_path)
    print(f"Model saved to {model_path}")

    return model

if __name__ == "__main__":
    train_model()
