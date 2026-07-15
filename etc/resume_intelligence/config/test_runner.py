"""DB를 전혀 사용하지 않는 프로젝트를 위한 테스트 러너.

DATABASES = {} 이므로 기본적으로 setup_databases가 필요하지 않지만, 실수로
django.test.TestCase(트랜잭션 래핑을 위해 DB 접근을 시도)를 사용하는 경우를
대비한 안전망으로 setup/teardown을 no-op으로 오버라이드한다.
"""
from django.test.runner import DiscoverRunner


class NoDatabaseTestRunner(DiscoverRunner):
    def setup_databases(self, **kwargs):
        return []

    def teardown_databases(self, old_config, **kwargs):
        pass
