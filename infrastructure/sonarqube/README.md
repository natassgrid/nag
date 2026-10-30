# SonarQube Local Stack

This directory provides a standalone Docker Compose environment for running **SonarQube Community Edition** backed by a dedicated PostgreSQL 16 database.

---

## Quick Start

### 1. Prerequisites
SonarQube uses an embedded Elasticsearch instance which requires `vm.max_map_count` to be at least `262144`.
Check on Linux:
```bash
sysctl vm.max_map_count
```
If less than `262144`:
```bash
sudo sysctl -w vm.max_map_count=262144
```

### 2. Start SonarQube
```bash
cd infrastructure/sonarqube
./start-sonarqube.sh
```
Or directly with Docker Compose:
```bash
docker compose up -d
```

### 3. Access SonarQube UI
- **URL**: [http://localhost:9000](http://localhost:9000)
- **Default Username**: `admin`
- **Default Password**: `admin`
*(You will be prompted to change the password on initial login)*

### 4. Stop SonarQube
```bash
cd infrastructure/sonarqube
./stop-sonarqube.sh
```
Or:
```bash
docker compose down
```

---

## Scanning Code with Gradle

SonarQube scanner can be executed via Gradle for any service.

1. Generate an analysis token in SonarQube UI: **User Avatar > My Account > Security > Generate Token**.
2. Run scan (for example on `question-bank-service`):
```bash
./gradlew sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=<YOUR_GENERATED_TOKEN>
```
Or scan only a specific subproject:
```bash
./gradlew :backend:question-bank-service:sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=<YOUR_GENERATED_TOKEN>
```

---

## Service Architecture

| Service | Container Name | Port | Description |
|---|---|---|---|
| `sonarqube` | `exam-sonarqube` | `9000` | SonarQube Community Edition |
| `sonar-db` | `exam-sonarqube-db` | Internal (5432) | Dedicated PostgreSQL 16 DB |

Both services run on an isolated Docker network (`exam-sonar-network`) with persistent volumes for data, extensions, and logs.
