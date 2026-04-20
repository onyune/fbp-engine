package com.fbp.engine.node.impl;

import com.fbp.engine.message.Message;
import com.fbp.engine.node.ProtocolNode;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EchoProtocolNode extends ProtocolNode {
    private Socket socket;
    private InputStream in;
    private OutputStream out;

    public EchoProtocolNode(String id, Map<String, Object> config) {
        super(id, config);
    }

    @Override
    protected void connect() throws Exception {
        String host = (String) getConfig("host");
        if(host==null){
            host= "localhost";
        }

        Object portObj = getConfig("port");
        int port  = portObj instanceof Number ? ((Number) portObj).intValue() : 8080;

        this.socket = new Socket(host,port);
        this.out= socket.getOutputStream();
        this.in = socket.getInputStream();
    }

    @Override
    protected void disconnect() throws Exception {
        if (in != null) {
            in.close();
        }
        if (out != null) {

            out.close();
        }

        if(socket!=null && !socket.isClosed()){
            socket.close();
        }
    }

    @Override
    protected void onProcess(Message message) {

    }
}
