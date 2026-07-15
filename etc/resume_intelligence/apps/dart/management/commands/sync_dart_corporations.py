from django.core.management.base import BaseCommand, CommandError

from apps.dart import exceptions
from apps.dart.services import sync_service


class Command(BaseCommand):
    help = "DART 고유번호(corpCode.xml) 목록을 내려받아 data/dart/corporations.json에 저장한다."

    def handle(self, *args, **options):
        try:
            count = sync_service.sync_corporations()
        except exceptions.DartApiKeyMissing as exc:
            raise CommandError(f"DART_API_KEY가 설정되지 않았습니다: {exc}") from exc
        except exceptions.DartError as exc:
            raise CommandError(f"DART 동기화 중 오류가 발생했습니다: {exc}") from exc

        self.stdout.write(self.style.SUCCESS(f"{count}개 기업 정보를 동기화했습니다."))
