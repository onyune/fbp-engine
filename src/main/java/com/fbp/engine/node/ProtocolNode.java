package com.fbp.engine.node;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 추상 클래스 ProtocolNode
 * 연결 상태 관리  (DISCONNECTED, CONNECTING, CONNECTED, ERROR)
 * 자동 재연결 로직 (연결이 끊기면 일정 주기로 재시도)
 * 연결 이벤트 알림 (연결 성공/실패/끊김을 로그 또는 메시지로 전파)
 * 설정(Configuration) 관리 (호스트, 포트, 인증 정보 등)
 *
 */
@Slf4j
public abstract class ProtocolNode extends AbstractNode {

    private final Map<String, Object> config;
    @Getter
    private ConnectionState connectionState;
    private final long reconnectIntervalMs;

    //재연결 스케줄러와 재시도 횟수 관리
    private ScheduledExecutorService scheduler;
    private int maxRetries;
    private int currentRetryCount =0;

    protected ProtocolNode(String id, Map<String, Object> config) {
        super(id);
        this.config = config;
        this.connectionState = ConnectionState.DISCONNECTED;

        Object intervalMs = config.get("reconnectIntervalMs");
        this.reconnectIntervalMs = intervalMs instanceof Number ? ((Number) intervalMs).longValue(): 5000L;

        Object retriesConfig = getConfig("maxRetries");
        this.maxRetries = retriesConfig instanceof Number ? ((Number) retriesConfig).intValue() : 10;
    }

    /**
     * 초기화 로직
     * 이 추상 클래스를 상속받는 클래스의 connect 실행
     * 실패시 재연결 로직 수행
     * 성공 시 상태 변경
     */
    @Override
    public void initialize() {
        super.initialize();
        this.currentRetryCount=0;
        doConnect();
    }

    /**
     * 연결 끊는 로직
     * disconnect() 호출
     * 성공 시 상태 변경
     */
    @Override
    public void shutdown() {
        if(scheduler != null && !scheduler.isShutdown()){
            scheduler.shutdownNow();
        }
        try{
            disconnect();
        } catch (Exception e) {
            log.error("[{}] 연결 해제 중 에러 발생: {}", getId(), e.getMessage());
        }finally {
            this.connectionState=ConnectionState.DISCONNECTED;
        }

        super.shutdown();
    }

    // 하위 클래스가 실제 연결 로직 구현 (예외 발생 가능)
    protected abstract void connect() throws Exception;

    // 하위 클래스가 실제 연결 해제 로직 구현
    protected abstract void disconnect() throws Exception;

    protected void reconnect() {
        if(currentRetryCount >= maxRetries){
            log.error("[{}] 최대 재연결 시도 횟수({}회) 초과. 재시도를 중단합니다.", getId(), maxRetries);
            this.connectionState = ConnectionState.ERROR;
            return;
        }
        if(scheduler == null || scheduler.isShutdown()){
            scheduler = Executors.newSingleThreadScheduledExecutor();
        }

        currentRetryCount++;
        log.info("[{}] {}ms 후 재연결 시도", getId(), reconnectIntervalMs);
        scheduler.schedule(this::doConnect, reconnectIntervalMs, TimeUnit.MILLISECONDS);

    }

    public Object getConfig(String key){
        return config != null ? config.get(key) : null;
    }

    public boolean isConnected(){
        return this.connectionState == ConnectionState.CONNECTED;
    }

    private void doConnect(){
        this.connectionState = ConnectionState.CONNECTING;

        try{
            connect(); //하위 클래스 연결 로직 호출
            this.connectionState = ConnectionState.CONNECTED; // 성공 시 상태변경
            this.currentRetryCount = 0; // 연결 성공시 재시도 횟수 초기화
            log.info("[{}] 연결 성공", getId());
        } catch (Exception e) {
            log.error("[{}] 연결 실패: ", e.getMessage());
            this.connectionState = ConnectionState.ERROR; //실패 시 에러
            reconnect(); // 실패 시 재연결 스케줄러 시작
        }
    }
}
