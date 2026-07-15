from django.contrib import messages
from django.http import HttpRequest, HttpResponse, JsonResponse
from django.middleware.csrf import get_token
from django.shortcuts import redirect, render
from django.urls import reverse
from django.utils.http import url_has_allowed_host_and_scheme
from django.views.decorators.csrf import ensure_csrf_cookie
from django.views.decorators.http import require_http_methods

from . import services
from .forms import LoginForm, SignupForm
from .services import UsernameAlreadyExistsError


def _safe_next_url(request: HttpRequest, default: str | None = None) -> str:
    # 로그인/회원가입 성공 후 기본 도착지는 이력서 대시보드다.
    default = default or reverse("resumes:dashboard")
    next_url = request.POST.get("next") or request.GET.get("next")
    if next_url and url_has_allowed_host_and_scheme(
        next_url, allowed_hosts={request.get_host()}, require_https=request.is_secure()
    ):
        return next_url
    return default


@require_http_methods(["GET", "POST"])
def signup_view(request: HttpRequest) -> HttpResponse:
    if request.method == "POST":
        form = SignupForm(request.POST)
        if form.is_valid():
            try:
                user = services.signup(
                    username=form.cleaned_data["username"],
                    email=form.cleaned_data["email"],
                    password=form.cleaned_data["password"],
                )
            except UsernameAlreadyExistsError as exc:
                form.add_error("username", str(exc))
            else:
                services.login(request, user)
                messages.success(request, "회원가입이 완료되었습니다.")
                return redirect(_safe_next_url(request))
    else:
        form = SignupForm()
    return render(request, "accounts/signup.html", {"form": form})


@require_http_methods(["GET", "POST"])
def login_view(request: HttpRequest) -> HttpResponse:
    if request.method == "POST":
        form = LoginForm(request.POST)
        if form.is_valid():
            user = services.authenticate(
                username=form.cleaned_data["username"],
                password=form.cleaned_data["password"],
            )
            if user is not None:
                services.login(request, user)
                return redirect(_safe_next_url(request))
            form.add_error(None, "아이디 또는 비밀번호가 올바르지 않습니다.")
    else:
        form = LoginForm()
    return render(request, "accounts/login.html", {"form": form, "next": request.GET.get("next", "")})


@require_http_methods(["POST"])
def logout_view(request: HttpRequest) -> HttpResponse:
    services.logout(request)
    messages.success(request, "로그아웃되었습니다.")
    return redirect("accounts:login")


@ensure_csrf_cookie
@require_http_methods(["GET"])
def csrf_token_view(request: HttpRequest) -> HttpResponse:
    """JSON API 클라이언트가 CSRF 토큰을 얻기 위한 엔드포인트.

    이 응답은 csrftoken 쿠키를 설정한다(아직 없었다면). 클라이언트는 이후
    /api/v1/... 상태 변경 요청(POST/PATCH/DELETE)에 이 쿠키 값을
    `X-CSRFToken` 헤더로 담아 보내야 한다.
    """
    return JsonResponse({"csrfToken": get_token(request)})
