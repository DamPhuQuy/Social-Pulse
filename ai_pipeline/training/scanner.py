"""Dataset scanner: reads .zst archives, reservoir-samples posts, extracts interactions."""
from __future__ import annotations

import random
from collections import defaultdict
from pathlib import Path

from ai_pipeline.shared.schema import LightGbmFeatureSchema
from . import json_support as js
from .arguments import TrainingArguments
from .types import AuthorAggregate, InteractionScanResult, ScanResult, SubmissionRecord

_HOT_SCORE_TIME_DIVISOR = 45000.0
_REDDIT_EPOCH = 1134028003

_MEDIA_EXTENSIONS = (".jpg", ".jpeg", ".png", ".gif", ".webp", ".mp4", ".mov")
_SKIP_THUMBNAILS = {"", "self", "default", "nsfw", "image"}


class PushshiftDatasetScanner:

    def scan_submissions(self, arguments: TrainingArguments) -> ScanResult:
        rng = random.Random(arguments.seed)
        reservoir: list[SubmissionRecord] = []
        author_aggregates: dict[str, AuthorAggregate] = {}

        scanned = filtered = accepted = 0

        with js.JsonLineReader(arguments.submissions_path) as reader:
            for payload in reader:
                scanned += 1
                record = self._preprocess_submission(payload, arguments.min_content_length)
                if record is None:
                    filtered += 1
                    continue

                accepted += 1
                popularity = self.popularity(record.score, record.num_comments, record.num_crossposts)
                agg = author_aggregates.setdefault(record.author, AuthorAggregate())
                agg.increment(popularity)

                if len(reservoir) < arguments.sample_size:
                    reservoir.append(record)
                else:
                    idx = rng.randint(0, accepted - 1)
                    if idx < arguments.sample_size:
                        reservoir[idx] = record

                if accepted >= arguments.scan_limit_posts:
                    break

        stats = {
            "submissions_scanned": scanned,
            "submissions_filtered": filtered,
            "submissions_accepted": accepted,
            "reservoir_size": len(reservoir),
        }
        return ScanResult(list(reservoir), dict(author_aggregates), stats)

    def scan_interactions(
        self, comments_path: Path, post_author_map: dict[str, str], scan_limit: int
    ) -> InteractionScanResult:
        interactions: dict[str, dict[str, list[float]]] = defaultdict(lambda: defaultdict(list))
        scanned = matched = 0

        with js.JsonLineReader(comments_path) as reader:
            for payload in reader:
                scanned += 1
                commenter = js.normalize_text(payload.get("author"))
                if not commenter or commenter.lower() == "[deleted]":
                    continue

                link_id = payload.get("link_id", "")
                post_id = js.strip_thing_prefix(str(link_id))
                post_author = post_author_map.get(post_id)
                if post_author is None or post_author.lower() == commenter.lower():
                    continue

                created_utc = js.double_value(payload, "created_utc")
                if created_utc <= 0:
                    continue

                matched += 1
                interactions[commenter][post_author].append(created_utc)

                if scanned >= scan_limit:
                    break

        stats = {
            "comments_scanned": scanned,
            "interactions_extracted": matched,
            "unique_viewers": len(interactions),
        }
        # Convert defaultdicts to regular dicts
        return InteractionScanResult(
            {k: dict(v) for k, v in interactions.items()}, stats
        )

    def _preprocess_submission(self, payload: dict, min_content_length: int) -> SubmissionRecord | None:
        author = js.normalize_text(payload.get("author"))
        title = js.normalize_text(payload.get("title"))
        body = js.normalize_text(payload.get("selftext"))
        created_utc = js.double_value(payload, "created_utc")
        retrieved_on = js.double_value(payload, "retrieved_on") if payload.get("retrieved_on") is not None else created_utc
        score = max(0, js.int_value(payload, "score"))
        num_comments = max(0, js.int_value(payload, "num_comments"))
        num_crossposts = max(0, js.int_value(payload, "num_crossposts"))

        if not author or author.lower() in ("[deleted]", "automoderator"):
            return None
        if created_utc <= 0:
            return None
        if not title and not body:
            return None
        if len(title) + len(body) < min_content_length:
            return None

        post_id = js.normalize_text(payload.get("id"))
        if not post_id:
            return None

        raw_ratio = js.optional_double_value(payload, "upvote_ratio")
        upvote_ratio = raw_ratio if (raw_ratio is not None and 0.0 <= raw_ratio <= 1.0) else LightGbmFeatureSchema.DEFAULT_UPVOTE_RATIO

        return SubmissionRecord(
            post_id=post_id,
            author=author,
            author_created_utc=js.optional_double_value(payload, "author_created_utc"),
            created_utc=created_utc,
            retrieved_on=retrieved_on,
            title_length=len(title),
            body_length=len(body),
            score=score,
            num_comments=num_comments,
            num_crossposts=num_crossposts,
            has_multimedia=self._detect_multimedia(payload),
            is_share_post=self._detect_share_post(payload),
            hot_score=self._reddit_hot_score(score, created_utc),
            upvote_ratio=upvote_ratio,
        )

    @staticmethod
    def popularity(score: int, num_comments: int, num_crossposts: int) -> float:
        return float(max(score, 0) + num_comments + num_crossposts)

    @staticmethod
    def _detect_multimedia(payload: dict) -> bool:
        if payload.get("is_video"):
            return True
        if payload.get("media") is not None:
            return True
        if payload.get("secure_media") is not None:
            return True
        thumb = js.normalize_text(payload.get("thumbnail")).lower()
        if thumb and thumb not in _SKIP_THUMBNAILS:
            return True
        url = str(payload.get("url", "")).lower()
        return any(url.endswith(ext) for ext in _MEDIA_EXTENSIONS)

    @staticmethod
    def _detect_share_post(payload: dict) -> bool:
        return js.int_value(payload, "num_crossposts") > 0 or payload.get("crosspost_parent") is not None

    @staticmethod
    def _reddit_hot_score(score: int, created_utc: float) -> float:
        import math
        order = math.log10(max(abs(score), 1))
        sign = 1.0 if score > 0 else (-1.0 if score < 0 else 0.0)
        seconds = created_utc - _REDDIT_EPOCH
        return js.round6(sign * order + seconds / _HOT_SCORE_TIME_DIVISOR)
