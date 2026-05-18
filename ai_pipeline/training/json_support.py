"""JSON utilities: zstd line reader, text normalization, serialization."""
from __future__ import annotations

import json
from pathlib import Path
from typing import Any, Iterator


def normalize_text(value: str | None) -> str:
    if value is None:
        return ""
    normalized = value.strip()
    if normalized.lower() in ("[deleted]", "[removed]", "null"):
        return ""
    return normalized


def strip_thing_prefix(value: str) -> str:
    sep = value.find("_")
    return value[sep + 1:] if sep >= 0 else value


def text_value(node: dict, key: str) -> str:
    v = node.get(key)
    if v is None:
        return ""
    return str(v)


def int_value(node: dict, key: str) -> int:
    v = node.get(key)
    if v is None:
        return 0
    if isinstance(v, (int, float)):
        return int(round(v))
    try:
        return int(round(float(str(v))))
    except (ValueError, TypeError):
        return 0


def double_value(node: dict, key: str) -> float:
    v = optional_double_value(node, key)
    return v if v is not None else 0.0


def optional_double_value(node: dict, key: str) -> float | None:
    v = node.get(key)
    if v is None:
        return None
    if isinstance(v, (int, float)):
        return float(v)
    s = str(v).strip()
    if not s:
        return None
    try:
        return float(s)
    except (ValueError, TypeError):
        return None


def round6(value: float) -> float:
    return round(value * 1_000_000) / 1_000_000


def write_json(output_path: Path, data: Any) -> None:
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(data, indent=2, default=str), encoding="utf-8")


def to_pretty_json(data: Any) -> str:
    return json.dumps(data, indent=2, default=str)


class JsonLineReader:
    """Streams JSON lines from a .zst compressed file."""

    def __init__(self, path: Path):
        import zstandard as zstd
        self._fh = open(path, "rb")
        self._dctx = zstd.ZstdDecompressor()
        self._reader = self._dctx.stream_reader(self._fh)
        self._text_stream = self._reader.__enter__()
        import io
        self._lines = io.TextIOWrapper(self._text_stream, encoding="utf-8")

    def __iter__(self) -> Iterator[dict]:
        return self

    def __next__(self) -> dict:
        while True:
            line = self._lines.readline()
            if not line:
                raise StopIteration
            line = line.strip()
            if not line:
                continue
            try:
                return json.loads(line)
            except json.JSONDecodeError:
                continue

    def close(self) -> None:
        self._lines.close()
        self._fh.close()

    def __enter__(self):
        return self

    def __exit__(self, *_):
        self.close()
