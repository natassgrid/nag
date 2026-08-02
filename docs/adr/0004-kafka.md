# 0004: Apache Kafka as Event Streaming & Audit Backbone

- **Status**: Accepted
- **Date**: 2026-08-02
- **Deciders**: Infrastructure & Security Architecture Team

---

## 1. Context

High-stakes government examinations generate high-velocity telemetry, candidate response streams, and security audit logs during peak exam hours.

Key challenges:
- High volume event streaming ($>50,000$ events/sec during nationwide exam submissions).
- Need for durable, replayable log streams for security audits and forensic analysis.
- Multi-subscriber architecture where audit services, analytics, and notification engines consume the same event streams independently.

---

## 2. Decision

We adopt **Apache Kafka** as the distributed event streaming platform for the National Assessment Grid:

1. **Topic Partitioning Strategy**: Topics partitioned by `tenantId` and `examId` to guarantee ordered message processing per exam session while enabling parallel consumer scaling.
2. **Log Retention & Durability**: Configured with replication factor of 3 and `acks=all` for critical audit and response streams to prevent message loss.
3. **Kafka Connect & Debezium**: Used for Change Data Capture (CDC) to stream database audit changes into security monitoring infrastructure without impacting core service APIs.

---

## 3. Consequences

### Positive
- **Extreme Throughput & Durability**: High disk I/O performance via sequential log writes and zero-copy transfer.
- **Event Replayability**: Consumer services can reprocess event streams from historical offsets during disaster recovery or system audits.
- **Decoupled Architecture**: Producers and consumers scale independently without direct network dependencies.

### Negative
- **Operational Overhead**: Requires managing Zookeeper / KRaft clusters, broker tuning, partition rebalancing, and storage capacity monitoring.
