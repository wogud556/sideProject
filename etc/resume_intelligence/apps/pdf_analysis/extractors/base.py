from typing import Protocol

from ..schemas.document import ExtractedDocument


class PdfExtractor(Protocol):
    def extract(self, file_path: str) -> ExtractedDocument: ...
