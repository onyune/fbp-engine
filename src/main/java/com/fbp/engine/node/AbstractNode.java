package com.fbp.engine.node;

import com.fbp.engine.core.InputPort;
import com.fbp.engine.core.OutputPort;
import com.fbp.engine.core.impl.DefaultInputPort;
import com.fbp.engine.core.impl.DefaultOutputPort;
import com.fbp.engine.message.Message;
import com.fbp.engine.metrics.MetricsCollector;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractNode implements Node{
    @Getter
    private final String id;

    @Setter
    @Getter
    private String flowId;


    public AbstractNode(String id) {
        this.id = id;
    }

    private final Map<String, InputPort> inputPorts = new HashMap<>();
    private final Map<String, OutputPort> outputPorts = new HashMap<>();

    protected void addInputPort(String name){
        inputPorts.put(name, new DefaultInputPort(name, this));
    }
    protected void addOutputPort(String name){
        outputPorts.put(name, new DefaultOutputPort(name));
    }

    public InputPort getInputPort(String name){
        return inputPorts.get(name);
    }

    public OutputPort getOutputPort(String name){
        return outputPorts.get(name);
    }

    protected void send(String portName, Message message){
        OutputPort port = outputPorts.get(portName);
        if(port!=null){
            port.send(message);
        }
    }

    @Override
    public final void process(Message message) {
        long startTime = System.currentTimeMillis();
        boolean success = true;
        try{
            onProcess(message);
        }catch (Exception e){
            success=false;
            throw e;
        }finally {
            long durationMs = System.currentTimeMillis() -startTime;
            String metricKey = (flowId != null ? flowId + ":" : "") + id;
            MetricsCollector.getInstance()
                    .recordProcessing(metricKey, durationMs, success);
        }
    }

    @Override
    public void initialize() {

    }

    @Override
    public void shutdown() {

    }

    protected abstract void onProcess(Message message);
}
