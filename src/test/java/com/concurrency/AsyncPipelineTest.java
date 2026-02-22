package com.concurrency;

import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.concurrent.TimeUnit.*;
import static org.awaitility.Awaitility.*;
import static org.junit.jupiter.api.Assertions.*;

class AsyncPipelineTest {

    @Test
    void pipeline_executesAllStagesInOrder() {
        AsyncPipeline pipeline = new AsyncPipeline();
        pipeline.execute("order-42");

        // Wait until the pipeline completes
        await().atMost(5, SECONDS)
                .until(() -> pipeline.getFinalResult() != null);

        // Verify final result includes all transformations
        assertEquals("stored:RAW:ORDER-42", pipeline.getFinalResult());

        // Verify the stages ran in sequence
        assertEquals(List.of("FETCH", "TRANSFORM", "STORE"),
                pipeline.getStagesCompleted());
    }

    @Test
    void pipeline_progressesThroughStages() {
        AsyncPipeline pipeline = new AsyncPipeline();
        pipeline.execute("data");

        // First, wait for FETCH to complete (earliest stage)
        await().atMost(3, SECONDS)
                .until(() -> pipeline.getStagesCompleted().contains("FETCH"));

        // Then wait for the full pipeline
        await().atMost(5, SECONDS)
                .until(() -> pipeline.getStagesCompleted().size() == 3);

        assertNotNull(pipeline.getFinalResult());
    }
}
