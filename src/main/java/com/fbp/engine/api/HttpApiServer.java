package com.fbp.engine.api;

import com.fbp.engine.engine.FlowManager;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;

/// HttpServer 기반 REST API 서버
@Slf4j
public class HttpApiServer {
    private HttpServer server;
    private final int port;

    public HttpApiServer(int port) {
        this.port = port;
    }

    public void start(FlowManager flowManager) throws IOException{
        server=HttpServer.create(new InetSocketAddress(port), 0 );
        server.setExecutor(Executors.newFixedThreadPool(10));

        server.createContext("/health", new HealthHandler(flowManager));
        server.createContext("/flows", new FlowHandler(flowManager));
        server.createContext("/nodes", new MetricsHandler(flowManager));

        server.start();
        log.info("[HttpApiServer] 서버 시작");

    }

    public void stop(){
        if(server != null){
            server.stop(0);
            log.info("[HttpApiServer]서버 종료");
        }
    }
}
