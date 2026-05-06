package com.fbp.engine.node;

import com.fbp.engine.message.Message;
import lombok.Getter;

@Getter
public class RoutingRule {
    private final String field; //검사할 메시지의 키
    private final String operator; // 연산자
    private final Object targetValue; // 비교할 값
    private final String targetPort; // 조건이 참일 때 전송할 출력 포트 이름

    public RoutingRule(String field, String operator, Object targetValue, String targetPort) {
        this.field = field;
        this.operator = operator;
        this.targetValue = targetValue;
        this.targetPort = targetPort;
    }

    public boolean evaluate(Message message){
        if(!message.hasKey(field)){
            return false;
        }

        Object value = message.get(field);
        if(value==null) return false   ;

        switch (operator){
            case "==": return value.equals(targetValue);
            case "!=": return !value.equals(targetValue);
            case ">": {
                if(value instanceof Number && targetValue instanceof Number){
                    return ((Number) value).doubleValue() >  ((Number) targetValue).doubleValue();
                }
                return false;
            }
            case "<":{
                if(value instanceof Number && targetValue instanceof Number){
                    return ((Number) value).doubleValue() <  ((Number) targetValue).doubleValue();
                }
                return false;
            }
            default:
                throw  new IllegalArgumentException("지원하지 않는 연산자입니다: " + operator);
        }
    }
}
