package com.concurrency;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class MessageBroker {
    private final BlockingQueue<String> queue;
    private final List<String> processedMessages = new CopyOnWriteArrayList<>();
    private volatile boolean producerDone = false;

    public MessageBroker(int capacity) {
        this.queue = new LinkedBlockingQueue<>(capacity);
    }

    // Producer: Adds messages to the queue (blocks if full)
    public void produce(List<String> messages) {
        new Thread(() -> {
            try {
                for (String msg : messages) {
                    queue.put(msg); // Blocks if queue is full (backpressure!)
                }
                producerDone = true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    // Consumer: Takes messages from the queue (blocks if empty)
    public void consume() {
        new Thread(() -> {
            try {
                while (!producerDone || !queue.isEmpty()) {
                    String msg = queue.poll(500, TimeUnit.MILLISECONDS);
                    if (msg != null) {
                        // Simulate processing delay
                        Thread.sleep(50);
                        processedMessages.add(msg);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    public List<String> getProcessedMessages() {
        return processedMessages;
    }

    public int getQueueSize() {
        return queue.size();
    }
}
