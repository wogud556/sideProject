"""JSON 파일 기반 저장소 유틸리티.

이 프로젝트는 데이터베이스를 전혀 사용하지 않는다. 모든 영속 데이터는
JSON 파일로 저장하며, 쓰기는 임시 파일에 기록한 뒤 os.replace로 교체하는
방식으로 원자성을 보장한다. 동일 경로에 대한 동시 read-modify-write는
경로별 threading.Lock으로 보호한다 (단일 프로세스 가정, 프로토타입 수준).
"""
import json
import os
import threading
from pathlib import Path
from typing import Any, Callable

_locks: dict[str, threading.Lock] = {}
_locks_guard = threading.Lock()


def _lock_for(path: Path) -> threading.Lock:
    key = str(Path(path))
    with _locks_guard:
        lock = _locks.get(key)
        if lock is None:
            lock = threading.Lock()
            _locks[key] = lock
        return lock


def read_json(path: Path | str, default: Any = None) -> Any:
    """path의 JSON을 읽어 반환한다. 파일이 없으면 default를 반환한다."""
    path = Path(path)
    if not path.exists():
        return default
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def write_json(path: Path | str, data: Any) -> None:
    """data를 path에 원자적으로 기록한다.

    부모 디렉터리가 없으면 생성하고, 동일 디렉터리 내 임시 파일에 먼저 쓴
    뒤 os.replace로 교체한다 (도중에 프로세스가 죽어도 원본 파일이 깨지지
    않는다).
    """
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    tmp_path = path.with_name(f".{path.name}.tmp-{os.getpid()}-{threading.get_ident()}")
    with open(tmp_path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    os.replace(tmp_path, path)


def update_json(path: Path | str, mutate_fn: Callable[[Any], Any], default: Any = None) -> Any:
    """path의 JSON을 읽고 mutate_fn을 적용한 뒤 다시 저장한다.

    mutate_fn(current) -> new_value 형태로, 반환값이 저장된다.
    동일 경로에 대한 동시 호출은 Lock으로 직렬화된다.
    """
    path = Path(path)
    lock = _lock_for(path)
    with lock:
        current = read_json(path, default)
        new_value = mutate_fn(current)
        write_json(path, new_value)
        return new_value
