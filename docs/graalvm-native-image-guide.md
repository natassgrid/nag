# GraalVM Native Image & Spring Boot 4.x AOT Guide

## Architecture Overview

Spring Boot 4.x introduces first-class Ahead-of-Time (AOT) engine optimizations that analyze the application graph at build time and generate pre-computed bean registrations, reflection metadata, and native configuration. When combined with GraalVM Native Image compilation, Java bytecode is directly translated into standalone, machine-native executables (ELF on Linux / Mach-O on macOS / PE on Windows).

```
                         Standard OpenJDK JVM                  GraalVM Native Image (Spring Boot 4.x)
                    ┌──────────────────────────────┐     ┌────────────────────────────────────────┐
                    │       Bytecode (.class)      │     │       Pre-Compiled Machine Code        │
                    │              │               │     │      (Ahead-Of-Time / Spring AOT)      │
                    │   JIT Compilation + Warmup   │     │                   │                    │
                    │              ▼               │     │                   ▼                    │
                    │   Startup Time: 4s - 12s     │     │        Startup Time: < 30ms            │
                    │   Memory (RSS): 250MB - 600MB│     │        Memory (RSS): 25MB - 50MB       │
                    └──────────────────────────────┘     └────────────────────────────────────────┘
```

---

## Performance Targets & Benefits

| Metric | JVM Mode (HotSpot) | GraalVM Native Image | Improvement |
|---|---|---|---|
| **Cold-Start Duration** | 4,000ms – 12,000ms | **15ms – 45ms** | **~99% faster** |
| **Base Memory Footprint (RSS)** | 250MB – 600MB | **25MB – 50MB** | **~85% memory reduction** |
| **Container Image Size** | ~280MB (Temurin JRE) | **~38MB (Distroless)** | **~86% smaller image** |
| **Autoscaling Agility** | Slow (requires warmup) | Instantaneous scale-to-zero | Critical for peak exam traffic |

---

## Build Toolchain Configuration

### 1. Version Catalog (`gradle/libs.versions.toml`)

The Gradle version catalog declares Spring Boot 4.x and the GraalVM Native Build Tools plugin:

```toml
[versions]
spring-boot = "4.1.0"
graalvm-native-plugin = "0.10.5"

[plugins]
spring-boot = { id = "org.springframework.boot", version.ref = "spring-boot" }
graalvm-native = { id = "org.graalvm.buildtools.native", version.ref = "graalvm-native-plugin" }
```

### 2. Root Build Script (`build.gradle`)

Root `build.gradle` configures default Native Image build arguments for all subprojects:

```groovy
plugins.withId('org.graalvm.buildtools.native') {
    graalvmNative {
        binaries {
            main {
                imageName = project.name
                buildArgs.addAll([
                    '-H:+ReportExceptionStackTraces',
                    '-H:+AddAllCharsets',
                    '--enable-preview',
                    '--initialize-at-build-time=org.slf4j.LoggerFactory,org.slf4j.helpers.Reporter'
                ])
            }
        }
    }
}
```

---

## Spring AOT Runtime Hints (`GraalVmRuntimeHintsRegistrar`)

Dynamic Java reflection, JDK dynamic proxies, serialization, and classpath resources that cannot be statically discovered by the AOT compiler are registered in `backend/shared-lib/src/main/java/com/examplatform/shared/aot/GraalVmRuntimeHintsRegistrar.java`:

- **Shared Domain & API DTOs**: `ApiResponse`, `ExamPlatformProblemDetail`, `ProblemDetailBuilder`, `TokenResponse`, `GenericDomainEvent`, `TenantContext`.
- **Lifecycle Enums**: `QuestionState`, `PaperState`, `SessionState`, `EvaluationState`, `AuditEventType`, `TranslationState`.
- **Third-Party Libraries**: `com.fasterxml.uuid.Generators` (UUID v7), `org.postgresql.util.PGobject`, `com.pgvector.PGvector`.
- **Resource Bundles**: `application*.yml`, `db/migration/*`, `META-INF/spring/*`, `*.proto`.

Registered automatically via:
- `META-INF/spring/aot.factories`
- `com.examplatform.shared.aot.GraalVmAutoConfiguration` (`@ImportRuntimeHints`)

---

## Common Gradle Commands

```bash
# 1. Validate Spring Boot 4.x AOT processing (fast, tests bean definition generation)
./gradlew :backend:api-gateway:processAot
./gradlew :backend:paper-generator:processAot

# 2. Collect reachability metadata for external dependencies
./gradlew :backend:api-gateway:collectReachabilityMetadata

# 3. Build native executable (requires GraalVM JDK 21+ with native-image tool)
./gradlew :backend:api-gateway:nativeCompile -x test

# 4. Run native unit & integration tests
./gradlew :backend:shared-lib:test
./gradlew :backend:api-gateway:nativeTest
```

---

## Building Native Container Images

Build standalone distroless native containers without needing a local GraalVM installation:

```bash
# Build native container for api-gateway
docker build -f backend/Dockerfile.native --build-arg SERVICE_NAME=api-gateway -t nag/api-gateway:native .

# Build native container for paper-generator
docker build -f backend/Dockerfile.native --build-arg SERVICE_NAME=paper-generator -t nag/paper-generator:native .

# Run native container
docker run -p 8080:8080 nag/api-gateway:native
```
