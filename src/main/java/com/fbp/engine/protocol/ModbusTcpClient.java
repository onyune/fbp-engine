package com.fbp.engine.protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import lombok.extern.slf4j.Slf4j;

/**
 * 외부 라이브러리 없이 Java Socket 통신을 이용하여 MODBUS TCP 프로토콜을 직접 구현한 클라이언트입니다.
 * 이 클래스는 MBAP 헤더와 PDU 프레임을 바이트 단위로 직접 조립하고 파싱하며,
 * 현재 FC 03(Read Holding Registers)과 FC 06(Write Single Register) 기능을 지원.
 */
@Slf4j
public class ModbusTcpClient {
    private  Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private int transactionId=0;

    private final String host;
    private final int port;

    /**
     * 클라이언트 인스턴스를 생성합니다.
     * 생성 시점에는 네트워크 연결을 맺지 않으며, 통신을 위해서는 {@link #connect()}를 호출해야 합니다.
     *
     * @param host 연결할 MODBUS 장비(슬레이브)의 IP 주소 또는 호스트명
     * @param port 연결할 MODBUS 장비의 포트 (일반적으로 502)
     */
    public ModbusTcpClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * 대상 장비와 TCP 소켓 연결을 수립하고, 데이터 입출력 스트림을 초기화합니다.
     * 무한 대기를 방지하기 위해 3초(3000ms)의 타임아웃이 설정됩니다.
     *
     * @throws IOException 연결에 실패하거나 스트림 생성 중 오류가 발생한 경우
     */
    public void connect() throws IOException {
        this.socket = new Socket(host,port);
        socket.setSoTimeout(3000);
        this.out = new DataOutputStream(socket.getOutputStream());
        this.in = new DataInputStream(socket.getInputStream());
    }

    /**
     * 연결된 소켓과 입출력 스트림 자원을 안전하게 해제합니다.
     * 자원 해제 중 발생하는 오류는 내부적으로 무시됩니다.
     */
    public void disconnect(){
        try{
            if(in != null) in.close();
            if(out != null) out.close();
            if(socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            log.warn("소켓, 데이터스트림 닫는 도중 에러");
        }
    }

    /**
     * 현재 장비와 정상적으로 소켓 연결이 유지되고 있는지 확인합니다.
     *
     * @return 연결되어 있으면 true, 끊어졌거나 연결 전이면 false
     */
    public boolean isConnected(){
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    /**
     * [FC 03] MODBUS 장비의 Holding Register(유지 레지스터) 값을 읽어옵니다.
     *
     * @param unitId 대상 슬레이브 장비의 식별 번호 (보통 1 또는 0xFF)
     * @param startAddress 읽기를 시작할 0 기반의 레지스터 메모리 주소
     * @param quantity 시작 주소로부터 읽어올 레지스터의 개수
     * @return 읽어온 레지스터 값들이 담긴 정수형 배열
     * @throws IOException 네트워크 통신 오류, 응답 타임아웃 또는 Transaction ID 불일치 시
     * @throws ModbusException 장비가 처리 불가를 알리는 에러 응답(Exception Code)을 반환한 경우
     */
    public int[] readHoldingRegisters(int unitId, int startAddress, int quantity) throws IOException, ModbusException {
        int currentTxId = transactionId++;

        buildMbapHeader(currentTxId, 6, unitId); // MBAP 조립

        out.writeByte(0x03); // Function code
        out.writeShort(startAddress); // 시작 주소
        out.writeShort(quantity); // 레지스터 개수
        out.flush(); // 전송

        // 응답 프레임 수신 및 파싱
        int respTxId = readMbapHeader();
        if(respTxId != currentTxId){
            throw new IOException("Transaction Id 불일치");
        }

        int respFc = in.readUnsignedByte();

        //에러 응답이면 ModbusException 발생
        if(respFc == (0x03 | 0x80)){
            int exceptionCode = in.readUnsignedByte();
            throw new ModbusException(0x03, exceptionCode);
        }

        int byteCount = in.readUnsignedByte();

        int[] registers = new int[quantity];
        for(int i = 0; i< quantity; i++){
            registers[i] = in.readUnsignedShort();
        }
        return registers;
    }

    /**
     * [FC 06] MODBUS 장비의 특정 Single Register에 단일 값을 기록합니다.
     *
     * @param unitId 대상 슬레이브 장비의 식별 번호
     * @param address 값을 기록할 0 기반의 레지스터 메모리 주소
     * @param value 레지스터에 기록할 16비트 값 (0 ~ 65535)
     * @throws IOException 네트워크 통신 오류 또는 에코백(Echo-back) 응답 불일치 시
     * @throws ModbusException 장비가 쓰기 불가를 알리는 에러 응답을 반환한 경우
     */
    public void writeSingleRegister(int unitId, int address, int value) throws IOException, ModbusException{
        int currentTxId = transactionId++;

        buildMbapHeader(currentTxId, 6, unitId);

        out.writeByte(0x06); // Function code
        out.writeShort(address); // 주소
        out.writeShort(value); // 값
        out.flush(); // 전송

        int respTxId = readMbapHeader();
        if(respTxId != currentTxId){
            throw new IOException("Transaction Id 불일치");
        }

        int respFc = in.readUnsignedByte();

        //에러 응답이면 ModbusException 발생
        if(respFc == (0x06 | 0x80)){
            int exceptionCode = in.readUnsignedByte();
            throw new ModbusException(0x06, exceptionCode);
        }

        int respAddress = in.readUnsignedShort();
        int respValue = in.readUnsignedShort();

        if(respAddress != address || respValue != value){
            throw new IOException("쓰기 실패: 에코백 검증 오류");
        }
    }

    /**
     * MODBUS TCP 프레임의 공통 부분인 MBAP 헤더(7바이트)를 스트림에 기록합니다.
     *
     * @param transactionId 요청과 응답을 매핑하기 위한 고유 식별자
     * @param length 이어지는 Unit ID와 PDU의 바이트 길이 합
     * @param unitId 슬레이브 장비 식별자
     * @throws IOException 스트림 쓰기 실패 시
     */
    private void buildMbapHeader(int transactionId, int length, int unitId) throws IOException{
        out.writeShort(transactionId); //2byte
        out.writeShort(0x0000); // modbus protocol id
        out.writeShort(length);
        out.writeByte(unitId); // 1byte
    }

    /**
     * 소켓 스트림에서 수신된 응답 프레임 중 MBAP 헤더 7바이트를 읽어 파싱합니다.
     *
     * @return 파싱된 응답 프레임의 Transaction ID
     * @throws IOException 스트림 읽기 실패 또는 타임아웃 시
     */
    private int readMbapHeader() throws IOException{
        int respTxId = in.readUnsignedShort(); //2byte (transactionId)
        int respProtocolId = in.readUnsignedShort();// 2byte (ProtocolId)
        int respLength = in.readUnsignedShort(); // 2byte length
        int respUnitId = in.readUnsignedByte(); // 1byte unitId
        return respTxId;
    }
}
