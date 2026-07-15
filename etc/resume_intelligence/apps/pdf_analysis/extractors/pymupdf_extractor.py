"""PyMuPDF(fitz) 기반 페이지별 텍스트 블록 추출기."""
import fitz

from ..schemas.document import ExtractedBlock, ExtractedDocument

# fitz block_type: 0 = 텍스트, 1 = 이미지
TEXT_BLOCK_TYPE = 0


class PyMuPdfExtractor:
    def extract(self, file_path: str) -> ExtractedDocument:
        doc = fitz.open(str(file_path))
        try:
            blocks: list[ExtractedBlock] = []
            for page_index in range(doc.page_count):
                page = doc[page_index]
                order = 0
                for raw_block in page.get_text("blocks"):
                    x0, y0, x1, y1, text, _block_no, block_type = raw_block[:7]
                    if block_type != TEXT_BLOCK_TYPE:
                        continue
                    text = text.strip()
                    if not text:
                        continue
                    blocks.append(
                        ExtractedBlock(
                            page=page_index + 1,
                            order=order,
                            text=text,
                            bbox=(x0, y0, x1, y1),
                            method="text",
                        )
                    )
                    order += 1
            page_count = doc.page_count
        finally:
            doc.close()

        return ExtractedDocument(
            file_path=str(file_path),
            page_count=page_count,
            blocks=blocks,
            ocr_used=False,
            warnings=[],
        )
