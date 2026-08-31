"""
IndicTrans2 Translation Service
Model: ai4bharat/indictrans2-en-indic-dist-200M
Translates English to 22 Indian languages.

API:
  POST /translate
    Body: {"text": "...", "target_lang": "hin_Deva"}
    Returns: {"translated_text": "...", "source_lang": "eng_Latn", "target_lang": "hin_Deva"}

  POST /translate/batch
    Body: {"texts": ["...", "..."], "target_lang": "hin_Deva"}
    Returns: {"translations": [...], "source_lang": "eng_Latn", "target_lang": "hin_Deva"}

  GET /languages
    Returns list of supported target languages

  GET /health
    Returns health status
"""

import logging
from contextlib import asynccontextmanager
from typing import Optional

import torch
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field
from transformers import AutoModelForSeq2SeqLM, AutoTokenizer
from IndicTransToolkit.processor import IndicProcessor

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

MODEL_NAME = "ai4bharat/indictrans2-en-indic-dist-200M"
SOURCE_LANG = "eng_Latn"

SUPPORTED_LANGUAGES = {
    "asm_Beng": "Assamese",
    "ben_Beng": "Bengali",
    "brx_Deva": "Bodo",
    "doi_Deva": "Dogri",
    "gom_Deva": "Konkani",
    "guj_Gujr": "Gujarati",
    "hin_Deva": "Hindi",
    "kan_Knda": "Kannada",
    "kas_Arab": "Kashmiri (Arabic)",
    "kas_Deva": "Kashmiri (Devanagari)",
    "mai_Deva": "Maithili",
    "mal_Mlym": "Malayalam",
    "mni_Beng": "Manipuri (Bengali)",
    "mni_Mtei": "Manipuri (Meitei)",
    "mar_Deva": "Marathi",
    "npi_Deva": "Nepali",
    "ory_Orya": "Odia",
    "pan_Guru": "Punjabi",
    "san_Deva": "Sanskrit",
    "sat_Olck": "Santali",
    "snd_Arab": "Sindhi",
    "tam_Taml": "Tamil",
    "tel_Telu": "Telugu",
    "urd_Arab": "Urdu",
}

# Global model references
tokenizer = None
model = None
ip = None
DEVICE = "cpu"


@asynccontextmanager
async def lifespan(app: FastAPI):
    global tokenizer, model, ip, DEVICE
    logger.info(f"Loading model: {MODEL_NAME}")

    DEVICE = "cuda" if torch.cuda.is_available() else "cpu"

    tokenizer = AutoTokenizer.from_pretrained(MODEL_NAME, trust_remote_code=True)
    model = AutoModelForSeq2SeqLM.from_pretrained(MODEL_NAME, trust_remote_code=True)
    model = model.to(DEVICE)
    model.eval()

    ip = IndicProcessor(inference=True)

    logger.info(f"Model loaded on {DEVICE}. Ready.")
    yield
    logger.info("Shutting down.")


app = FastAPI(
    title="IndicTrans2 Translation Service",
    description="English to 22 Indian Languages (ai4bharat/indictrans2-en-indic-dist-200M)",
    version="1.0.0",
    lifespan=lifespan,
)


class TranslateRequest(BaseModel):
    text: str = Field(..., min_length=1, max_length=5000, description="English text to translate")
    target_lang: str = Field(..., description="Target language code (e.g., hin_Deva, tam_Taml)")


class TranslateResponse(BaseModel):
    translated_text: str
    source_lang: str = SOURCE_LANG
    target_lang: str


class BatchTranslateRequest(BaseModel):
    texts: list[str] = Field(..., min_length=1, max_length=50, description="List of English texts (max 50)")
    target_lang: str = Field(..., description="Target language code")


class BatchTranslateResponse(BaseModel):
    translations: list[str]
    source_lang: str = SOURCE_LANG
    target_lang: str
    count: int


def translate_texts(texts: list[str], target_lang: str) -> list[str]:
    """Translate a batch of texts from English to target Indian language."""
    # Preprocess using IndicProcessor
    batch = ip.preprocess_batch(texts, src_lang=SOURCE_LANG, tgt_lang=target_lang)

    # Tokenize
    inputs = tokenizer(
        batch,
        return_tensors="pt",
        padding=True,
        truncation=True,
        max_length=256,
    ).to(DEVICE)

    # Generate
    with torch.no_grad():
        generated = model.generate(
            **inputs,
            max_length=256,
            num_beams=5,
            num_return_sequences=1,
            early_stopping=True,
        )

    # Decode
    with tokenizer.as_target_tokenizer():
        raw_translations = tokenizer.batch_decode(generated, skip_special_tokens=True)

    # Postprocess using IndicProcessor
    translations = ip.postprocess_batch(raw_translations, lang=target_lang)

    return translations


@app.get("/health")
async def health():
    return {"status": "healthy", "model": MODEL_NAME, "device": DEVICE}


@app.get("/languages")
async def languages():
    return {"source": SOURCE_LANG, "targets": SUPPORTED_LANGUAGES}


@app.post("/translate", response_model=TranslateResponse)
async def translate(request: TranslateRequest):
    if request.target_lang not in SUPPORTED_LANGUAGES:
        raise HTTPException(
            status_code=400,
            detail=f"Unsupported target language: {request.target_lang}. Supported: {list(SUPPORTED_LANGUAGES.keys())}",
        )

    try:
        translations = translate_texts([request.text], request.target_lang)
        return TranslateResponse(
            translated_text=translations[0],
            target_lang=request.target_lang,
        )
    except Exception as e:
        logger.error(f"Translation error: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"Translation failed: {str(e)}")


@app.post("/translate/batch", response_model=BatchTranslateResponse)
async def translate_batch(request: BatchTranslateRequest):
    if request.target_lang not in SUPPORTED_LANGUAGES:
        raise HTTPException(
            status_code=400,
            detail=f"Unsupported target language: {request.target_lang}. Supported: {list(SUPPORTED_LANGUAGES.keys())}",
        )

    try:
        translations = translate_texts(request.texts, request.target_lang)
        return BatchTranslateResponse(
            translations=translations,
            target_lang=request.target_lang,
            count=len(translations),
        )
    except Exception as e:
        logger.error(f"Batch translation error: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"Translation failed: {str(e)}")


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=7860)
