# =============================================================================
# IndicTrans2 — English to 22 Indian Languages Translation Service
# Model: ai4bharat/indictrans2-en-indic-dist-200M (~850MB)
# Serves a REST API at port 7860
# =============================================================================

FROM python:3.10-slim

WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    git \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# Install Python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application
COPY app.py .

# Download model at build time (cached in Docker layer)
# Uses HF_TOKEN if model is gated; otherwise public access
ARG HF_TOKEN=""
ENV HF_TOKEN=${HF_TOKEN}
RUN python -c "\
from transformers import AutoModelForSeq2SeqLM, AutoTokenizer; \
from IndicTransToolkit.processor import IndicProcessor; \
tokenizer = AutoTokenizer.from_pretrained('ai4bharat/indictrans2-en-indic-dist-200M', trust_remote_code=True); \
model = AutoModelForSeq2SeqLM.from_pretrained('ai4bharat/indictrans2-en-indic-dist-200M', trust_remote_code=True); \
ip = IndicProcessor(inference=True); \
print('Model downloaded successfully')"

EXPOSE 7860

HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:7860/health')" || exit 1

CMD ["python", "app.py"]
