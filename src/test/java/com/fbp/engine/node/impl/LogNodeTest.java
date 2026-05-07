package com.fbp.engine.node.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fbp.engine.core.impl.LocalConnection;
import com.fbp.engine.message.Message;
import java.util.HashMap;
import org.junit.jupiter.api.Test;

class LogNodeTest {

    @Test
    void testPassThrough() {
        LogNode logger = new LogNode("logger");
        LocalConnection conn = new LocalConnection("c1");
        logger.getOutputPort("out").connect(conn);

        Message msg = new Message(new HashMap<>());
        logger.process(msg);

        assertEquals(msg, conn.poll());
    }

    @Test
    void testMiddleInsertion() {
        GeneratorNode gen = new GeneratorNode("gen");
        LogNode logger = new LogNode("logger");
        PrintNode printer = new PrintNode("printer");

        LocalConnection c1 = new LocalConnection("c1");
        LocalConnection c2 = new LocalConnection("c2");

        gen.getOutputPort("out").connect(c1);
        c1.setTarget(logger.getInputPort("in"));

        logger.getOutputPort("out").connect(c2);
        c2.setTarget(printer.getInputPort("in"));

        gen.generate("key", "val");
        Message msgToLogger = c1.poll();

        logger.getInputPort("in").receive(msgToLogger);
        Message msgToPrinter = c2.poll();

        assertNotNull(msgToPrinter);
        assertEquals("val", msgToPrinter.get("key"));
    }
}