package com.fbp.engine.node.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fbp.engine.message.Message;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileWriterNodeTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("파일 생성")
    void test1_fileCreation() {

        Path filePath = tempDir.resolve("test_log.txt");
        FileWriteNode node = new FileWriteNode("fw-1", filePath.toString());

        node.initialize();

        assertTrue(Files.exists(filePath), "initialize() 호출 후 파일이 생성되어야 합니다.");

        node.shutdown();
    }

    @Test
    @DisplayName("내용 기록")
    void test2_contentWritten() throws Exception {
        Path filePath = tempDir.resolve("test_log_3lines.txt");
        FileWriteNode node = new FileWriteNode("fw-1", filePath.toString());
        node.initialize();

        node.onProcess(new Message(new HashMap<>()));
        node.onProcess(new Message(new HashMap<>()));
        node.onProcess(new Message(new HashMap<>()));

        node.shutdown();
        List<String> lines = Files.readAllLines(filePath);
        assertEquals(3, lines.size(), "파일에 정확히 3줄이 기록되어야 합니다.");
    }

    @Test
    @DisplayName("shutdown 후 파일 닫힘")
    void test3_closedAfterShutdown() {
        Path filePath = tempDir.resolve("test_log_closed.txt");
        FileWriteNode node = new FileWriteNode("fw-1", filePath.toString());

        node.initialize();
        node.shutdown();

        assertThrows(RuntimeException.class, () -> {
            node.onProcess(new Message(new HashMap<>()));
        }, "shutdown() 이후에 쓰기를 시도하면 예외가 발생해야 합니다.");
    }
}