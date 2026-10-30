package com.resourceflow.service;

import com.resourceflow.exception.ResourceValidationException;
import com.resourceflow.model.Resource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class CapacityTransferTest {

    private Resource gpu(String id, double capacity) {
        return new Resource(id, id, "Compute", capacity, capacity, "Hours", "", true);
    }

    @Test
    @DisplayName("Transfer moves total and available capacity")
    void transferMovesCapacity() {
        Resource h100 = gpu("GPU-H100", 120);
        Resource a100 = gpu("GPU-A100", 80);

        CapacityTransfer.transfer(h100, a100, 20);

        assertEquals(100, h100.getTotalCapacity(), 1e-9);
        assertEquals(100, h100.getAvailableCapacity(), 1e-9);
        assertEquals(100, a100.getTotalCapacity(), 1e-9);
        assertEquals(100, a100.getAvailableCapacity(), 1e-9);
    }

    @Test
    @DisplayName("Invalid transfers are rejected with clear messages")
    void invalidTransfersAreRejected() {
        Resource h100 = gpu("GPU-H100", 120);
        Resource a100 = gpu("GPU-A100", 80);
        Resource seats = new Resource("LAB", "Lab", "Facilities", 25, 25, "Seats", "", true);

        assertThrows(ResourceValidationException.class, () -> CapacityTransfer.transfer(h100, a100, 0));
        assertThrows(ResourceValidationException.class, () -> CapacityTransfer.transfer(h100, a100, 500));
        assertThrows(ResourceValidationException.class, () -> CapacityTransfer.transfer(h100, h100, 5));
        ResourceValidationException unitError =
                assertThrows(ResourceValidationException.class, () -> CapacityTransfer.transfer(h100, seats, 5));
        assertTrue(unitError.getMessage().contains("Seats"));
        assertThrows(IllegalArgumentException.class, () -> CapacityTransfer.transfer(null, a100, 5));
        assertEquals(120, h100.getTotalCapacity(), 1e-9, "failed transfers must not change anything");
    }

    @Test
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    @DisplayName("Opposite transfers on two threads never deadlock and never lose capacity")
    void opposingTransfersDoNotDeadlock() throws InterruptedException {
        Resource a = gpu("GPU-A", 1000);
        Resource b = gpu("GPU-B", 1000);
        int rounds = 20_000;

        // Thread 1 moves A -> B while thread 2 moves B -> A: the classic deadlock setup
        Thread forward = new Thread(() -> {
            for (int i = 0; i < rounds; i++) CapacityTransfer.transfer(a, b, 1);
        }, "forward");
        Thread backward = new Thread(() -> {
            for (int i = 0; i < rounds; i++) CapacityTransfer.transfer(b, a, 1);
        }, "backward");

        forward.start();
        backward.start();
        forward.join(15_000);
        backward.join(15_000);

        assertFalse(forward.isAlive() || backward.isAlive(), "threads are stuck: deadlock");
        assertEquals(2000, a.getTotalCapacity() + b.getTotalCapacity(), 1e-6, "capacity was created or lost");
    }
}
