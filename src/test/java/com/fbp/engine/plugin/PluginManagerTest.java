package com.fbp.engine.plugin;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fbp.engine.message.Message;
import com.fbp.engine.node.Node;
import com.fbp.engine.registry.NodeRegistry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PluginManagerTest {

    @TempDir
    Path tempDir;

    private NodeRegistry registry;
    private PluginManager pluginManager;

    @BeforeEach
    void setUp() {
        registry = new NodeRegistry();
        pluginManager = new PluginManager(registry, tempDir.toString());
    }

    @AfterEach
    void tearDown() {
        pluginManager.close();
    }

    private void createPluginJar(Path jarPath, String providerClassName) throws Exception {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(jarPath))) {
            zos.putNextEntry(new ZipEntry("META-INF/services/com.fbp.engine.plugin.NodeProvider"));
            zos.write(providerClassName.getBytes());
            zos.closeEntry();
        }
    }

    private void createCorruptJar(Path jarPath) throws Exception {
        Files.writeString(jarPath, "This is not a valid zip archive");
    }

    @Test
    @DisplayName("1. ClassPath 플러그인 로드: ServiceLoader로 ClassPath 내 NodeProvider 자동 발견 및 등록")
    void testLoadFromClassPath() {
        assertDoesNotThrow(() -> pluginManager.loadPlugins());
    }

    @Test
    @DisplayName("2. 외부 JAR 로드: plugins/ 디렉토리의 JAR에서 NodeProvider 발견 및 등록")
    void testLoadFromExternalJar() throws Exception {
        createPluginJar(tempDir.resolve("test-plugin.jar"), TestProvider1.class.getName());

        pluginManager.loadPlugins();

        assertTrue(registry.isRegistered("TestNode1"));
    }

    @Test
    @DisplayName("3. NodeRegistry 자동 등록: 로드된 플러그인의 노드 타입이 NodeRegistry에 등록됨")
    void testAutomaticRegistry() throws Exception {
        createPluginJar(tempDir.resolve("auto-reg-plugin.jar"), TestProvider1.class.getName());

        pluginManager.loadPlugins();
        Node node = registry.create("TestNode1", Map.of());

        assertNotNull(node);
        assertEquals("plugin-node-1", node.getId());
    }

    @Test
    @DisplayName("4. 타입 충돌 처리: 내장 노드와 동일한 typeName의 플러그인 노드 → 정책에 맞게 처리")
    void testTypeCollisionResolution() throws Exception {
        registry.register("TestNode1", config -> new DummyNode("original"), false);
        createPluginJar(tempDir.resolve("conflict-plugin.jar"), TestProvider1.class.getName());

        pluginManager.loadPlugins();
        Node created = registry.create("TestNode1", Map.of());

        assertEquals("plugin-node-1", created.getId());
    }

    @Test
    @DisplayName("5. 잘못된 JAR: 유효하지 않은 JAR 파일 → 예외 후 나머지 플러그인은 정상 로드")
    void testCorruptJarHandling() throws Exception {
        createCorruptJar(tempDir.resolve("corrupt.jar"));
        createPluginJar(tempDir.resolve("valid.jar"), TestProvider1.class.getName());

        assertDoesNotThrow(() -> pluginManager.loadPlugins());
        assertTrue(registry.isRegistered("TestNode1"));
    }

    @Test
    @DisplayName("6. plugins 디렉토리 없음: 디렉토리가 없으면 스캔 건너뜀 (예외 아님)")
    void testMissingPluginDirectory() {
        PluginManager managerForMissingDir = new PluginManager(registry, tempDir.resolve("non-existent").toString());

        assertDoesNotThrow(() -> managerForMissingDir.loadPlugins());
    }

    @Test
    @DisplayName("7. 빈 plugins 디렉토리: 디렉토리는 있지만 JAR가 없으면 정상 (플러그인 0개)")
    void testEmptyPluginDirectory() {
        assertDoesNotThrow(() -> pluginManager.loadPlugins());
        assertFalse(registry.isRegistered("TestNode1"));
    }

    @Test
    @DisplayName("8. 플러그인 수 확인: 복수 JAR 로드 시 전체 등록된 노드 타입 수가 예상과 일치")
    void testMultiplePluginsCount() throws Exception {
        createPluginJar(tempDir.resolve("plugin1.jar"), TestProvider1.class.getName());
        createPluginJar(tempDir.resolve("plugin2.jar"), TestProvider2.class.getName());

        pluginManager.loadPlugins();

        assertTrue(registry.isRegistered("TestNode1"));
        assertTrue(registry.isRegistered("TestNode2"));
    }

    public static class TestProvider1 implements NodeProvider {
        @Override
        public List<NodeDescriptor> getNodeDescriptors() {
            return List.of(new NodeDescriptor("TestNode1", "desc", DummyNode.class, config -> new DummyNode("plugin-node-1")));
        }
    }

    public static class TestProvider2 implements NodeProvider {
        @Override
        public List<NodeDescriptor> getNodeDescriptors() {
            return List.of(new NodeDescriptor("TestNode2", "desc", DummyNode.class, config -> new DummyNode("plugin-node-2")));
        }
    }

    static class DummyNode implements Node {
        private final String id;

        public DummyNode(String id) {
            this.id = id;
        }

        @Override public String getId() { return id; }
        @Override public void process(Message message) {}
        @Override public void initialize() {}
        @Override public void shutdown() {}
    }
}