# 0003: PostgreSQL as Primary Relational Database

- **Status**: Accepted
- **Date**: 2026-08-02
- **Deciders**: Core Database & Backend Team

---

## 1. Context

The platform requires a robust, ACID-compliant database to handle relational domain data, including complex question metadata, examination schedules, candidate records, seat allocations, and evaluation rubrics.

Key requirements:
- ACID transactional guarantees for financial/registration transactions and schedule locking.
- Advanced security features including Row-Level Security (RLS) for multi-tenant isolation and field-level encryption support.
- Support for JSONB semi-structured data (e.g., dynamic question attributes, section structures, and rules).
- High-availability replication and open-source licensing without vendor lock-in.

---

## 2. Decision

We choose **PostgreSQL** as the primary relational datastore for all core microservices:

1. **Database Per Microservice**: Each microservice maintains its dedicated PostgreSQL database schema, preventing cross-domain database coupling.
2. **Row-Level Security (RLS)**: Enforce tenant isolation at the database layer using `tenant_id` policies.
3. **JSONB Data Types**: Utilize JSONB for flexible question metadata, dynamic rule configurations, and audit parameters while maintaining relational integrity on core entities.
4. **Connection Pooling**: Use HikariCP connection pools in Spring Boot alongside PgBouncer for database connection proxying under high concurrency.

---

## 3. Consequences

### Positive
- **Proven Reliability & Integrity**: ACID compliance guarantees zero data corruption during concurrent transactions.
- **Multi-Tenant Security**: Native Row-Level Security ensures robust tenant data isolation at the DB engine level.
- **Extensible Datatypes**: Built-in JSONB support eliminates the need for a separate document database for question item parameters.

### Negative
- **Horizontal Write Scaling Limit**: Single-primary PostgreSQL setups require read-replicas or sharding (e.g. Citus) for extreme write-volume scenarios.
