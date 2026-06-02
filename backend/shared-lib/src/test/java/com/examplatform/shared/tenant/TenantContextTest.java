package com.examplatform.shared.tenant;

import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TenantContext")
class TenantContextTest {

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Nested
    @DisplayName("set and get")
    class SetAndGet {

        @Test
        @DisplayName("returns null before any value is set")
        void returnsNullByDefault() {
            assertThat(TenantContext.getTenantId()).isNull();
        }

        @Test
        @DisplayName("returns the value that was set")
        void returnsSetValue() {
            TenantContext.setTenantId("tenant-abc");
            assertThat(TenantContext.getTenantId()).isEqualTo("tenant-abc");
        }

        @Test
        @DisplayName("clears the value")
        void clearsValue() {
            TenantContext.setTenantId("tenant-xyz");
            TenantContext.clear();
            assertThat(TenantContext.getTenantId()).isNull();
        }
    }

    @Nested
    @DisplayName("thread isolation")
    class ThreadIsolation {

        @Test
        @DisplayName("different threads have independent tenant values")
        void differentThreadsHaveIndependentValues() throws InterruptedException {
            TenantContext.setTenantId("tenant-main");
            String[] otherThreadValue = {null};

            Thread other = Thread.ofVirtual().start(() -> {
                // Virtual thread should NOT inherit (InheritableThreadLocal propagates
                // to child threads started FROM a thread with a value; here we start
                // from test thread but then overwrite)
                TenantContext.setTenantId("tenant-other");
                otherThreadValue[0] = TenantContext.getTenantId();
                TenantContext.clear();
            });
            other.join();

            assertAll(
                () -> assertThat(TenantContext.getTenantId()).isEqualTo("tenant-main"),
                () -> assertThat(otherThreadValue[0]).isEqualTo("tenant-other")
            );
        }
    }
}
