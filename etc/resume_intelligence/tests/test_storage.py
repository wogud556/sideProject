import shutil
import tempfile
import threading
from pathlib import Path

from django.test import SimpleTestCase

from storage.json_store import read_json, update_json, write_json


class JsonStoreTests(SimpleTestCase):
    def setUp(self):
        self.tmp_dir = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, self.tmp_dir, ignore_errors=True)

    def test_read_json_missing_file_returns_default(self):
        path = self.tmp_dir / "missing.json"
        self.assertEqual(read_json(path, default={"a": 1}), {"a": 1})
        self.assertIsNone(read_json(path))

    def test_write_json_then_read_json_roundtrip(self):
        path = self.tmp_dir / "nested" / "data.json"
        write_json(path, {"hello": "세계", "n": 1})
        self.assertTrue(path.exists())
        self.assertEqual(read_json(path), {"hello": "세계", "n": 1})

    def test_write_json_creates_parent_directories(self):
        path = self.tmp_dir / "a" / "b" / "c.json"
        write_json(path, [1, 2, 3])
        self.assertTrue(path.parent.is_dir())
        self.assertEqual(read_json(path), [1, 2, 3])

    def test_write_json_is_atomic_no_leftover_tmp_files(self):
        path = self.tmp_dir / "atomic.json"
        write_json(path, {"x": 1})
        leftover = list(self.tmp_dir.glob(".*.tmp-*"))
        self.assertEqual(leftover, [])

    def test_update_json_mutates_existing_value(self):
        path = self.tmp_dir / "counter.json"
        write_json(path, {"count": 0})

        def increment(current):
            current = dict(current)
            current["count"] += 1
            return current

        result = update_json(path, increment, default={"count": 0})
        self.assertEqual(result, {"count": 1})
        self.assertEqual(read_json(path), {"count": 1})

    def test_update_json_uses_default_when_file_missing(self):
        path = self.tmp_dir / "new.json"

        def append_item(current):
            current = list(current)
            current.append("item")
            return current

        result = update_json(path, append_item, default=[])
        self.assertEqual(result, ["item"])

    def test_update_json_is_thread_safe_for_concurrent_increments(self):
        path = self.tmp_dir / "concurrent.json"
        write_json(path, {"count": 0})

        def increment(current):
            current = dict(current)
            current["count"] += 1
            return current

        def worker():
            for _ in range(20):
                update_json(path, increment, default={"count": 0})

        threads = [threading.Thread(target=worker) for _ in range(5)]
        for t in threads:
            t.start()
        for t in threads:
            t.join()

        self.assertEqual(read_json(path)["count"], 100)
