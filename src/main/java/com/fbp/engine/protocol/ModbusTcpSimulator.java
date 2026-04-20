package com.fbp.engine.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ModbusTcpSimulator {
    private final int port;
    private ServerSocket serverSocket;
    private final int[] registers;
    private volatile boolean running;

    /**
     * 시뮬레이터 인스턴스를 생성한다
     *
     * @param port          연결을 대기할 포트 번호
     * @param registerCount 메모리에 할당할 가상 레지스터의 총 개수
     */
    public ModbusTcpSimulator(int port, int registerCount) {
        this.port = port;
        this.registers = new int[registerCount];
        this.running = false;
    }

    public void start() {
        if(running) return;
        running=true;

        new Thread(()->{
            try{
                serverSocket = new ServerSocket(port);
                log.info("[Simulator] 클라이언트 시작됨 (포트:{})", port);
                while(running){
                    try{
                        Socket clientSocket = serverSocket.accept();
                        log.info("[Simulator] 클라이언트 접속됨: "+ clientSocket.getRemoteSocketAddress());
                        new Thread(()-> handleClient(clientSocket)).start();
                    } catch (IOException e) {
                        if(running){
                            log.error("[Simulator] 클라이언트 연결 수락 중 오류: {}", e.getMessage());
                        }
                    }
                }
            }catch (IOException e){
                log.error("[Simulator] 서버 소켓 생성 실패: {}", e.getMessage());
            }
        }).start();
    }

    /**
     * 시뮬레이터를 종료하고 열려있는 서버 소켓을 닫는다.
     */
    public void stop(){
        running=false;
        try{
            if(serverSocket !=null  && !serverSocket.isClosed()){
                serverSocket.close();
                log.info("[Simulator] MODBUS 시뮬레이터 종료");
            }
        } catch (IOException e) {
            //에러 무시
        }
    }

    /**
     * 연결된 클라이언트와 통신을 처리합니다.
     *
     * @param socket 연결된 클라이언트 소켓
     */
    private void handleClient(Socket socket) {
        try (socket;
             DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

            while (running && !socket.isClosed()) {
                // MBAP 헤더 수신 (7바이트)
                int txId = in.readUnsignedShort(); //transaction id
                in.readUnsignedShort(); // Protocol ID
                in.readUnsignedShort(); // Length
                int unitId = in.readUnsignedByte();

                // Function Code 수신
                int fc = in.readUnsignedByte();

                // 기능 분기
                if (fc == 0x03) {
                    handleReadHoldingRegisters(in, out, txId, unitId, fc);
                } else if (fc == 0x06) {
                    handleWriteSingleRegister(in, out, txId, unitId, fc);
                } else {
                    // 지원하지 않는 Function Code (Exception 0x01: ILLEGAL_FUNCTION)
                    sendErrorResponse(out, txId, unitId, fc, 0x01);
                }
            }
        } catch (IOException e) {
            System.out.println("[Simulator] 클라이언트 연결 해제");
        }
    }

    private void handleReadHoldingRegisters(DataInputStream in, DataOutputStream out, int txId, int unitId, int fc) throws IOException {
        int startAddress = in.readUnsignedShort();
        int quantity = in.readUnsignedShort();

        // 에러: 범위 초과 (Exception 0x02: ILLEGAL_DATA_ADDRESS)
        if (startAddress < 0 || startAddress + quantity > registers.length) {
            sendErrorResponse(out, txId, unitId, fc, 0x02);
            return;
        }

        // 정상 응답 조립 (MBAP + FC + ByteCount + 데이터)
        out.writeShort(txId);
        out.writeShort(0x0000);
        out.writeShort(3 + (quantity * 2)); // Length: Unit ID(1) + FC(1) + ByteCount(1) + 데이터 길이
        out.writeByte(unitId);

        out.writeByte(fc);
        out.writeByte(quantity * 2); // Byte Count

        // 레지스터 값 전송
        for (int i = 0; i < quantity; i++) {
            out.writeShort(registers[startAddress + i]);
        }
        out.flush();
    }

    private void handleWriteSingleRegister(DataInputStream in, DataOutputStream out, int txId, int unitId, int fc) throws IOException {
        int address = in.readUnsignedShort();
        int value = in.readUnsignedShort();

        // 에러: 존재하지 않는 주소 (Exception 0x02: ILLEGAL_DATA_ADDRESS)
        if (address < 0 || address >= registers.length) {
            sendErrorResponse(out, txId, unitId, fc, 0x02);
            return;
        }

        // 값 기록
        registers[address] = value;
        System.out.println("[Simulator] 레지스터 쓰기 발생 -> 주소: " + address + ", 값: " + value);

        // 에코백 응답 (요청과 동일한 값을 그대로 전송)
        out.writeShort(txId);
        out.writeShort(0x0000);
        out.writeShort(6); // Length: Unit ID(1) + FC(1) + 주소(2) + 값(2)
        out.writeByte(unitId);

        out.writeByte(fc);
        out.writeShort(address);
        out.writeShort(value);
        out.flush();
    }

    /**
     * MODBUS 에러 응답 프레임을 전송합니다.
     */
    private void sendErrorResponse(DataOutputStream out, int txId, int unitId, int fc, int exceptionCode) throws IOException {
        out.writeShort(txId);
        out.writeShort(0x0000);
        out.writeShort(3); // Length: Unit ID(1) + 에러FC(1) + ExceptionCode(1)
        out.writeByte(unitId);
        out.writeByte(fc | 0x80); // 에러 응답은 Function Code의 최상위 비트를 1로 세팅 (+0x80)
        out.writeByte(exceptionCode);
        out.flush();
    }

    /**
     * 시뮬레이터의 특정 레지스터 값을 임의로 설정합니다. (테스트용)
     */
    public void setRegister(int address, int value) {
        if (address >= 0 && address < registers.length) {
            registers[address] = value;
        }
    }

    /**
     * 시뮬레이터의 특정 레지스터 값을 확인합니다. (테스트 검증용)
     */
    public int getRegister(int address) {
        if (address >= 0 && address < registers.length) {
            return registers[address];
        }
        throw new IllegalArgumentException("잘못된 레지스터 주소입니다.");
    }
}
