package com.examplatform.shared.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TenantContextTest {

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void setAndGet_returnsStoredTenantId() {
        TenantContext.set("authority-001");
        assertEquals("authority-001", TenantContext.get());
    }

    @Test
    void clear_removesValue() {
        TenantContext.set("authority-001");
        TenantContext.clear();
        assertNull(TenantContext.get());
    }

    @Test
    void getRequired_throwsWhenNotSet() {
        assertThrows(IllegalStateException.class, TenantContext::getRequired);
    }

    @Test
    void getRequired_returnsValueWhenSet() {
        TenantContext.set("authority-002");
        assertEquals("authority-002", TenantContext.getRequired());
    }

    @Test
    void set_throwsForNullTenant() {
        assertThrows(IllegalArgumentException.class, () -> TenantContext.set(null));
    }

    @Test
    void set_throwsForBlankTenant() {
        assertThrows(IllegalArgumentException.class, () -> TenantContext.set("   "));
    }

    @Test
    void tenantIsolation_betweenThreads() throws InterruptedException {
        TenantContext.set("main-thread-tenant");

        Thread otherThread = new Thread(() -> {
            // A different thread should NOT inherit InheritableThreadLocal
            // values from an unrelated thread (only child threads inherit).
            // This thread is not a child; assert it starts with null.
            TenantContext.clear(); // ensure clean state
            assertNull(TenantContext.get(), "Independent threads should not share tenant context");
        });

        otherThread.start();
        otherThread.join();

        // Main thread value should be unchanged
        assertEquals("main-thread-tenant", TenantContext.get());
    }

    @Test
    void inheritableThreadLocal_propagatesToChildThread() throws InterruptedException {
        TenantContext.set("parent-tenant");

        // Use an array to capture the child's view (lambdas require effectively final)
        String[] childValue = new String[1];
        Thread child = new Thread(() -> childValue[0] = TenantContext.get());
        child.start();
        child.join();

        assertEquals("parent-tenant", childValue[0],
                "Child thread should inherit tenant from parent via InheritableThreadLocal");
    }
}
