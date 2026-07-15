"""data/users.json 기반 사용자 저장소.

repository 계층은 dict in/out만 다룬다 (Pydantic 등 검증/직렬화는 service
계층 책임). settings.DATA_DIR은 매 호출 시점에 참조한다 (테스트에서
override_settings로 임시 디렉터리를 주입하기 위함).
"""
from typing import Any

from django.conf import settings

from storage.json_store import read_json, update_json


def _users_path():
    return settings.DATA_DIR / "users.json"


def list_users() -> list[dict[str, Any]]:
    return read_json(_users_path(), default=[])


def get_user_by_username(username: str) -> dict[str, Any] | None:
    for user in list_users():
        if user["username"] == username:
            return user
    return None


def get_user_by_id(user_id: str) -> dict[str, Any] | None:
    for user in list_users():
        if user["id"] == user_id:
            return user
    return None


def create_user(user: dict[str, Any]) -> dict[str, Any]:
    def mutate(current):
        current = list(current)
        current.append(user)
        return current

    update_json(_users_path(), mutate, default=[])
    return user


def create_user_if_username_available(user: dict[str, Any]) -> bool:
    """username이 아직 없을 때만 사용자를 추가한다.

    중복 검사(조회)와 추가(쓰기)를 update_json의 락 내부에서 원자적으로
    수행해, 동시에 같은 username으로 signup이 들어와도 하나만 성공하도록
    보장한다(개별 get_user_by_username() 조회 후 create_user() 호출하는
    방식은 두 요청 사이에 TOCTOU 레이스가 발생할 수 있다).

    반환값은 실제로 생성됐는지 여부.
    """
    created = False

    def mutate(current):
        nonlocal created
        current = list(current)
        if any(existing["username"] == user["username"] for existing in current):
            created = False
            return current
        current.append(user)
        created = True
        return current

    update_json(_users_path(), mutate, default=[])
    return created
