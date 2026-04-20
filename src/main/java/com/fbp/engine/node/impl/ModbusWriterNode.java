package com.fbp.engine.node.impl;

import com.fbp.engine.message.Message;
import com.fbp.engine.node.ProtocolNode;
import com.fbp.engine.protocol.ModbusTcpClient;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * input port : in
 * output port : result
 */
@Slf4j
public class ModbusWriterNode extends ProtocolNode {
    private ModbusTcpClient client;

    public ModbusWriterNode(String id, Map<String, Object> config) {
        super(id, config);
        addInputPort("in");
        addOutputPort("result"); // 1개 (쓰기 결과 전달, 선택)
    }

    @Override
    protected void connect() throws Exception {
        String host = (String) getConfig("host");
        int port = (int) getConfig("port");

        client = new ModbusTcpClient(host,port);
        client.connect();
    }

    @Override
    protected void disconnect() throws Exception {
        if(client!=null){
            client.disconnect();
        }
    }
    @Override
    protected void onProcess(Message message) {
        String valueField = (String) getConfig("valueField");
        int slaveId = (int) getConfig("slaveId");
        int registerAddress = (int) getConfig("registerAddress");
        Object scaleObj = getConfig("scale");

        double scale = (scaleObj instanceof Number) ? ((Number) scaleObj).doubleValue(): 1.0;

        try{
            Object rawValue = message.get(valueField);

            if(rawValue == null){
                return; // 필터가 없으면 더 이상 진행하지 않음
            }

            Number numValue = (Number) rawValue;
            int scaledValue = (int) Math.round(numValue.doubleValue() * scale);

            client.writeSingleRegister(slaveId, registerAddress, scaledValue);

            if(getOutputPort("result") != null){
                Map<String, Object> resultPayload = new HashMap<>();
                resultPayload.put("status", "success");
                resultPayload.put("writtenAddress", registerAddress);
                resultPayload.put("writtenValue", scaledValue);
                resultPayload.put("originalMessage", message.getPayload());

                send("result", new Message(resultPayload));
            }
        }catch (Exception e){
            log.error("MODBUS 레지스터 쓰기 실패 (주소: {}): {}", registerAddress, e.getMessage());
        }
    }

}
