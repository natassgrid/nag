# =============================================================================
# IndicTrans2 — English to 22 Indian Languages Translation Service
# Model: ai4bharat/indictrans2-en-indic-dist-200M (~850MB)
# CPU-only build for local development
# Model downloads on first start (not during build)
# Serves a REST API at port 7860
# =============================================================================

FROM python:3.10-slim

WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    git \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# Install PyTorch CPU + dependencies
RUN pip install --no-cache-dir torch --extra-index-url https://download.pytorch.org/whl/cpu || \
    pip install --no-cache-dir torch

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application
COPY app.py .

# Model will be downloaded to this volume on first start
VOLUME /root/.cache/huggingface

EXPOSE 7860

HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=5 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:7860/health')" || exit 1

CMD ["python", "app.py"]
