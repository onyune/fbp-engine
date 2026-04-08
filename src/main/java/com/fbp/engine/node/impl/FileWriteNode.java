package com.fbp.engine.node.impl;

import com.fbp.engine.exception.NodeProcessException;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.AbstractNode;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Objects;

/**
 * input port : in
 * output port : X
 */
public class FileWriteNode extends AbstractNode {
    private final String filePath;
    private BufferedWriter writer;

    public FileWriteNode(String id, String filePath) {
        super(id);
        this.filePath = filePath;
        addInputPort("in");
    }

    @Override
    public void initialize() {
        super.initialize();
        try{
            this.writer = new BufferedWriter(new FileWriter(filePath, true));
        } catch (IOException e) {
            throw new NodeProcessException("[" + getId() + "] 파일 열기 실패: " + filePath, e);
        }
    }

    @Override
    public void shutdown() {
        if(Objects.nonNull(writer)){
            try {
                writer.close();
            } catch (IOException e) {
                throw new NodeProcessException("[" + getId() + "] 파일 닫기 실패: " + filePath, e);
            }
        }
        super.shutdown();
    }

    @Override
    protected void onProcess(Message message) {
        String messageToString = message.toString();
        try{
            writer.write(messageToString);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            throw new NodeProcessException("[" + getId() + "] 파일 쓰기 실패: " + filePath, e);
        }
    }
}
