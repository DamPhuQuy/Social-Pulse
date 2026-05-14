from __future__ import annotations

from collections import defaultdict
from dataclasses import dataclass
from datetime import datetime
import random
from typing import Any, Iterable


@dataclass(frozen=True)
class Interaction:
    user_id: str
    item_id: str
    subreddit: str
    event_type: str
    timestamp: int
    label: int = 1


@dataclass(frozen=True)
class BinarySample:
    user_id: str
    item_id: str
    label: int
    timestamp: int
    negative_type: str | None = None


@dataclass(frozen=True)
class PairwiseSample:
    user_id: str
    positive_item_id: str
    negative_item_id: str
    timestamp: int


@dataclass(frozen=True)
class HoldoutExample:
    user_id: str
    history: tuple[Interaction, ...]
    target: Interaction


def _normalize_text(value: Any) -> str:
    if value is None:
        return ""
    text = str(value).strip()
    if text in {"[deleted]", "[removed]", "nan", "None"}:
        return ""
    return text


def _to_timestamp(value: Any) -> int:
    if value is None or value == "":
        raise ValueError("timestamp is required")
    if isinstance(value, int):
        return value
    if isinstance(value, float):
        return int(value)
    text = str(value).strip()
    if text.isdigit():
        return int(text)
    return int(datetime.fromisoformat(text).timestamp())


def build_item_text(record: dict[str, Any]) -> str:
    parts = [
        _normalize_text(record.get("title")),
        _normalize_text(record.get("selftext")),
        _normalize_text(record.get("body")),
        _normalize_text(record.get("subreddit")),
    ]
    return " ".join(part for part in parts if part)


def infer_event_type(record: dict[str, Any]) -> str:
    if _normalize_text(record.get("body")):
        parent_id = _normalize_text(record.get("parent_id"))
        if parent_id.startswith("t1_"):
            return "reply"
        return "comment"
    return "post"


def build_positive_interactions(records: Iterable[dict[str, Any]]) -> list[Interaction]:
    interactions: dict[tuple[str, str], Interaction] = {}
    for record in records:
        user_id = _normalize_text(record.get("author"))
        item_id = _normalize_text(record.get("item_id") or record.get("post_id") or record.get("submission_id") or record.get("id"))
        subreddit = _normalize_text(record.get("subreddit"))
        if not user_id or not item_id or not subreddit:
            continue

        interaction = Interaction(
            user_id=user_id,
            item_id=item_id,
            subreddit=subreddit,
            event_type=infer_event_type(record),
            timestamp=_to_timestamp(record.get("created_utc") or record.get("timestamp")),
        )

        key = (interaction.user_id, interaction.item_id)
        existing = interactions.get(key)
        if existing is None or interaction.timestamp < existing.timestamp:
            interactions[key] = interaction

    return sorted(interactions.values(), key=lambda item: (item.user_id, item.timestamp, item.item_id))


def build_subreddit_interactions(interactions: Iterable[Interaction]) -> list[dict[str, Any]]:
    grouped: dict[tuple[str, str], dict[str, Any]] = {}
    for interaction in interactions:
        key = (interaction.user_id, interaction.subreddit)
        current = grouped.get(key)
        if current is None:
            grouped[key] = {
                "user_id": interaction.user_id,
                "subreddit": interaction.subreddit,
                "timestamp": interaction.timestamp,
                "interaction_count": 1,
            }
            continue
        current["interaction_count"] += 1
        current["timestamp"] = min(current["timestamp"], interaction.timestamp)
    return sorted(grouped.values(), key=lambda item: (item["user_id"], item["subreddit"]))


def build_binary_samples(
    positives: Iterable[Interaction],
    all_items: Iterable[dict[str, Any]],
    negatives_per_positive: int = 1,
    seed: int = 7,
) -> list[BinarySample]:
    positive_list = list(positives)
    samples = [
        BinarySample(
            user_id=interaction.user_id,
            item_id=interaction.item_id,
            label=1,
            timestamp=interaction.timestamp,
            negative_type=None,
        )
        for interaction in positive_list
    ]
    negatives = sample_weak_negatives(positive_list, all_items, negatives_per_positive, seed)
    samples.extend(negatives)
    return sorted(samples, key=lambda item: (item.user_id, item.timestamp, item.item_id, item.label))


def sample_weak_negatives(
    positives: Iterable[Interaction],
    all_items: Iterable[dict[str, Any]],
    negatives_per_positive: int = 1,
    seed: int = 7,
) -> list[BinarySample]:
    rng = random.Random(seed)
    positive_list = list(positives)
    user_positive_items: dict[str, set[str]] = defaultdict(set)
    for interaction in positive_list:
        user_positive_items[interaction.user_id].add(interaction.item_id)

    catalog = []
    for item in all_items:
        item_id = _normalize_text(item.get("item_id") or item.get("post_id") or item.get("submission_id") or item.get("id"))
        subreddit = _normalize_text(item.get("subreddit"))
        created_utc = _to_timestamp(item.get("created_utc") or item.get("timestamp"))
        if item_id:
            catalog.append({"item_id": item_id, "subreddit": subreddit, "created_utc": created_utc})

    negatives: dict[tuple[str, str], BinarySample] = {}
    for positive in positive_list:
        eligible = [
            item for item in catalog
            if item["created_utc"] <= positive.timestamp
            and item["item_id"] not in user_positive_items[positive.user_id]
        ]
        same_subreddit = [item for item in eligible if item["subreddit"] == positive.subreddit]
        pool = same_subreddit or eligible
        if not pool:
            continue

        sample_count = min(negatives_per_positive, len(pool))
        for candidate in rng.sample(pool, sample_count):
            key = (positive.user_id, candidate["item_id"])
            negatives[key] = BinarySample(
                user_id=positive.user_id,
                item_id=candidate["item_id"],
                label=0,
                timestamp=positive.timestamp,
                negative_type="sampled_negative",
            )
    return sorted(negatives.values(), key=lambda item: (item.user_id, item.timestamp, item.item_id))


def build_pairwise_samples(binary_samples: Iterable[BinarySample]) -> list[PairwiseSample]:
    positives_by_user_time: dict[tuple[str, int], list[str]] = defaultdict(list)
    negatives_by_user_time: dict[tuple[str, int], list[str]] = defaultdict(list)
    for sample in binary_samples:
        key = (sample.user_id, sample.timestamp)
        if sample.label == 1:
            positives_by_user_time[key].append(sample.item_id)
        else:
            negatives_by_user_time[key].append(sample.item_id)

    pairwise: list[PairwiseSample] = []
    for key, positives in positives_by_user_time.items():
        negatives = negatives_by_user_time.get(key, [])
        for positive_item in positives:
            for negative_item in negatives:
                pairwise.append(PairwiseSample(
                    user_id=key[0],
                    positive_item_id=positive_item,
                    negative_item_id=negative_item,
                    timestamp=key[1],
                ))
    return sorted(pairwise, key=lambda item: (item.user_id, item.timestamp, item.positive_item_id, item.negative_item_id))


def chronological_split(interactions: Iterable[Interaction], train_ratio: float = 0.8, validation_ratio: float = 0.1) -> dict[str, list[Interaction]]:
    ordered = sorted(interactions, key=lambda item: item.timestamp)
    total = len(ordered)
    train_end = int(total * train_ratio)
    validation_end = train_end + int(total * validation_ratio)
    return {
        "train": ordered[:train_end],
        "validation": ordered[train_end:validation_end],
        "test": ordered[validation_end:],
    }


def build_leave_one_out_holdout(interactions: Iterable[Interaction]) -> list[HoldoutExample]:
    by_user: dict[str, list[Interaction]] = defaultdict(list)
    for interaction in sorted(interactions, key=lambda item: (item.user_id, item.timestamp)):
        by_user[interaction.user_id].append(interaction)

    holdouts: list[HoldoutExample] = []
    for user_id, user_interactions in by_user.items():
        if len(user_interactions) < 2:
            continue
        holdouts.append(HoldoutExample(
            user_id=user_id,
            history=tuple(user_interactions[:-1]),
            target=user_interactions[-1],
        ))
    return holdouts
