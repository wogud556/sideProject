import shutil
import tempfile
from pathlib import Path
from unittest.mock import Mock

from django.http import JsonResponse
from django.test import Client, RequestFactory, SimpleTestCase, override_settings
from django.urls import reverse

from apps.accounts import repository, services
from apps.accounts.decorators import api_login_required, login_required
from apps.accounts.middleware import CurrentUserMiddleware
from apps.accounts.services import UsernameAlreadyExistsError


def _tmp_data_dir():
    return Path(tempfile.mkdtemp())


class AccountsTestBase(SimpleTestCase):
    def setUp(self):
        self.data_dir = _tmp_data_dir()
        self.addCleanup(shutil.rmtree, self.data_dir, ignore_errors=True)
        self.override = override_settings(DATA_DIR=self.data_dir)
        self.override.enable()
        self.addCleanup(self.override.disable)


class RepositoryTests(AccountsTestBase):
    def test_list_users_empty_when_no_file(self):
        self.assertEqual(repository.list_users(), [])

    def test_create_user_then_get_by_username_and_id(self):
        user = {"id": "u1", "username": "alice", "email": "a@example.com",
                "password_hash": "hash", "created_at": "2026-07-15T00:00:00"}
        repository.create_user(user)

        self.assertEqual(repository.get_user_by_username("alice"), user)
        self.assertEqual(repository.get_user_by_id("u1"), user)
        self.assertIsNone(repository.get_user_by_username("bob"))
        self.assertIsNone(repository.get_user_by_id("missing"))

    def test_create_user_appends_to_existing_list(self):
        repository.create_user({"id": "u1", "username": "alice", "email": "a@x.com",
                                 "password_hash": "h", "created_at": "t"})
        repository.create_user({"id": "u2", "username": "bob", "email": "b@x.com",
                                 "password_hash": "h", "created_at": "t"})
        self.assertEqual(len(repository.list_users()), 2)


class ServicesTests(AccountsTestBase):
    def test_signup_creates_user_with_hashed_password(self):
        user = services.signup("alice", "a@example.com", "password123")
        self.assertEqual(user["username"], "alice")
        self.assertNotEqual(user["password_hash"], "password123")
        self.assertIsNotNone(repository.get_user_by_username("alice"))

    def test_signup_duplicate_username_raises(self):
        services.signup("alice", "a@example.com", "password123")
        with self.assertRaises(UsernameAlreadyExistsError):
            services.signup("alice", "a2@example.com", "password456")

    def test_authenticate_success(self):
        services.signup("alice", "a@example.com", "password123")
        user = services.authenticate("alice", "password123")
        self.assertIsNotNone(user)
        self.assertEqual(user["username"], "alice")

    def test_authenticate_wrong_password_returns_none(self):
        services.signup("alice", "a@example.com", "password123")
        self.assertIsNone(services.authenticate("alice", "wrong"))

    def test_authenticate_unknown_username_returns_none(self):
        self.assertIsNone(services.authenticate("nobody", "password123"))

    def test_login_sets_session_and_get_current_user(self):
        user = services.signup("alice", "a@example.com", "password123")
        request = RequestFactory().get("/")
        request.session = {}
        services.login(request, user)
        self.assertEqual(request.session[services.SESSION_USER_ID_KEY], user["id"])

        current = services.get_current_user(request)
        self.assertEqual(current["username"], "alice")

    def test_logout_clears_session(self):
        user = services.signup("alice", "a@example.com", "password123")
        request = RequestFactory().get("/")
        request.session = {}
        services.login(request, user)
        services.logout(request)
        self.assertIsNone(services.get_current_user(request))

    def test_get_current_user_returns_none_when_not_logged_in(self):
        request = RequestFactory().get("/")
        request.session = {}
        self.assertIsNone(services.get_current_user(request))


class ConcurrentSignupTests(AccountsTestBase):
    """MED-3 회귀 테스트: 동일 username으로 동시에 signup()이 들어와도
    정확히 한 명만 생성되어야 한다(중복 검사+추가가 update_json 락 내부에서
    원자적으로 수행되는지 확인)."""

    def test_only_one_concurrent_signup_with_same_username_succeeds(self):
        import threading

        results: list[str] = []  # "ok" 또는 "duplicate"
        lock = threading.Lock()

        def attempt_signup(i):
            try:
                services.signup("racer", f"racer{i}@example.com", "password123")
                outcome = "ok"
            except UsernameAlreadyExistsError:
                outcome = "duplicate"
            with lock:
                results.append(outcome)

        threads = [threading.Thread(target=attempt_signup, args=(i,)) for i in range(20)]
        for t in threads:
            t.start()
        for t in threads:
            t.join()

        self.assertEqual(results.count("ok"), 1)
        self.assertEqual(results.count("duplicate"), 19)

        users = [u for u in repository.list_users() if u["username"] == "racer"]
        self.assertEqual(len(users), 1)


class MiddlewareTests(AccountsTestBase):
    def test_injects_current_user_when_logged_in(self):
        user = services.signup("alice", "a@example.com", "password123")
        request = RequestFactory().get("/")
        request.session = {}
        services.login(request, user)

        get_response = Mock(return_value="response")
        middleware = CurrentUserMiddleware(get_response)
        middleware(request)

        self.assertEqual(request.current_user["username"], "alice")

    def test_injects_none_when_not_logged_in(self):
        request = RequestFactory().get("/")
        request.session = {}

        get_response = Mock(return_value="response")
        middleware = CurrentUserMiddleware(get_response)
        middleware(request)

        self.assertIsNone(request.current_user)


class DecoratorsTests(AccountsTestBase):
    def test_login_required_redirects_when_anonymous(self):
        request = RequestFactory().get("/protected/")
        request.current_user = None

        view = login_required(lambda req: "ok")
        response = view(request)

        self.assertEqual(response.status_code, 302)
        self.assertIn(reverse("accounts:login"), response.url)

    def test_login_required_calls_view_when_logged_in(self):
        request = RequestFactory().get("/protected/")
        request.current_user = {"id": "u1", "username": "alice"}

        view = login_required(lambda req: "ok")
        response = view(request)

        self.assertEqual(response, "ok")

    def test_api_login_required_returns_401_when_anonymous(self):
        request = RequestFactory().get("/api/protected/")
        request.current_user = None

        view = api_login_required(lambda req: JsonResponse({"ok": True}))
        response = view(request)

        self.assertIsInstance(response, JsonResponse)
        self.assertEqual(response.status_code, 401)

    def test_api_login_required_calls_view_when_logged_in(self):
        request = RequestFactory().get("/api/protected/")
        request.current_user = {"id": "u1", "username": "alice"}

        view = api_login_required(lambda req: JsonResponse({"ok": True}))
        response = view(request)

        self.assertEqual(response.status_code, 200)


class ViewsTests(AccountsTestBase):
    def setUp(self):
        super().setUp()
        self.client = Client()

    def test_signup_view_get_renders_form(self):
        response = self.client.get(reverse("accounts:signup"))
        self.assertEqual(response.status_code, 200)

    def test_signup_view_post_creates_user_and_logs_in(self):
        response = self.client.post(reverse("accounts:signup"), {
            "username": "alice",
            "email": "a@example.com",
            "password": "password123",
            "password_confirm": "password123",
        })
        self.assertEqual(response.status_code, 302)
        self.assertIsNotNone(repository.get_user_by_username("alice"))

    def test_signup_view_post_password_mismatch_shows_error(self):
        response = self.client.post(reverse("accounts:signup"), {
            "username": "alice",
            "email": "a@example.com",
            "password": "password123",
            "password_confirm": "different",
        })
        self.assertEqual(response.status_code, 200)
        self.assertIsNone(repository.get_user_by_username("alice"))

    def test_signup_view_post_duplicate_username_shows_error(self):
        services.signup("alice", "a@example.com", "password123")
        response = self.client.post(reverse("accounts:signup"), {
            "username": "alice",
            "email": "a2@example.com",
            "password": "password456",
            "password_confirm": "password456",
        })
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "이미 사용 중인 아이디입니다", status_code=200)

    def test_login_view_get_renders_form(self):
        response = self.client.get(reverse("accounts:login"))
        self.assertEqual(response.status_code, 200)

    def test_login_view_post_success_redirects(self):
        services.signup("alice", "a@example.com", "password123")
        response = self.client.post(reverse("accounts:login"), {
            "username": "alice",
            "password": "password123",
        })
        self.assertEqual(response.status_code, 302)

    def test_login_view_post_invalid_credentials_shows_error(self):
        services.signup("alice", "a@example.com", "password123")
        response = self.client.post(reverse("accounts:login"), {
            "username": "alice",
            "password": "wrong",
        })
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, "아이디 또는 비밀번호가 올바르지 않습니다")

    def test_logout_view_clears_session(self):
        services.signup("alice", "a@example.com", "password123")
        self.client.post(reverse("accounts:login"), {
            "username": "alice",
            "password": "password123",
        })
        response = self.client.post(reverse("accounts:logout"))
        self.assertEqual(response.status_code, 302)

    def test_logout_view_rejects_get(self):
        response = self.client.get(reverse("accounts:logout"))
        self.assertEqual(response.status_code, 405)
