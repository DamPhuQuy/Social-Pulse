import pandas as pd
import numpy as np
from sklearn.ensemble import GradientBoostingRegressor
from sklearn.model_selection import train_test_split
import joblib
from pathlib import Path

def generate_synthetic_data(n_samples=10000):
    """Generate synthetic training data mimicking Reddit distributions"""
    np.random.seed(42)

    # Base interaction metrics using log-normal distribution (power-law like)
    base_popularity = np.random.lognormal(mean=3.0, sigma=1.5, size=n_samples)
    
    upvote_count = np.maximum(0, (base_popularity * 10).astype(int))
    # Downvotes usually much lower than upvotes
    downvote_count = np.maximum(0, (upvote_count * np.random.uniform(0, 0.3, n_samples)).astype(int))
    cmt_count = np.maximum(0, (upvote_count * np.random.uniform(0.1, 0.5, n_samples)).astype(int))
    view_count = np.maximum(upvote_count + downvote_count, (upvote_count * np.random.uniform(5, 20, n_samples)).astype(int))
    share_count = np.maximum(0, (upvote_count * np.random.uniform(0, 0.1, n_samples)).astype(int))
    
    # Ratios and scores
    total_votes = upvote_count + downvote_count
    upvote_ratio = np.where(total_votes > 0, upvote_count / total_votes, 0.0)
    
    post_age_hours = np.random.exponential(scale=24, size=n_samples) # Most posts are recent
    
    # Simple hot score approximation (Reddit's algorithm involves log of score and time)
    score = upvote_count - downvote_count
    hot_score = np.where(score > 0, np.log10(np.maximum(1, score)) + (post_age_hours / 45000), 0.0)
    
    data = {
        # Post features
        'hot_score': hot_score,
        'upvote_ratio': upvote_ratio,
        'has_image': np.random.choice([0, 1], n_samples, p=[0.4, 0.6]),
        'content_length': np.maximum(10, np.random.lognormal(mean=4.0, sigma=1.0, size=n_samples).astype(int)),
        'post_age_hours': post_age_hours,
        'upvote_count': upvote_count,
        'downvote_count': downvote_count,
        'cmt_count': cmt_count,
        'share_count': share_count,
        'view_count': view_count,

        # Interaction features (historical)
        'interaction_count_7d': np.random.poisson(lam=2, size=n_samples),
        'interaction_count_30d': np.random.poisson(lam=5, size=n_samples),
        'affinity_score': np.random.exponential(scale=2, size=n_samples),
        'last_interaction_hours': np.random.uniform(0, 720, n_samples),
    }

    df = pd.DataFrame(data)

    # Generate target: engagement probability (0 to 1)
    # The more upvotes and affinity a user has, the higher the chance they engage
    target_score = (
        0.3 * df['upvote_ratio'] +
        0.2 * (np.log1p(df['hot_score']) / 2).clip(0, 1) +
        0.3 * (df['affinity_score'] / 10).clip(0, 1) +
        0.1 * (df['interaction_count_7d'] / 10).clip(0, 1) +
        0.1 * df['has_image'] +
        np.random.normal(0, 0.1, n_samples)
    ).clip(0, 1)
    
    # Binarize to simulate click (1) or ignore (0)
    df['relevance'] = (target_score > np.percentile(target_score, 70)).astype(int)

    return df

def train_model():
    """Train the ranking model"""
    print("Generating synthetic training data...")
    df = generate_synthetic_data(n_samples=10000)

    feature_columns = [
        'hot_score', 'upvote_ratio', 'has_image', 'content_length', 'post_age_hours',
        'upvote_count', 'downvote_count', 'cmt_count', 'share_count', 'view_count',
        'interaction_count_7d', 'interaction_count_30d', 'affinity_score', 'last_interaction_hours'
    ]

    X = df[feature_columns]
    y = df['relevance']

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

    model_path = Path(__file__).parent / "models" / "ranking_model.txt"
    model_path.parent.mkdir(parents=True, exist_ok=True)
    
    # Save using LightGBM/XGBoost style if we use them, but here it's scikit-learn GBR
    # Wait, the main.py expects a LightGBM model: lgb.Booster(model_file=str(model_path))
    # We should train a LightGBM model instead of scikit-learn GBR!
    
    import lightgbm as lgb
    
    lgb_train = lgb.Dataset(X_train, y_train)
    lgb_eval = lgb.Dataset(X_test, y_test, reference=lgb_train)
    
    params = {
        'objective': 'regression', # Or lambdarank if we have groups
        'metric': 'rmse',
        'learning_rate': 0.1,
        'max_depth': 5,
        'verbose': -1
    }
    
    print("Training LightGBM...")
    gbm = lgb.train(
        params,
        lgb_train,
        num_boost_round=100,
        valid_sets=[lgb_train, lgb_eval]
    )
    
    gbm.save_model(str(model_path))
    print(f"Model saved to {model_path}")
    
    # Save metadata
    import json
    from datetime import datetime
    metadata = {
        'model_type': 'LightGBM Regression (Simulated Ranker)',
        'n_features': len(feature_columns),
        'feature_names': feature_columns,
        'trained_at': datetime.now().isoformat(),
        'version': '1.0.0'
    }
    with open(model_path.parent / 'model_metadata.json', 'w') as f:
        json.dump(metadata, f, indent=2)

    return gbm

if __name__ == "__main__":
    train_model()
