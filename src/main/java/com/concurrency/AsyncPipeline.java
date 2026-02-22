package com.concurrency;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

public class AsyncPipeline {
    private final List<String> stagesCompleted = new CopyOnWriteArrayList<>();
    private volatile String finalResult = null;

    public void execute(String input) {
        CompletableFuture
                .supplyAsync(() -> {
                    // Stage 1: Fetch (simulate I/O delay)
                    sleep(200);
                    stagesCompleted.add("FETCH");
                    return "raw:" + input;
                })
                .thenApply(raw -> {
                    // Stage 2: Transform
                    sleep(100);
                    stagesCompleted.add("TRANSFORM");
                    return raw.toUpperCase();
                })
                .thenApply(transformed -> {
                    // Stage 3: Store
                    sleep(100);
                    stagesCompleted.add("STORE");
                    return "stored:" + transformed;
                })
                .thenAccept(result -> {
                    finalResult = result;
                })
                .exceptionally(ex -> {
                    finalResult = "ERROR: " + ex.getMessage();
                    return null;
                });
    }

    public String getFinalResult() {
        return finalResult;
    }

    public List<String> getStagesCompleted() {
        return stagesCompleted;
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
