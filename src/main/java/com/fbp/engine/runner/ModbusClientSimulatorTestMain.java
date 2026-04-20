package com.fbp.engine.runner;

import com.fbp.engine.protocol.ModbusTcpClient;
import com.fbp.engine.protocol.ModbusTcpSimulator;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;

//stage2 step3-5
@Slf4j
public class ModbusClientSimulatorTestMain {
    public static void main(String[] args) {
        ModbusTcpSimulator simulator = new ModbusTcpSimulator(5020, 10);
        simulator.start();

        simulator.setRegister(0, 250);
        simulator.setRegister(1, 600);
        simulator.setRegister(2, 1);

        ModbusTcpClient client = new ModbusTcpClient("localhost", 5020);

        try{
            client.connect();

            // 주소 0번부터 3개의 레지스트 읽기 (FC 03)
            int slaveId = 1;
            int[] values = client.readHoldingRegisters(slaveId, 0, 3);
            log.info("레지스터 읽기 결과: "+ Arrays.toString(values));

            // 주소 2번에 값 100 쓰기 (FC 06)
            client.writeSingleRegister(slaveId,2,100);

            int[] updatesValues = client.readHoldingRegisters(slaveId,2,1);
            System.out.println("주소 2번 다시 읽기 결과: "+Arrays.toString(updatesValues));


        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            client.disconnect();
            simulator.stop();
        }
    }
}
