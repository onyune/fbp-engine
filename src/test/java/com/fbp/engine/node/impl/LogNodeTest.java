package com.fbp.engine.node.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fbp.engine.core.Connection;
import com.fbp.engine.message.Message;
import java.util.HashMap;
import org.junit.jupiter.api.Test;

class LogNodeTest {

    @Test
    void testPassThrough() {
        LogNode logger = new LogNode("logger");
        Connection conn = new Connection("c1");
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

        Connection c1 = new Connection("c1");
        Connection c2 = new Connection("c2");

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