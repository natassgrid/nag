#!/bin/bash

# SPDX-License-Identifier: AGPL-3.0-only
#
# National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
# Copyright (C) 2025 NAG Contributors
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as published
# by the Free Software Foundation, version 3 of the License.

# =============================================================================
# Kafka Topic Creation Script
# Open Digital Public Infrastructure (DPI) Platform
# Requirements: 20.3
#
# Creates all required topics with appropriate partition counts and
# replication factor 3 for production durability.
# =============================================================================

set -euo pipefail

BOOTSTRAP_SERVER="${KAFKA_BOOTSTRAP_SERVERS:-kafka-1:9092}"
REPLICATION_FACTOR="${KAFKA_REPLICATION_FACTOR:-3}"

echo "=== Creating Kafka Topics (bootstrap: $BOOTSTRAP_SERVER, RF: $REPLICATION_FACTOR) ==="

create_topic() {
  local topic=$1
  local partitions=$2
  local config=${3:-""}

  echo "Creating topic: $topic (partitions=$partitions, RF=$REPLICATION_FACTOR)"

  kafka-topics.sh --bootstrap-server "$BOOTSTRAP_SERVER" \
    --create \
    --if-not-exists \
    --topic "$topic" \
    --partitions "$partitions" \
    --replication-factor "$REPLICATION_FACTOR" \
    ${config:+--config "$config"}
}

# --- Audit Events (append-only, long retention) ---
create_topic "exam.audit.events" 6 "retention.ms=-1,cleanup.policy=delete"

# --- Notification Outbound ---
create_topic "exam.notifications.outbound" 3 "retention.ms=604800000"

# --- Session Events (high throughput during exam windows) ---
create_topic "exam.session.events" 12 "retention.ms=604800000"

# --- Question Lifecycle ---
create_topic "exam.question.lifecycle" 3 "retention.ms=2592000000"

# --- Proctoring Alerts (real-time processing) ---
create_topic "exam.proctoring.alerts" 6 "retention.ms=2592000000"

# --- Evaluation Events ---
create_topic "exam.evaluation.events" 6 "retention.ms=2592000000"

# --- Identity Events ---
create_topic "exam.identity.events" 3 "retention.ms=2592000000"

# --- Paper Events ---
create_topic "exam.paper.events" 3 "retention.ms=2592000000"

# --- Response Saved (highest throughput — 12 partitions for parallelism) ---
create_topic "exam.response.saved" 12 "retention.ms=2592000000,min.insync.replicas=2"

# --- Result Published ---
create_topic "exam.result.published" 3 "retention.ms=-1"

echo "=== All topics created successfully ==="

# Verify topics
echo ""
echo "=== Topic Listing ==="
kafka-topics.sh --bootstrap-server "$BOOTSTRAP_SERVER" --list

echo ""
echo "=== Topic Details ==="
kafka-topics.sh --bootstrap-server "$BOOTSTRAP_SERVER" --describe
