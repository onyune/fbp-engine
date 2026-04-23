package com.fbp.engine.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PluginClassLoaderTest {

    @TempDir
    Path tempDir;

    private Path createRealClassJar(String className) throws Exception {
        Path srcFile = tempDir.resolve(className + ".java");
        Files.writeString(srcFile, "public class " + className + " {}");

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler != null) {
            compiler.run(null, null, null, srcFile.toString());
        }

        Path jarPath = tempDir.resolve(className + ".jar");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(jarPath))) {
            Path classFile = tempDir.resolve(className + ".class");
            if (Files.exists(classFile)) {
                zos.putNextEntry(new ZipEntry(className + ".class"));
                zos.write(Files.readAllBytes(classFile));
                zos.closeEntry();
            }
        }
        return jarPath;
    }

    @Test
    @DisplayName("1. JAR 로드: 외부 JAR의 클래스를 정상적으로 로드")
    void testLoadClassFromExternalJar() throws Exception {
        Path jarPath = createRealClassJar("ExternalTestNode");

        try (PluginClassLoader loader = new PluginClassLoader(new URL[]{jarPath.toUri().toURL()}, null)) {
            Class<?> clazz = loader.loadClass("ExternalTestNode");

            assertNotNull(clazz);
            assertEquals("ExternalTestNode", clazz.getName());
        }
    }

    @Test
    @DisplayName("2. 클래스 격리: 플러그인 클래스가 엔진의 내부 클래스에 영향을 주지 않음")
    void testClassIsolation() throws Exception {
        Path jarPath1 = createRealClassJar("IsolatedNode");
        Path jarPath2 = createRealClassJar("IsolatedNode");

        try (PluginClassLoader loader1 = new PluginClassLoader(new URL[]{jarPath1.toUri().toURL()}, null);
             PluginClassLoader loader2 = new PluginClassLoader(new URL[]{jarPath2.toUri().toURL()}, null)) {

            Class<?> clazz1 = loader1.loadClass("IsolatedNode");
            Class<?> clazz2 = loader2.loadClass("IsolatedNode");

            assertNotNull(clazz1);
            assertNotNull(clazz2);
            assertNotSame(clazz1, clazz2);
            assertEquals(loader1, clazz1.getClassLoader());
            assertEquals(loader2, clazz2.getClassLoader());
        }
    }

    @Test
    @DisplayName("3. 리소스 해제: close() 호출 시 JAR 파일 핸들 해제")
    void testCloseReleasesResources() throws Exception {
        Path jarPath = createRealClassJar("ResourceTestNode");
        PluginClassLoader loader = new PluginClassLoader(new URL[]{jarPath.toUri().toURL()}, null);

        loader.close();

        assertThrows(ClassNotFoundException.class, () -> loader.loadClass("ResourceTestNode"));
    }

    @Test
    @DisplayName("4. 존재하지 않는 JAR: 없는 경로의 JAR → 예외")
    void testLoadFromNonExistentJar() throws Exception {
        URL fakeUrl = tempDir.resolve("ghost.jar").toUri().toURL();

        try (PluginClassLoader loader = new PluginClassLoader(new URL[]{fakeUrl}, null)) {
            assertThrows(ClassNotFoundException.class, () -> loader.loadClass("AnyClass"));
        }
    }

}