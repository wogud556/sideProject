from . import services


class CurrentUserMiddleware:
    """세션의 user_id로 조회한 사용자를 request.current_user에 주입한다.

    django.contrib.auth의 AuthenticationMiddleware(DB 조회)를 대체한다.
    """

    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        request.current_user = services.get_current_user(request)
        return self.get_response(request)
