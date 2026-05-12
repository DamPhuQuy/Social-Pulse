"""
Data schemas for feed ranking system
"""
from dataclasses import dataclass
from typing import List, Optional
from datetime import datetime


@dataclass
class UserProfile:
    user_id: int
    follower_count: int
    following_count: int
    post_count: int
    account_age_days: int
    preferred_topics: List[str]
    engagement_rate: float
    avg_session_duration_minutes: float
    active_hours: List[int]


@dataclass
class Post:
    post_id: int
    author_id: int
    topic: str
    created_at: datetime
    content_length: int
    has_image: bool
    has_video: bool
    # Early signals (available at ranking time)
    author_follower_count: int
    author_avg_engagement_rate: float
    predicted_quality_score: float  # Pre-computed quality


@dataclass
class UserPostInteraction:
    user_id: int
    post_id: int
    timestamp: datetime
    # Ground truth labels (from real user behavior)
    engaged: bool  # Binary: did user engage?
    engagement_type: int  # 0=skip, 1=view, 2=click, 3=upvote, 4=comment, 5=share
    dwell_time_seconds: float
    position: int  # Rank in feed when shown


@dataclass
class RelationshipFeatures:
    user_id: int
    author_id: int
    follows: bool
    interaction_count_7d: int
    interaction_count_30d: int
    hours_since_last_interaction: float
    affinity_score: float  # Computed from past behavior
