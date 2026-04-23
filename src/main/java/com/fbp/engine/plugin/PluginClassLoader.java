package com.fbp.engine.plugin;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * 3. 메모리 적재
 * PluginScanner 에서 찾은 URL 배열을 들고 PluginClassLoader가 생성됨.
 * 이제 이 로더는 메인 엔진이 모르는 외부 JAR 파일 내부의 클래스들을 읽어올 수 있는 능력을 갖게 됨
 */
public class PluginClassLoader extends URLClassLoader {
    public PluginClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }
    public void addJar(URL url){
        super.addURL(url);
    }

}
