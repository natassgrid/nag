#!/bin/bash
# ============================================================================
# WSL Development Environment Setup Script
# Open Source Government Examination Platform
# Run this in your WSL terminal: bash scripts/setup-wsl-dev.sh
# ============================================================================

set -e

echo "🚀 Setting up WSL development environment..."

# ─── 1. System packages ─────────────────────────────────────────────────────
echo "📦 Installing system packages..."
sudo apt-get update -y
sudo apt-get install -y \
  openjdk-21-jdk \
  curl \
  unzip \
  git \
  build-essential \
  apt-transport-https \
  ca-certificates \
  gnupg

# ─── 2. Set JAVA_HOME ───────────────────────────────────────────────────────
echo "☕ Configuring Java 21..."
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
echo 'export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64' >> ~/.bashrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc

# ─── 3. Node.js 22 LTS via NodeSource ───────────────────────────────────────
echo "🟢 Installing Node.js 22 LTS..."
curl -fsSL https://deb.nodesource.com/setup_22.x | sudo -E bash -
sudo apt-get install -y nodejs

# ─── 4. Gradle (via wrapper — no global install needed) ─────────────────────
echo "🐘 Generating Gradle wrapper..."
# The project already has a wrapper task defined in build.gradle
# Just need to make gradlew executable if it exists
if [ -f "./gradlew" ]; then
  chmod +x ./gradlew
  echo "   gradlew found and made executable"
else
  echo "   No gradlew found — will generate with: gradle wrapper"
  # Install Gradle temporarily to generate wrapper
  curl -sL https://services.gradle.org/distributions/gradle-8.10.2-bin.zip -o /tmp/gradle.zip
  sudo unzip -q /tmp/gradle.zip -d /opt/
  sudo ln -sf /opt/gradle-8.10.2/bin/gradle /usr/local/bin/gradle
  gradle wrapper --gradle-version 8.10.2
  chmod +x ./gradlew
  rm /tmp/gradle.zip
fi

# ─── 5. Docker (use Docker Desktop WSL integration) ─────────────────────────
echo "🐳 Docker setup..."
echo "   → Enable 'WSL Integration' in Docker Desktop Settings > Resources > WSL Integration"
echo "   → Select your Ubuntu distro and restart Docker Desktop"

# ─── 6. Verify installations ────────────────────────────────────────────────
echo ""
echo "✅ Verification:"
echo "   Java:    $(java --version 2>&1 | head -1)"
echo "   Node:    $(node --version)"
echo "   npm:     $(npm --version)"
echo "   Git:     $(git --version)"
if command -v docker &> /dev/null; then
  echo "   Docker:  $(docker --version)"
else
  echo "   Docker:  Not available yet (enable WSL integration in Docker Desktop)"
fi

# ─── 7. Install frontend dependencies ───────────────────────────────────────
echo ""
echo "📦 Installing frontend dependencies..."
if [ -d "frontend" ]; then
  (cd frontend && rm -rf node_modules package-lock.json && npm install)
  echo "   ✅ Frontend dependencies installed"
fi

# ─── 8. Test Gradle build ───────────────────────────────────────────────────
echo ""
echo "🏗️  Testing Gradle build (compile only)..."
./gradlew :backend:shared-lib:compileJava --no-daemon -q && echo "   ✅ Gradle build works"

echo ""
echo "🎉 Done! Development environment is ready."
echo "   Run './gradlew build -x test' to compile all services"
echo "   Run 'cd frontend && npm start' to start Angular dev server"
