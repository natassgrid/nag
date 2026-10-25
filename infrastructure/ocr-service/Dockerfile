# =============================================================================
# Tesseract OCR Service with English and Hindi Support
# CPU-only microservice for question paper scanned page text extraction
# Port: 5005
# =============================================================================

FROM python:3.11-slim

WORKDIR /app

# Install system dependencies & Tesseract OCR packages
RUN apt-get update && apt-get install -y --no-install-recommends \
    tesseract-ocr \
    tesseract-ocr-eng \
    tesseract-ocr-hin \
    curl \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app.py .

EXPOSE 5005

HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=3 \
    CMD curl -sf http://localhost:5005/health || exit 1

CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "5005"]
