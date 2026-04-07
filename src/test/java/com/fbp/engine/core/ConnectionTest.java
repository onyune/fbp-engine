package com.fbp.engine.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fbp.engine.message.Message;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ConnectionTest {

    @Test
    void testDeliverAndPoll() {
        Connection conn = new Connection("test-conn");
        Message msg = new Message(new HashMap<>());

        conn.deliver(msg);
        assertEquals(msg, conn.poll());
    }

    @Test
    void testMessageOrder() {
        Connection conn = new Connection("test-conn");

        Map<String, Object> d1 = new HashMap<>(); d1.put("seq", 1);
        Map<String, Object> d2 = new HashMap<>(); d2.put("seq", 2);
        Map<String, Object> d3 = new HashMap<>(); d3.put("seq", 3);

        Message m1 = new Message(d1);
        Message m2 = new Message(d2);
        Message m3 = new Message(d3);

        conn.deliver(m1);
        conn.deliver(m2);
        conn.deliver(m3);

        assertEquals(m1, conn.poll());
        assertEquals(m2, conn.poll());
        assertEquals(m3, conn.poll());
    }

    @Test
    void testMultiThreadAndPollBlocking() throws InterruptedException {
        Connection conn = new Connection("test-conn");
        Message targetMsg = new Message(new HashMap<>());

        AtomicReference<Message> receivedMsg = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Thread consumer = new Thread(() -> {
            receivedMsg.set(conn.poll());
            latch.countDown();
        });
        consumer.start();

        Thread.sleep(100);
        assertEquals(Thread.State.WAITING, consumer.getState());

        conn.deliver(targetMsg);

        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertEquals(targetMsg, receivedMsg.get());
    }

    @Test
    void testBufferCapacityBlocking() throws InterruptedException {
        Connection conn = new Connection("test-conn", 2);

        conn.deliver(new Message(new HashMap<>()));
        conn.deliver(new Message(new HashMap<>()));

        CountDownLatch latch = new CountDownLatch(1);
        Thread producer = new Thread(() -> {
            conn.deliver(new Message(new HashMap<>()));
            latch.countDown();
        });
        producer.start();

        assertFalse(latch.await(500, TimeUnit.MILLISECONDS));
        assertEquals(2, conn.getBufferSize());

        producer.interrupt();
    }

    @Test
    void testBufferSize() {
        Connection conn = new Connection("test-conn", 10);
        conn.deliver(new Message(new HashMap<>()));
        conn.deliver(new Message(new HashMap<>()));

        assertEquals(2, conn.getBufferSize());
    }
}