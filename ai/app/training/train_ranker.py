"""
LightGBM Ranker training pipeline
Uses LambdaMART for learning-to-rank
"""
import lightgbm as lgb
import numpy as np
import pandas as pd
from sklearn.model_selection import GroupShuffleSplit
import joblib
from pathlib import Path
import json
from datetime import datetime


class RankingModelTrainer:
    """
    Train LightGBM Ranker with proper ranking objective
    NOT regression - uses LambdaMART for ranking
    """

    def __init__(self, config: dict = None):
        self.config = config or self._default_config()
        self.model = None
        self.feature_names = None
        self.training_history = {}

    def _default_config(self) -> dict:
        return {
            'objective': 'lambdarank',  # NOT regression!
            'metric': 'ndcg',  # Ranking metric
            'ndcg_eval_at': [5, 10, 20],
            'boosting_type': 'gbdt',
            'num_leaves': 63,
            'learning_rate': 0.05,
            'n_estimators': 500,  # More trees for production
            'max_depth': 7,
            'min_child_samples': 20,
            'subsample': 0.8,
            'colsample_bytree': 0.8,
            'reg_alpha': 0.1,
            'reg_lambda': 0.1,
            'random_state': 42,
            'n_jobs': -1,
            'verbose': 1
        }

    def train(
        self,
        X: np.ndarray,
        y: np.ndarray,
        groups: np.ndarray,
        feature_names: list,
        validation_split: float = 0.2
    ):
        """
        Train LightGBM Ranker

        Args:
            X: Features (n_samples, n_features)
            y: Labels (engagement_type: 0-5)
            groups: Number of posts per user (query)
            feature_names: List of feature names
        """
        self.feature_names = feature_names

        # Split by groups (keep all posts for a user together)
        print("Splitting data by groups...")
        X_train, X_val, y_train, y_val, groups_train, groups_val = self._group_split(
            X, y, groups, test_size=validation_split
        )

        print(f"Train: {len(X_train)} samples, {len(groups_train)} groups")
        print(f"Val: {len(X_val)} samples, {len(groups_val)} groups")

        # Create LightGBM datasets
        train_data = lgb.Dataset(
            X_train,
            label=y_train,
            group=groups_train,
            feature_name=feature_names
        )

        val_data = lgb.Dataset(
            X_val,
            label=y_val,
            group=groups_val,
            feature_name=feature_names,
            reference=train_data
        )

        # Train model
        print("\nTraining LightGBM Ranker...")
        callbacks = [
            lgb.log_evaluation(period=50),
            lgb.early_stopping(stopping_rounds=50)
        ]

        self.model = lgb.train(
            self.config,
            train_data,
            valid_sets=[train_data, val_data],
            valid_names=['train', 'valid'],
            callbacks=callbacks
        )

        # Evaluate
        print("\n" + "="*50)
        print("Training completed!")
        print("="*50)

        train_metrics = self._evaluate(X_train, y_train, groups_train, "Train")
        val_metrics = self._evaluate(X_val, y_val, groups_val, "Validation")

        self.training_history = {
            'train_metrics': train_metrics,
            'val_metrics': val_metrics,
            'best_iteration': self.model.best_iteration,
            'feature_importance': self._get_feature_importance()
        }

        return self.model

    def _group_split(
        self,
        X: np.ndarray,
        y: np.ndarray,
        groups: np.ndarray,
        test_size: float = 0.2
    ):
        """Split data by groups (users)"""

        # Create group IDs
        group_ids = np.repeat(np.arange(len(groups)), groups)

        # Split by groups
        gss = GroupShuffleSplit(n_splits=1, test_size=test_size, random_state=42)
        train_idx, val_idx = next(gss.split(X, y, groups=group_ids))

        X_train, X_val = X[train_idx], X[val_idx]
        y_train, y_val = y[train_idx], y[val_idx]

        # Recalculate group sizes for train/val
        train_group_ids = group_ids[train_idx]
        val_group_ids = group_ids[val_idx]

        groups_train = np.bincount(train_group_ids)
        groups_train = groups_train[groups_train > 0]

        groups_val = np.bincount(val_group_ids)
        groups_val = groups_val[groups_val > 0]

        return X_train, X_val, y_train, y_val, groups_train, groups_val

    def _evaluate(self, X: np.ndarray, y: np.ndarray, groups: np.ndarray, name: str) -> dict:
        """Evaluate ranking metrics"""

        y_pred = self.model.predict(X)

        # Calculate NDCG@K
        ndcg_scores = self._calculate_ndcg(y, y_pred, groups, k_values=[5, 10, 20])

        # Calculate MAP
        map_score = self._calculate_map(y, y_pred, groups)

        metrics = {
            'ndcg@5': ndcg_scores[5],
            'ndcg@10': ndcg_scores[10],
            'ndcg@20': ndcg_scores[20],
            'map': map_score
        }

        print(f"\n{name} Metrics:")
        for metric, value in metrics.items():
            print(f"  {metric}: {value:.4f}")

        return metrics

    def _calculate_ndcg(self, y_true: np.ndarray, y_pred: np.ndarray, groups: np.ndarray, k_values: list) -> dict:
        """Calculate NDCG@K"""

        ndcg_scores = {k: [] for k in k_values}

        start_idx = 0
        for group_size in groups:
            end_idx = start_idx + group_size

            y_true_group = y_true[start_idx:end_idx]
            y_pred_group = y_pred[start_idx:end_idx]

            # Sort by predicted scores
            sorted_indices = np.argsort(y_pred_group)[::-1]
            y_true_sorted = y_true_group[sorted_indices]

            for k in k_values:
                ndcg_scores[k].append(self._ndcg_at_k(y_true_sorted, k))

            start_idx = end_idx

        return {k: np.mean(scores) for k, scores in ndcg_scores.items()}

    def _ndcg_at_k(self, y_true: np.ndarray, k: int) -> float:
        """Calculate NDCG@K for a single query"""

        k = min(k, len(y_true))
        y_true_k = y_true[:k]

        # DCG
        dcg = np.sum((2 ** y_true_k - 1) / np.log2(np.arange(2, k + 2)))

        # IDCG (ideal DCG)
        y_true_sorted = np.sort(y_true)[::-1][:k]
        idcg = np.sum((2 ** y_true_sorted - 1) / np.log2(np.arange(2, k + 2)))

        return dcg / idcg if idcg > 0 else 0.0

    def _calculate_map(self, y_true: np.ndarray, y_pred: np.ndarray, groups: np.ndarray) -> float:
        """Calculate Mean Average Precision"""

        ap_scores = []

        start_idx = 0
        for group_size in groups:
            end_idx = start_idx + group_size

            y_true_group = y_true[start_idx:end_idx]
            y_pred_group = y_pred[start_idx:end_idx]

            # Sort by predicted scores
            sorted_indices = np.argsort(y_pred_group)[::-1]
            y_true_sorted = y_true_group[sorted_indices]

            # Calculate AP
            relevant = y_true_sorted > 0
            if relevant.sum() == 0:
                ap_scores.append(0.0)
            else:
                precisions = np.cumsum(relevant) / np.arange(1, len(relevant) + 1)
                ap = np.sum(precisions * relevant) / relevant.sum()
                ap_scores.append(ap)

            start_idx = end_idx

        return np.mean(ap_scores)

    def _get_feature_importance(self) -> dict:
        """Get feature importance"""

        importance = self.model.feature_importance(importance_type='gain')
        feature_importance = dict(zip(self.feature_names, importance))

        # Sort by importance
        feature_importance = dict(sorted(feature_importance.items(), key=lambda x: x[1], reverse=True))

        return feature_importance

    def save_model(self, output_dir: str = "models"):
        """Save model and metadata"""

        output_path = Path(output_dir)
        output_path.mkdir(parents=True, exist_ok=True)

        # Save model
        model_path = output_path / "ranking_model.txt"
        self.model.save_model(str(model_path))

        # Save metadata
        metadata = {
            'model_type': 'LightGBM Ranker',
            'objective': 'lambdarank',
            'feature_names': self.feature_names,
            'n_features': len(self.feature_names),
            'training_history': self.training_history,
            'config': self.config,
            'trained_at': datetime.now().isoformat(),
            'version': '1.0.0'
        }

        metadata_path = output_path / "model_metadata.json"
        with open(metadata_path, 'w') as f:
            json.dump(metadata, f, indent=2)

        # Save feature importance plot data
        importance_df = pd.DataFrame([
            {'feature': k, 'importance': v}
            for k, v in self.training_history['feature_importance'].items()
        ])
        importance_df.to_csv(output_path / "feature_importance.csv", index=False)

        print(f"\n✅ Model saved to {output_path}")
        print(f"  - Model: {model_path}")
        print(f"  - Metadata: {metadata_path}")
        print(f"  - Feature importance: {output_path / 'feature_importance.csv'}")

        return str(model_path)


if __name__ == "__main__":
    from data.feature_engineering import FeatureEngineer

    # Load data
    print("Loading data...")
    users = pd.read_parquet('data/users.parquet')
    posts = pd.read_parquet('data/posts.parquet')
    relationships = pd.read_parquet('data/relationships.parquet')
    interactions = pd.read_parquet('data/interactions.parquet')

    # Extract features
    print("\nExtracting features...")
    engineer = FeatureEngineer()
    df = engineer.extract_features(interactions, users, posts, relationships)

    # Prepare ranking data
    X, y, groups = engineer.prepare_ranking_data(df)

    # Train model
    print("\n" + "="*50)
    print("Training LightGBM Ranker")
    print("="*50)

    trainer = RankingModelTrainer()
    model = trainer.train(X, y, groups, engineer.feature_columns)

    # Save model
    trainer.save_model()

    # Print top features
    print("\n" + "="*50)
    print("Top 10 Most Important Features:")
    print("="*50)
    for i, (feat, importance) in enumerate(list(trainer.training_history['feature_importance'].items())[:10], 1):
        print(f"{i:2d}. {feat:40s} {importance:10.1f}")
