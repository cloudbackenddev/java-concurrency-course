package com.concurrency;

import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.concurrent.TimeUnit.*;
import static org.awaitility.Awaitility.*;
import static org.junit.jupiter.api.Assertions.*;

class MessageBrokerTest {

    @Test
    void allMessagesAreConsumed_inOrder() {
        List<String> messages = List.of("Order-1", "Order-2", "Order-3", "Order-4", "Order-5");
        MessageBroker broker = new MessageBroker(3); // Small buffer = forces blocking!

        // Start consumer FIRST, then producer
        broker.consume();
        broker.produce(messages);

        // Await until all 5 messages have been processed
        await().atMost(10, SECONDS)
                .until(() -> broker.getProcessedMessages().size() == messages.size());

        // Verify all messages arrived AND in the correct order
        assertEquals(messages, broker.getProcessedMessages());
    }

    @Test
    void queueAppliesBackpressure_whenFull() {
        // Buffer of 2, but we push 5 items — producer must block until consumer drains
        MessageBroker broker = new MessageBroker(2);

        List<String> messages = List.of("A", "B", "C", "D", "E");
        broker.produce(messages);

        // Queue should fill up to capacity (2) before consumer starts
        await().atMost(2, SECONDS)
                .until(() -> broker.getQueueSize() == 2);

        // Now start consuming
        broker.consume();

        // Wait for all to be processed
        await().atMost(10, SECONDS)
                .until(() -> broker.getProcessedMessages().size() == 5);

        assertEquals(messages, broker.getProcessedMessages());
    }
}
