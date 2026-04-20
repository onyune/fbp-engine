package com.fbp.engine.protocol;


public class ModbusException extends Exception {
    private static final int ILLEGAL_FUNCTION=0x01;
    private static final int ILLEGAL_DATA_ADDRESS = 0x02;
    private static final int ILLEGAL_DATA_VALUE = 0x03;
    private static final int SLAVE_DEVICE_FAILURE = 0x04;

    private final int functionCode;
    private final int exceptionCode;

    public ModbusException(int functionCode, int exceptionCode){
        super(String.format("MODBUS 에러 — FC: 0x%02X, Exception: 0x%02X (%s)",
                functionCode, exceptionCode, getExceptionMessage(exceptionCode)));
        this.exceptionCode=exceptionCode;
        this.functionCode=functionCode;
    }
    public int getExceptionCode() {
        return exceptionCode;
    }

    private static String getExceptionMessage(int code) {
        switch (code) {
            case ILLEGAL_FUNCTION: return "Illegal Function";
            case ILLEGAL_DATA_ADDRESS: return "Illegal Data Address";
            case ILLEGAL_DATA_VALUE: return "Illegal Data Value";
            case SLAVE_DEVICE_FAILURE: return "Slave Device Failure";
            default: return "Unknown Error";
        }
    }

}
