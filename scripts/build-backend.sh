#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────────────────────────
# Build script for Java/Gradle backend (WSL)
# Usage: ./scripts/build-backend.sh [--skip-tests] [--service <name>]
# ──────────────────────────────────────────────────────────────────────────────
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# ── Defaults ──────────────────────────────────────────────────────────────────
SKIP_TESTS=false
SERVICE=""
CLEAN=false
PARALLEL=true

# ── Parse arguments ───────────────────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
    case "$1" in
        --skip-tests|-x)
            SKIP_TESTS=true
            shift
            ;;
        --service|-s)
            SERVICE="$2"
            shift 2
            ;;
        --clean|-c)
            CLEAN=true
            shift
            ;;
        --no-parallel)
            PARALLEL=false
            shift
            ;;
        --help|-h)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --skip-tests, -x     Skip running tests"
            echo "  --service, -s NAME   Build a single service (e.g., question-bank-service)"
            echo "  --clean, -c          Run clean before build"
            echo "  --no-parallel        Disable parallel execution"
            echo "  --help, -h           Show this help message"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

# ── Prerequisites check ──────────────────────────────────────────────────────
echo "═══════════════════════════════════════════════════════════════════"
echo " Exam Platform — Backend Build (Gradle)"
echo "═══════════════════════════════════════════════════════════════════"

# Check Java
if ! command -v java &>/dev/null; then
    echo "ERROR: Java not found. Install Java 21 (Temurin):"
    echo "  sudo apt install temurin-21-jdk"
    echo "  -- or --"
    echo "  curl -s https://get.sdkman.io | bash && sdk install java 21.0.4-tem"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -1 | awk -F '"' '{print $2}' | cut -d. -f1)
if [[ "$JAVA_VERSION" -lt 21 ]]; then
    echo "ERROR: Java 21+ required, found Java $JAVA_VERSION"
    echo "  Use sdkman: sdk install java 21.0.4-tem"
    exit 1
fi
echo "✓ Java $(java -version 2>&1 | head -1 | awk -F '"' '{print $2}')"

# ── Build command assembly ────────────────────────────────────────────────────
cd "$PROJECT_ROOT"

GRADLE_CMD="./gradlew"
GRADLE_ARGS=()

if [[ "$CLEAN" == true ]]; then
    GRADLE_ARGS+=("clean")
fi

if [[ -n "$SERVICE" ]]; then
    GRADLE_ARGS+=(":backend:${SERVICE}:build")
else
    GRADLE_ARGS+=("build")
fi

if [[ "$SKIP_TESTS" == true ]]; then
    GRADLE_ARGS+=("-x" "test")
fi

if [[ "$PARALLEL" == true ]]; then
    GRADLE_ARGS+=("--parallel")
fi

# Add common Gradle optimizations
GRADLE_ARGS+=("--build-cache" "--configuration-cache")

echo ""
echo "► Running: $GRADLE_CMD ${GRADLE_ARGS[*]}"
echo "───────────────────────────────────────────────────────────────────"

$GRADLE_CMD "${GRADLE_ARGS[@]}"

echo ""
echo "═══════════════════════════════════════════════════════════════════"
echo " ✓ Backend build completed successfully"
echo "═══════════════════════════════════════════════════════════════════"
