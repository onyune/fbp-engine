package com.fbp.engine.plugin;

import com.fbp.engine.registry.NodeRegistry;
import java.net.URL;
import java.util.ServiceLoader;
import lombok.extern.slf4j.Slf4j;

/**
 * 4. 서비스 발견 (PluginManger + ServiceLoader)
 * ServiceLoader.load(NodeProvider.class, PluginClassLoader); 가 실행되면,
 * JAR 파일 안의 META-INF/services/ 설정을 읽어 실제 구현체 클래스를 찾아내고 인스턴스를 생성함
 *
 * 5. 레지스트리 등록
 * 생성된 NodeProvider로 부터 NodeDescriptor들을 꺼내와서 엔진의 NodeRegistry에 register 메서드로 집어넣음
 *
 * 6. 자원 해제
 * 엔진 종료시 PluginClassLoader들을 담아 점유 중인 JAR 파일 핸들을 해제함
 */
@Slf4j
public class PluginManager {
    private final NodeRegistry registry;
    private final PluginScanner scanner;
    private PluginClassLoader pluginClassLoader;

    public PluginManager(NodeRegistry registry, String pluginDirPath) {
        this.registry = registry;
        this.scanner = new PluginScanner(pluginDirPath);
    }

    public void loadPlugins(){
        log.info("[Plugin] 플러그인 로딩 시작");
        //기본 ClassPath 스캔 (내장 프로바이더 탐색)
        loadFromClassLoader(Thread.currentThread().getContextClassLoader());

        // 외부 JAR 파일 스캔 (Scanner 활용)
        URL[] jarUrls = scanner.scanForJars();

        // 찾은 JAR 파일이 있다면 커스텀 클래스로더로 읽어들임
        if (jarUrls.length > 0) {
            pluginClassLoader = new PluginClassLoader(jarUrls, Thread.currentThread().getContextClassLoader());
            loadFromClassLoader(pluginClassLoader);
        }
    }

    private void loadFromClassLoader(ClassLoader classLoader){
        ServiceLoader<NodeProvider> loader = ServiceLoader.load(NodeProvider.class, classLoader);

        for(NodeProvider provider : loader){
            try{
                for(NodeDescriptor descriptor : provider.getNodeDescriptors()){
                    // 외부 플러그인이 내장 노드를 덮어쓸 수 있도록 overwrite=true 설정
                    registry.register(descriptor.typeName(), descriptor.factory(),true);
                    log.info("[Plugin] 노드 등록 완료: 타입{} - 클래스{}", descriptor.typeName(), descriptor.nodeClass().getSimpleName());
                }
            } catch (Exception e) {

                log.error("[Plugin] 플러그인 로드 중 오류 발생: {}", provider.getClass().getName());
            }
        }
    }

    public void close(){
        if(pluginClassLoader !=null){
            try{
                pluginClassLoader.close();
            }catch (Exception e){
                log.error("[Plugin] PluginClassLoader 닫기 실패", e);
            }
        }
    }
}
