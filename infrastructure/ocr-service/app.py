"""
SPDX-License-Identifier: AGPL-3.0-only
Open Digital Public Infrastructure (DPI) Platform
Tesseract Fast OCR Service for Exam Question Papers (English + Hindi)
"""

import base64
import io
import logging
from typing import Optional
from fastapi import FastAPI, File, UploadFile, Query, HTTPException
from pydantic import BaseModel
from PIL import Image
import pytesseract

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
logger = logging.getLogger("ocr-service")

app = FastAPI(title="Exam Platform OCR Service", version="1.0.0")

# Verify available tesseract languages
AVAILABLE_LANGS = []
try:
    AVAILABLE_LANGS = pytesseract.get_languages(config="")
    logger.info(f"Available Tesseract languages: {AVAILABLE_LANGS}")
except Exception as e:
    logger.warning(f"Could not retrieve Tesseract languages: {e}")
    AVAILABLE_LANGS = ["eng"]

def get_effective_lang(requested_lang: str) -> str:
    """Selects available language pack or falls back to 'eng'."""
    if "hin" in requested_lang and "hin" in AVAILABLE_LANGS:
        return "eng+hin"
    return "eng"

class OcrRequest(BaseModel):
    image_base64: Optional[str] = None
    image_data_uri: Optional[str] = None
    lang: Optional[str] = "eng+hin"

class OcrResponse(BaseModel):
    status: str
    text: str
    length: int
    language: str

@app.get("/health")
def health():
    return {
        "status": "healthy",
        "service": "exam-ocr-service",
        "languages": AVAILABLE_LANGS
    }

def run_tesseract(image: Image.Image, lang: str) -> str:
    # Ensure RGB
    if image.mode != "RGB":
        image = image.convert("RGB")
    
    # Run tesseract with page segmentation mode 3 (fully automatic page segmentation)
    effective_lang = get_effective_lang(lang)
    text = pytesseract.image_to_string(image, lang=effective_lang, config="--psm 3")
    return text.strip()

@app.post("/ocr", response_model=OcrResponse)
async def ocr_json_endpoint(payload: OcrRequest):
    try:
        raw_b64 = None
        if payload.image_data_uri and "," in payload.image_data_uri:
            raw_b64 = payload.image_data_uri.split(",", 1)[1]
        elif payload.image_base64:
            raw_b64 = payload.image_base64

        if not raw_b64:
            raise HTTPException(status_code=400, detail="No image provided via image_data_uri or image_base64")

        decoded = base64.b64decode(raw_b64.strip())
        image = Image.open(io.BytesIO(decoded))

        requested_lang = payload.lang or "eng+hin"
        extracted_text = run_tesseract(image, requested_lang)
        logger.info(f"OCR extracted {len(extracted_text)} characters using lang '{get_effective_lang(requested_lang)}'")

        return OcrResponse(
            status="success",
            text=extracted_text,
            length=len(extracted_text),
            language=get_effective_lang(requested_lang)
        )
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"OCR processing failed: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"OCR extraction failed: {str(e)}")

@app.post("/ocr/upload", response_model=OcrResponse)
async def ocr_upload_endpoint(
    file: UploadFile = File(...),
    lang: str = Query("eng+hin")
):
    try:
        contents = await file.read()
        image = Image.open(io.BytesIO(contents))

        extracted_text = run_tesseract(image, lang)
        logger.info(f"OCR extracted {len(extracted_text)} characters from upload using lang '{get_effective_lang(lang)}'")

        return OcrResponse(
            status="success",
            text=extracted_text,
            length=len(extracted_text),
            language=get_effective_lang(lang)
        )
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"OCR upload processing failed: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"OCR upload extraction failed: {str(e)}")
