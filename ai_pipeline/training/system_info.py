"""Runtime and hardware inspection helpers for training metadata."""
from __future__ import annotations

import os
import platform
import shutil
import subprocess
from typing import Any


def collect_runtime_info() -> dict[str, Any]:
    return {
        "platform": {
            "system": platform.system(),
            "release": platform.release(),
            "version": platform.version(),
            "machine": platform.machine(),
            "python_version": platform.python_version(),
        },
        "cpu": {
            "logical_cores": os.cpu_count(),
            "processor": platform.processor(),
        },
        "memory": _memory_info(),
        "gpu": _gpu_info(),
    }


def _memory_info() -> dict[str, Any]:
    try:
        import psutil
    except ImportError:
        return {}

    vm = psutil.virtual_memory()
    return {
        "total_gb": round(vm.total / (1024 ** 3), 2),
        "available_gb": round(vm.available / (1024 ** 3), 2),
    }


def _gpu_info() -> list[dict[str, Any]]:
    nvidia_smi = shutil.which("nvidia-smi")
    if not nvidia_smi:
        return []

    try:
        result = subprocess.run(
            [nvidia_smi, "--query-gpu=name,memory.total,driver_version", "--format=csv,noheader,nounits"],
            capture_output=True,
            text=True,
            check=True,
        )
    except Exception:
        return []

    gpus: list[dict[str, Any]] = []
    for line in result.stdout.splitlines():
        parts = [part.strip() for part in line.split(",")]
        if len(parts) != 3:
            continue
        name, memory_mb, driver = parts
        try:
            total_gb = round(float(memory_mb) / 1024.0, 2)
        except ValueError:
            total_gb = None
        gpus.append({"name": name, "memory_total_gb": total_gb, "driver_version": driver})
    return gpus
