package com.fbp.engine.node.impl;

import com.fbp.engine.message.Message;
import com.fbp.engine.metrics.MetricsCollector;
import com.fbp.engine.node.AbstractNode;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * input port : X
 * output port : out
 */
public class GeneratorNode extends AbstractNode {
    private ScheduledExecutorService scheduler; //임시
    private int counter = 0; //임시

    public GeneratorNode(String id) {
        super(id);
        addOutputPort("out");
    }

    @Override
    protected void onProcess(Message message) {

    }

    // key, value를 가지고 메시지를 만들어서 연결된 outputPort의 send함
    public void generate(String key, Object value){
        long startTime = System.currentTimeMillis();
        boolean success = true;

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put(key, value);
            send("out", new Message(payload));
        } catch (Exception e) {
            success = false;
        } finally {
            long durationMs = System.currentTimeMillis() - startTime;
            String fId = this.getFlowId() != null ? this.getFlowId() : "unknown-flow";

            MetricsCollector.getInstance()
                    .recordNodeProcessing(fId, this.getId(), "GeneratorNode", durationMs, success, 0, 0);
            MetricsCollector.getInstance().recordFlowStats(fId, "local", durationMs, success);
        }
    }

    @Override //임시
    public void initialize() {
        scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
            counter++;
            double fakeTemp = 20.0 + (Math.random() * 10.0);
            generate("value", fakeTemp);
        }, 0, 1, TimeUnit.SECONDS);
    }

    @Override //임시
    public void shutdown() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }
}
