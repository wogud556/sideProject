"""회원가입/로그인/로그아웃 비즈니스 로직.

DB 없이 data/users.json에 저장된 계정을 다룬다. 비밀번호 해시는
django.contrib.auth.hashers를 사용한다 (DB 접근 없이 순수 해시 함수만 사용).
"""
import uuid
from typing import Any

from django.contrib.auth.hashers import check_password, make_password
from django.utils import timezone

from . import repository

SESSION_USER_ID_KEY = "user_id"


class UsernameAlreadyExistsError(Exception):
    pass


def signup(username: str, email: str, password: str) -> dict[str, Any]:
    user = {
        "id": str(uuid.uuid4()),
        "username": username,
        "email": email,
        "password_hash": make_password(password),
        "created_at": timezone.now().isoformat(),
    }
    # 중복 검사와 추가를 하나의 락 안에서 원자적으로 수행한다(TOCTOU 방지).
    if not repository.create_user_if_username_available(user):
        raise UsernameAlreadyExistsError(f"이미 사용 중인 아이디입니다: {username}")
    return user


def authenticate(username: str, password: str) -> dict[str, Any] | None:
    user = repository.get_user_by_username(username)
    if user is None:
        return None
    if not check_password(password, user["password_hash"]):
        return None
    return user


def login(request, user: dict[str, Any]) -> None:
    request.session[SESSION_USER_ID_KEY] = user["id"]


def logout(request) -> None:
    request.session.pop(SESSION_USER_ID_KEY, None)


def get_current_user(request) -> dict[str, Any] | None:
    user_id = request.session.get(SESSION_USER_ID_KEY)
    if user_id is None:
        return None
    return repository.get_user_by_id(user_id)
