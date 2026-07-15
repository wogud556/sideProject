from functools import wraps
from urllib.parse import urlencode

from django.http import Http404, JsonResponse
from django.shortcuts import redirect
from django.urls import reverse


def login_required(view_func):
    """비로그인 시 로그인 화면으로 리다이렉트하는 템플릿 뷰용 데코레이터."""

    @wraps(view_func)
    def wrapper(request, *args, **kwargs):
        if request.current_user is None:
            login_url = reverse("accounts:login")
            query = urlencode({"next": request.get_full_path()})
            return redirect(f"{login_url}?{query}")
        return view_func(request, *args, **kwargs)

    return wrapper


def api_login_required(view_func):
    """비로그인 시 401 JSON 응답을 반환하는 API 뷰용 데코레이터.

    또한 뷰(또는 그 안에서 호출하는 서비스 계층)가 던지는 Http404를
    Django 기본 HTML 404 페이지 대신 JSON 404 응답으로 변환한다(API
    응답은 항상 JSON이어야 하므로).
    """

    @wraps(view_func)
    def wrapper(request, *args, **kwargs):
        if request.current_user is None:
            return JsonResponse({"error": "authentication_required"}, status=401)
        try:
            return view_func(request, *args, **kwargs)
        except Http404 as exc:
            message = str(exc) or "요청한 리소스를 찾을 수 없습니다."
            return JsonResponse({"error_code": "NOT_FOUND", "message": message}, status=404)

    return wrapper
