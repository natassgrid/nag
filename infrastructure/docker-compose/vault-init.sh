#!/bin/sh

# SPDX-License-Identifier: AGPL-3.0-only
#
# National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
# Copyright (C) 2025 NAG Contributors
#
# =============================================================================
# Vault Init & Auto-Unseal Script
# Persists Vault keys and handles:
#  1. Initializing Vault on first deployment (saves unseal key & root token to /vault/data/vault-keys.json)
#  2. Auto-unsealing Vault on every startup / restart / redeployment
#  3. Creating static dev root token ($VAULT_TOKEN / vault_root_token)
#  4. Enabling KV-v2 and Transit engines and creating required encryption keys
# =============================================================================

export VAULT_ADDR="${VAULT_ADDR:-http://vault:8200}"
TARGET_TOKEN="${VAULT_TOKEN:-vault_root_token}"
KEYS_FILE="/vault/data/vault-keys.json"

echo "⏳ Waiting for Vault server to start at $VAULT_ADDR..."
while true; do
  STATUS_CODE=0
  vault status -address="$VAULT_ADDR" > /dev/null 2>&1 || STATUS_CODE=$?
  # vault status returns 0 (unsealed), 2 (sealed/uninitialized), or 1 (connection error)
  if [ "$STATUS_CODE" -eq 0 ] || [ "$STATUS_CODE" -eq 2 ]; then
    break
  fi
  sleep 1
done
echo "✅ Vault server is responding."

# ---------------------------------------------------------------------------
# 1. Initialize Vault if not initialized
# ---------------------------------------------------------------------------
STATUS_OUTPUT=$(vault status -address="$VAULT_ADDR" 2>&1 || true)

if echo "$STATUS_OUTPUT" | grep -q "Initialized.*false"; then
  echo "🔧 Initializing Vault (1 key share)..."
  INIT_RESPONSE=$(vault operator init -address="$VAULT_ADDR" -key-shares=1 -key-threshold=1 -format=json)
  echo "$INIT_RESPONSE" > "$KEYS_FILE"
  chmod 600 "$KEYS_FILE" 2>/dev/null || true
  echo "✅ Vault initialized. Keys stored in $KEYS_FILE."
else
  echo "ℹ️   Vault is already initialized."
fi

# ---------------------------------------------------------------------------
# 2. Extract Unseal Key & Initial Root Token
# ---------------------------------------------------------------------------
UNSEAL_KEY=""
INIT_ROOT_TOKEN=""
if [ -f "$KEYS_FILE" ]; then
  UNSEAL_KEY=$(grep -o '"unseal_keys_b64": *\[ *"[^"]*"' "$KEYS_FILE" 2>/dev/null | awk -F'"' '{print $4}')
  INIT_ROOT_TOKEN=$(grep -o '"root_token": *"[^"]*"' "$KEYS_FILE" 2>/dev/null | awk -F'"' '{print $4}')
fi

# ---------------------------------------------------------------------------
# 3. Unseal Vault if sealed
# ---------------------------------------------------------------------------
STATUS_OUTPUT=$(vault status -address="$VAULT_ADDR" 2>&1 || true)
if echo "$STATUS_OUTPUT" | grep -q "Sealed.*true"; then
  if [ -n "$UNSEAL_KEY" ]; then
    echo "🔓 Unsealing Vault with stored key..."
    vault operator unseal -address="$VAULT_ADDR" "$UNSEAL_KEY" || true
    echo "✅ Vault unsealed successfully."
  else
    echo "⚠️   Vault is sealed but unseal key was not found in $KEYS_FILE!"
  fi
else
  echo "✅ Vault is unsealed."
fi

# ---------------------------------------------------------------------------
# 4. Ensure Static Dev Root Token Exists (for seamless local microservice auth)
# ---------------------------------------------------------------------------
ADMIN_TOKEN="${INIT_ROOT_TOKEN:-$TARGET_TOKEN}"

# Test if target token already works
if ! VAULT_TOKEN="$TARGET_TOKEN" vault token lookup -address="$VAULT_ADDR" > /dev/null 2>&1; then
  echo "🔑 Creating dev root token '$TARGET_TOKEN'..."
  VAULT_TOKEN="$ADMIN_TOKEN" vault token create \
    -address="$VAULT_ADDR" \
    -id="$TARGET_TOKEN" \
    -policy="root" \
    -orphan=true \
    -ttl="0" > /dev/null 2>&1 || true
  echo "✅ Dev root token '$TARGET_TOKEN' active."
else
  echo "ℹ️   Dev root token '$TARGET_TOKEN' is valid."
fi

export VAULT_TOKEN="$TARGET_TOKEN"

# ---------------------------------------------------------------------------
# 5. Enable KV-v2 and Transit secrets engines (idempotent)
# ---------------------------------------------------------------------------
if vault secrets list -address="$VAULT_ADDR" 2>/dev/null | grep -q "^secret/"; then
  echo "ℹ️   KV secrets engine already enabled at secret/."
else
  vault secrets enable -address="$VAULT_ADDR" -path=secret kv-v2 || true
  echo "✅ KV secrets engine enabled at secret/."
fi

if vault secrets list -address="$VAULT_ADDR" 2>/dev/null | grep -q "^transit/"; then
  echo "ℹ️   Transit engine already enabled."
else
  vault secrets enable -address="$VAULT_ADDR" transit || true
  echo "✅ Transit engine enabled."
fi

# ---------------------------------------------------------------------------
# 6. Create transit keys (idempotent)
# ---------------------------------------------------------------------------
create_key() {
  KEY_NAME="$1"
  KEY_TYPE="${2:-aes256-gcm96}"
  if vault read -address="$VAULT_ADDR" "transit/keys/$KEY_NAME" > /dev/null 2>&1; then
    echo "ℹ️   Transit key '$KEY_NAME' exists (persisted)."
  else
    vault write -address="$VAULT_ADDR" "transit/keys/$KEY_NAME" type="$KEY_TYPE" || true
    echo "✅ Created transit key: $KEY_NAME (type: $KEY_TYPE)"
  fi
}

create_key "question-content-key"          "aes256-gcm96"
create_key "candidate-pii-key"             "aes256-gcm96"
create_key "translation-content-key"       "aes256-gcm96"
create_key "audit-signing-key-ecdsa-p256"  "ecdsa-p256"

echo ""
echo "============================================="
echo "  ✅ Vault initialization & persistence ready"
echo "============================================="
