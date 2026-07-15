from django.http import HttpRequest, HttpResponse
from django.shortcuts import redirect
from django.urls import include, path

from apps.accounts.views import csrf_token_view


def root_view(request: HttpRequest) -> HttpResponse:
    """로그인 상태면 대시보드로, 아니면 로그인 화면으로 보낸다."""
    if request.current_user is not None:
        return redirect("resumes:dashboard")
    return redirect("accounts:login")


urlpatterns = [
    path("accounts/", include("apps.accounts.urls")),
    path("resumes/", include("apps.resumes.urls")),
    path("dart/", include("apps.dart.urls")),
    # SPEC.md 5장 경로를 그대로 따르기 위해 두 앱의 API를 같은 /api/v1/ 아래
    # 나란히 마운트한다(dart의 "POST /resumes/{id}/careers/{cid}/dart-company"
    # 처럼 다른 앱의 URL 네임스페이스에 걸치는 엔드포인트가 있기 때문).
    path("api/v1/csrf", csrf_token_view, name="api_csrf"),
    path("api/v1/", include("apps.resumes.api.urls")),
    path("api/v1/", include("apps.dart.api.urls")),
    path("", root_view, name="root"),
]
