package com.fbp.engine.node.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fbp.engine.message.Message;
import com.fbp.engine.node.AbstractNode;
import java.util.HashMap;
import org.junit.jupiter.api.Test;

class PrintNodeTest {

    @Test
    void testInputPortNotNull() {
        PrintNode printer = new PrintNode("printer");
        assertNotNull(printer.getInputPort("in"));
    }

    @Test
    void testProcessWithoutException() {
        PrintNode printer = new PrintNode("printer");
        Message msg = new Message(new HashMap<>());
        assertDoesNotThrow(() -> printer.process(msg));
    }

    @Test
    void testInstanceOfAbstractNode() {
        PrintNode printer = new PrintNode("printer");
        assertTrue(printer instanceof AbstractNode);
    }
}