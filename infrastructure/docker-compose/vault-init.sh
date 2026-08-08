#!/bin/sh
# =============================================================================
# Vault Init Script — enables Transit engine and creates all required keys.
# Runs once after Vault (dev mode) is healthy. Safe to re-run (idempotent).
# =============================================================================
set -e

export VAULT_ADDR="${VAULT_ADDR:-http://vault:8200}"
export VAULT_TOKEN="${VAULT_TOKEN:-vault_root_token}"

echo "⏳  Waiting for Vault to be ready..."
until vault status -address="$VAULT_ADDR" > /dev/null 2>&1; do
  sleep 1
done
echo "✅  Vault is ready."

# ---------------------------------------------------------------------------
# Enable Transit secrets engine (idempotent)
# ---------------------------------------------------------------------------
if vault secrets list -address="$VAULT_ADDR" | grep -q "^transit/"; then
  echo "ℹ️   Transit engine already enabled."
else
  vault secrets enable -address="$VAULT_ADDR" transit
  echo "✅  Transit engine enabled."
fi

# ---------------------------------------------------------------------------
# Helper: create key only if it doesn't already exist
# ---------------------------------------------------------------------------
create_key() {
  KEY_NAME="$1"
  KEY_TYPE="${2:-aes256-gcm96}"
  if vault read -address="$VAULT_ADDR" "transit/keys/$KEY_NAME" > /dev/null 2>&1; then
    echo "ℹ️   Key '$KEY_NAME' already exists."
  else
    vault write -address="$VAULT_ADDR" "transit/keys/$KEY_NAME" type="$KEY_TYPE"
    echo "✅  Created key: $KEY_NAME (type: $KEY_TYPE)"
  fi
}

# ---------------------------------------------------------------------------
# Create all required transit keys
# ---------------------------------------------------------------------------
create_key "question-content-key"          "aes256-gcm96"
create_key "candidate-pii-key"             "aes256-gcm96"
create_key "translation-content-key"       "aes256-gcm96"
create_key "audit-signing-key-ecdsa-p256"  "ecdsa-p256"

echo ""
echo "============================================="
echo "  ✅  Vault init complete — all keys ready"
echo "============================================="
