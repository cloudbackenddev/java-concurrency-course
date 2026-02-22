package com.concurrency;

import org.junit.jupiter.api.Test;

import static java.util.concurrent.TimeUnit.*;
import static org.awaitility.Awaitility.*;
import static org.junit.jupiter.api.Assertions.*;

class BackgroundWorkerTest {

    @Test
    void workerStops_whenFlagSetToFalse() {
        BackgroundWorker worker = new BackgroundWorker();
        worker.start();

        // Wait until the worker has done at least some work (proves it's alive)
        await().atMost(2, SECONDS)
                .until(() -> worker.getWorkCount() > 0);

        // Now stop it
        worker.stop();

        // Awaitility polls until the worker's loop has exited
        await().atMost(2, SECONDS)
                .untilAsserted(() -> assertFalse(worker.isRunning()));
    }

    @Test
    void workerPerformsWork_whileRunning() {
        BackgroundWorker worker = new BackgroundWorker();
        worker.start();

        // Assert that work count keeps increasing over time
        await().atMost(3, SECONDS)
                .until(() -> worker.getWorkCount() >= 5);

        worker.stop();

        // Capture final count after stop
        int countAtStop = worker.getWorkCount();

        // Give it a moment, then verify no more work was done
        await().during(500, MILLISECONDS)
                .atMost(2, SECONDS)
                .until(() -> worker.getWorkCount() <= countAtStop + 1);
    }
}
