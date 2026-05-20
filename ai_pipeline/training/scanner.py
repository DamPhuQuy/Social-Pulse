"""Dataset scanner: reads .zst archives, reservoir-samples posts, extracts interactions."""
from __future__ import annotations

import random
import re
import time
from collections import defaultdict
from hashlib import sha1
from pathlib import Path

from ai_pipeline.shared.schema import RankingFeatureSchema
from . import json_support as js
from .arguments import TrainingArguments
from .types import AuthorAggregate, InteractionScanResult, ScanResult, SubmissionRecord

_HOT_SCORE_TIME_DIVISOR = 45000.0
_REDDIT_EPOCH = 1134028003

_MEDIA_EXTENSIONS = (".jpg", ".jpeg", ".png", ".gif", ".webp", ".mp4", ".mov")
_SKIP_THUMBNAILS = {"", "self", "default", "nsfw", "image"}
_PROGRESS_EVERY_SECONDS = 2.0
_PROGRESS_EVERY_PERCENT = 2
_URL_PATTERN = re.compile(r"(https?://\S+|www\.\S+)", re.IGNORECASE)
_TOKEN_PATTERN = re.compile(r"[a-z0-9]{2,}", re.IGNORECASE)
_LIKELY_BOT_AUTHORS = {
    "automoderator",
    "tweetposter",
    "imgurtranscriber",
    "gifreversingbot",
    "remindmebot",
    "vredditdownloader",
}


class PushshiftDatasetScanner:

    def scan_submissions(self, arguments: TrainingArguments) -> ScanResult:
        rng = random.Random(arguments.seed)
        reservoir: list[SubmissionRecord] = []
        author_aggregates: dict[str, AuthorAggregate] = {}
        seen_signatures: set[bytes] = set()
        filter_reasons: dict[str, int] = defaultdict(int)

        scanned = filtered = accepted = 0

        with js.JsonLineReader(arguments.submissions_path) as reader:
            progress = _ProgressReporter("submissions", arguments.submissions_path)
            for payload in reader:
                scanned += 1
                record, reason, signature = self._preprocess_submission(payload, arguments)
                if record is None:
                    filtered += 1
                    if reason:
                        filter_reasons[reason] += 1
                    continue
                if arguments.dedupe_posts and signature:
                    if signature in seen_signatures:
                        filtered += 1
                        filter_reasons["duplicate_content"] += 1
                        continue
                    seen_signatures.add(signature)

                accepted += 1
                popularity = self.popularity(record.score, record.num_comments, record.num_crossposts)
                agg = author_aggregates.setdefault(record.author, AuthorAggregate())
                agg.increment(popularity)

                if arguments.sample_size > 0:
                    if len(reservoir) < arguments.sample_size:
                        reservoir.append(record)
                    else:
                        idx = rng.randint(0, accepted - 1)
                        if idx < arguments.sample_size:
                            reservoir[idx] = record
                else:
                    reservoir.append(record)

                progress.maybe_report(
                    reader.progress_percent,
                    scanned_records=scanned,
                    accepted_records=accepted,
                    extra=f"filtered={filtered:,} sample={len(reservoir):,}",
                )
                if arguments.scan_limit_posts > 0 and accepted >= arguments.scan_limit_posts:
                    break
            progress.finish(
                reader.progress_percent,
                scanned_records=scanned,
                accepted_records=accepted,
                extra=f"filtered={filtered:,} sample={len(reservoir):,}",
            )

        stats = {
            "submissions_scanned": scanned,
            "submissions_filtered": filtered,
            "submissions_accepted": accepted,
            "reservoir_size": len(reservoir),
            "distinct_content_signatures": len(seen_signatures),
            "filter_reasons": dict(sorted(filter_reasons.items())),
        }
        return ScanResult(list(reservoir), dict(author_aggregates), stats)

    def scan_interactions(
        self,
        comments_path: Path,
        post_author_map: dict[str, str],
        scan_limit: int,
        arguments: TrainingArguments,
    ) -> InteractionScanResult:
        interactions: dict[str, dict[str, list[float]]] = defaultdict(lambda: defaultdict(list))
        scanned = matched = 0
        skipped_reasons: dict[str, int] = defaultdict(int)

        with js.JsonLineReader(comments_path) as reader:
            progress = _ProgressReporter("comments", comments_path)
            for payload in reader:
                scanned += 1
                commenter = js.normalize_text(payload.get("author"))
                if not commenter or commenter.lower() == "[deleted]":
                    skipped_reasons["invalid_author"] += 1
                    progress.maybe_report(
                        reader.progress_percent,
                        scanned_records=scanned,
                        accepted_records=matched,
                        extra=f"unique_viewers={len(interactions):,}",
                    )
                    continue
                if arguments.filter_bots and self._is_likely_bot_author(commenter):
                    skipped_reasons["bot_author"] += 1
                    progress.maybe_report(
                        reader.progress_percent,
                        scanned_records=scanned,
                        accepted_records=matched,
                        extra=f"unique_viewers={len(interactions):,}",
                    )
                    continue

                link_id = payload.get("link_id", "")
                post_id = js.strip_thing_prefix(str(link_id))
                post_author = post_author_map.get(post_id)
                if post_author is None:
                    skipped_reasons["unmapped_post"] += 1
                    continue
                if post_author.lower() == commenter.lower():
                    skipped_reasons["self_comment"] += 1
                    continue

                created_utc = js.double_value(payload, "created_utc")
                if created_utc <= 0:
                    skipped_reasons["invalid_timestamp"] += 1
                    progress.maybe_report(
                        reader.progress_percent,
                        scanned_records=scanned,
                        accepted_records=matched,
                        extra=f"unique_viewers={len(interactions):,}",
                    )
                    continue

                matched += 1
                interactions[commenter][post_author].append(created_utc)

                progress.maybe_report(
                    reader.progress_percent,
                    scanned_records=scanned,
                    accepted_records=matched,
                    extra=f"unique_viewers={len(interactions):,}",
                )
                if scan_limit > 0 and scanned >= scan_limit:
                    break
            progress.finish(
                reader.progress_percent,
                scanned_records=scanned,
                accepted_records=matched,
                extra=f"unique_viewers={len(interactions):,}",
            )

        stats = {
            "comments_scanned": scanned,
            "interactions_extracted": matched,
            "unique_viewers": len(interactions),
            "skipped_reasons": dict(sorted(skipped_reasons.items())),
        }
        # Convert defaultdicts to regular dicts
        return InteractionScanResult(
            {k: dict(v) for k, v in interactions.items()}, stats
        )

    def _preprocess_submission(
        self,
        payload: dict,
        arguments: TrainingArguments,
    ) -> tuple[SubmissionRecord | None, str | None, bytes | None]:
        author = js.normalize_text(payload.get("author"))
        title = js.normalize_text(payload.get("title"))
        body = js.normalize_text(payload.get("selftext"))
        created_utc = js.double_value(payload, "created_utc")
        retrieved_on = js.double_value(payload, "retrieved_on") if payload.get("retrieved_on") is not None else created_utc
        score = max(0, js.int_value(payload, "score"))
        num_comments = max(0, js.int_value(payload, "num_comments"))
        num_crossposts = max(0, js.int_value(payload, "num_crossposts"))
        combined_text = self._compose_text(title, body)

        if not author:
            return None, "missing_author", None
        if arguments.filter_bots and self._is_likely_bot_author(author):
            return None, "bot_author", None
        if created_utc <= 0:
            return None, "invalid_timestamp", None
        if not title and not body:
            return None, "empty_content", None
        if arguments.exclude_nsfw and bool(payload.get("over_18")):
            return None, "nsfw", None
        if len(combined_text) < arguments.min_content_length:
            return None, "too_short", None
        if len(combined_text) > arguments.max_content_length:
            return None, "too_long", None
        low_signal_reason = self._low_signal_reason(
            combined_text,
            min_distinct_token_count=arguments.min_distinct_token_count,
            min_alpha_char_count=arguments.min_alpha_char_count,
            max_url_count=arguments.max_url_count,
        )
        if low_signal_reason is not None:
            return None, low_signal_reason, None

        post_id = js.normalize_text(payload.get("id"))
        if not post_id:
            return None, "missing_post_id", None

        raw_ratio = js.optional_double_value(payload, "upvote_ratio")
        upvote_ratio = raw_ratio if (raw_ratio is not None and 0.0 <= raw_ratio <= 1.0) else RankingFeatureSchema.DEFAULT_UPVOTE_RATIO
        content_signature = self._content_signature(title, body)

        return (
            SubmissionRecord(
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
            ),
            None,
            content_signature,
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

    @staticmethod
    def _compose_text(title: str, body: str) -> str:
        return f"{title}\n{body}".strip()

    @staticmethod
    def _is_likely_bot_author(author: str) -> bool:
        normalized = author.strip().lower()
        if normalized in _LIKELY_BOT_AUTHORS:
            return True
        return normalized.endswith("bot") or normalized.endswith("-bot") or normalized.endswith("_bot")

    @staticmethod
    def _low_signal_reason(
        text: str,
        min_distinct_token_count: int,
        min_alpha_char_count: int,
        max_url_count: int,
    ) -> str | None:
        normalized = " ".join(text.split())
        alpha_count = sum(1 for ch in normalized if ch.isalpha())
        if alpha_count < min_alpha_char_count:
            return "low_alpha_content"

        url_count = len(_URL_PATTERN.findall(normalized))
        if url_count > max_url_count:
            return "too_many_urls"

        tokens = {token.lower() for token in _TOKEN_PATTERN.findall(normalized)}
        if len(tokens) < min_distinct_token_count:
            return "low_token_diversity"

        compact = normalized.replace(" ", "")
        if compact:
            char_counts: dict[str, int] = defaultdict(int)
            for ch in compact.lower():
                char_counts[ch] += 1
            dominant_ratio = max(char_counts.values()) / len(compact)
            if dominant_ratio > 0.45:
                return "repetitive_content"
        return None

    @staticmethod
    def _content_signature(title: str, body: str) -> bytes:
        normalized = " ".join(f"{title} {body}".lower().split())
        return sha1(normalized.encode("utf-8")).digest()


class _ProgressReporter:
    def __init__(self, stage: str, path: Path):
        self._stage = stage
        self._path = path.name
        self._started_at = time.monotonic()
        self._last_report_at = self._started_at
        self._last_percent_bucket = -1

    def maybe_report(
        self,
        percent: float,
        scanned_records: int,
        accepted_records: int,
        extra: str = "",
    ) -> None:
        now = time.monotonic()
        percent_bucket = int(percent // _PROGRESS_EVERY_PERCENT)
        if percent_bucket <= self._last_percent_bucket and (now - self._last_report_at) < _PROGRESS_EVERY_SECONDS:
            return
        self._last_percent_bucket = percent_bucket
        self._last_report_at = now
        self._print(percent, scanned_records, accepted_records, extra, done=False)

    def finish(
        self,
        percent: float,
        scanned_records: int,
        accepted_records: int,
        extra: str = "",
    ) -> None:
        self._print(percent, scanned_records, accepted_records, extra, done=True)

    def _print(
        self,
        percent: float,
        scanned_records: int,
        accepted_records: int,
        extra: str,
        done: bool,
    ) -> None:
        elapsed = max(0.001, time.monotonic() - self._started_at)
        speed = int(scanned_records / elapsed)
        status = "done" if done else "progress"
        suffix = f" | {extra}" if extra else ""
        print(
            f"[{self._stage}:{status}] {percent:6.2f}% | file={self._path} | "
            f"scanned={scanned_records:,} | accepted={accepted_records:,} | "
            f"rate={speed:,}/s{suffix}"
        )
