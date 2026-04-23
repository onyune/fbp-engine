package com.fbp.engine.plugin;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * 2. 파일 탐색
 * scanForJars()메서드가 plugins/ (외부 커스텀 노드 프로젝트의 jar를 모아둔 디렉토리)를 뒤져서 .jar 파일들을 찾아내고
 * 이를 자바가 인지할 수 있는 URL[] 배열로 반환함
 */
@Slf4j
public class PluginScanner {
    private final File pluginDir;

    public PluginScanner(String pluginDirPath) {
        this.pluginDir = new File(pluginDirPath);
    }

    /**
     * 플러그인 디렉토리를 스캔하여 JAR 파일들의 URL 목록을 반환합니다.
     */
    public URL[] scanForJars() {
        if (!pluginDir.exists() || !pluginDir.isDirectory()) {
            log.info("[PluginScanner] 플러그인 디렉토리가 존재하지 않습니다: {}", pluginDir.getAbsolutePath());
            return new URL[0];
        }

        // .jar 로 끝나는 파일만 필터링
        File[] jarFiles = pluginDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));

        if (jarFiles == null || jarFiles.length == 0) {
            log.info("[PluginScanner] 플러그인 디렉토리에 JAR 파일이 없습니다.");
            return new URL[0];
        }

        List<URL> urls = new ArrayList<>();
        for (File jar : jarFiles) {
            try {
                urls.add(jar.toURI().toURL());
                log.info("[PluginScanner] 외부 JAR 발견: {}", jar.getName());
            } catch (MalformedURLException e) {
                // 파일 경로가 이상한 경우 에러 로깅만 하고 다음 파일로 넘어감
                log.error("[PluginScanner] JAR 파일의 URL 변환 실패: {}", jar.getName(), e);
            }
        }

        return urls.toArray(new URL[0]);
    }
}