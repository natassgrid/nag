# Module Specification: Analytics & Reporting Service

## 1. Overview & Purpose

The **Analytics & Reporting Service** provides real-time operational monitoring dashboards, item-response theory (IRT) question difficulty analytics, center attendance reports, and post-exam score distribution statistics.

---

## 2. Core Metrics & Reports

- **Live Attendance Dashboard**: Real-time candidate login counts per center and shift during live exam delivery.
- **Item Analysis**: Question difficulty index ($p$-value) and point-biserial discrimination index ($r_{\text{pbi}}$).
- **Center Performance & Anomaly Detection**: Identifying center-level score outliers or concurrent submission velocity anomalies.
- **Demographic & Category Reports**: Aggregate score breakdowns by category, gender, and geographic region.

---

## 3. Data Ingestion Architecture

```
[ Candidate Submissions ] ──> [ Kafka Topic: answer.submitted ] ──> [ Flink / Stream Processor ]
                                                                           │
[ Grafana / Analytics Dashboard ] <── [ PostgreSQL Read Replica ] <────────┘
```

---

## 4. REST API Reference

Base Path: `/api/v1/analytics`

| Method | Path | Roles | Description |
|---|---|---|---|
| `GET` | `/live-attendance/{examId}` | EXAM_CONTROLLER, SUPER_ADMIN | Real-time center attendance metrics |
| `GET` | `/item-analysis/{examId}` | EXAM_CONTROLLER | Question difficulty & discrimination index |
| `GET` | `/reports/score-distribution/{examId}` | EXAM_CONTROLLER | Score distribution histogram data |
