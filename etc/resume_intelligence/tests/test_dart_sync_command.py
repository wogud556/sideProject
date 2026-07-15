import shutil
import tempfile
from io import StringIO
from pathlib import Path
from unittest.mock import patch

from django.core.management import call_command
from django.core.management.base import CommandError
from django.test import SimpleTestCase, override_settings

from apps.dart import exceptions, repository

from .helpers import build_corp_code_zip_bytes


class SyncDartCorporationsCommandTests(SimpleTestCase):
    def setUp(self):
        self.data_dir = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.data_dir, ignore_errors=True)
        self.override = override_settings(DATA_DIR=self.data_dir, DART_API_KEY="test-key")
        self.override.enable()
        self.addCleanup(self.override.disable)

    def test_success_writes_corporations_json(self):
        response_mock = _fake_response(build_corp_code_zip_bytes())
        with patch("apps.dart.clients.open_dart_client.requests.get", return_value=response_mock):
            out = StringIO()
            call_command("sync_dart_corporations", stdout=out)

        self.assertIn("4개 기업", out.getvalue())
        stored = repository.read_corporations()
        self.assertEqual(len(stored), 4)

    def test_missing_api_key_raises_command_error(self):
        with override_settings(DART_API_KEY=""):
            with self.assertRaises(CommandError):
                call_command("sync_dart_corporations", stdout=StringIO())

    def test_dart_error_raises_command_error(self):
        with patch(
            "apps.dart.services.sync_service.sync_corporations",
            side_effect=exceptions.DartTimeout("시간 초과"),
        ):
            with self.assertRaises(CommandError):
                call_command("sync_dart_corporations", stdout=StringIO())


def _fake_response(content: bytes):
    from unittest.mock import Mock

    response = Mock()
    response.content = content
    response.headers = {"Content-Type": "application/x-msdownload"}
    return response
