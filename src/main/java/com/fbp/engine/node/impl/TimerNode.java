package com.fbp.engine.node.impl;

import com.fbp.engine.message.Message;
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
public class TimerNode extends AbstractNode {
    private final long intervalMs;
    private int tickCount =0 ;
    private ScheduledExecutorService scheduler;

    public TimerNode(String id, long intervalMs) {
        super(id);
        this.intervalMs = intervalMs;
        addOutputPort("out");
    }

    @Override
    public void initialize() {
        super.initialize();
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(()->{
            Map<String, Object> payload = new HashMap<>();
            payload.put("tick", tickCount++);
            payload.put("timestamp", System.currentTimeMillis());

            send("out", new Message(payload));
        }, 0 , intervalMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void shutdown() {
        if(scheduler != null){
            scheduler.shutdown();
        }
        super.shutdown();
    }

    @Override
    protected void onProcess(Message message) {

    }
}
