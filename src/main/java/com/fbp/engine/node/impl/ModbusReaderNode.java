package com.fbp.engine.node.impl;

import com.fbp.engine.message.Message;
import com.fbp.engine.node.ProtocolNode;
import com.fbp.engine.protocol.ModbusException;
import com.fbp.engine.protocol.ModbusTcpClient;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * input port : trigger
 * output port : out, error
 */
@Slf4j
public class ModbusReaderNode extends ProtocolNode {
    private ModbusTcpClient client;

    public ModbusReaderNode(String id, Map<String, Object> config) {

        super(id, config);
        addInputPort("trigger");
        addOutputPort("out");
        addOutputPort("error");
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
        int slaveId = (int) getConfig("slaveId");
        int startAddress = (int) getConfig("startAddress");
        int count = (int) getConfig("count");

        try{
            int[] values = client.readHoldingRegisters(slaveId,startAddress,count);

            Map<String, Object> payload = new HashMap<>();
            Map<String, Object> mapping = (Map<String, Object>) getConfig("registerMapping");

            if(mapping!=null){
                for(int i = 0 ; i< count; i++){
                    String addressKey = String.valueOf(startAddress + i);
                    if(mapping.containsKey(addressKey)){
                        Map<String, Object> mapInfo = (Map<String, Object>) mapping.get(addressKey);
                        String name = (String) mapInfo.get("name");

                        Number scaleNum = (Number) mapInfo.getOrDefault("scale", 1.0);
                        double scale =  scaleNum.doubleValue();

                        payload.put(name,values[i] * scale);
                    }else {
                        payload.put("register_"+addressKey,values[i]);
                    }
                }
            }else{
                payload.put("slaveId", slaveId);
                Map<String,Integer> registers = new HashMap<>();
                for(int i = 0 ; i<count;i++){
                    registers.put(String.valueOf(startAddress+i), values[i]);
                }
                payload.put("registers", registers);
            }

            payload.put("timestamp", System.currentTimeMillis());
            send("out", new Message(payload));
        }catch (IOException  | ModbusException ex){
            Map<String, Object> payload = new HashMap<>();
            payload.put("error", ex.getMessage());
            payload.put("triggerMessage", message.getPayload());
            send("error", new Message(payload));
        }
    }
}
