package com.fbp.engine.registry;

import com.fbp.engine.node.Node;
import java.util.Map;

@FunctionalInterface //함수형 인터페이스 (추상메서드가 1개만 존재해야함, static이나 default는 상관없이 사용가능)
//이 어노테이션의 유명한 사용처는 Runnable.. 람다 함수 처럼 쓰기 위한 거 NodeRegistry에 매개변수로 NodeFactory를 넣으면 Thread 만들때 Runnable 란에
//람다 함수로 만들어서 넣은 것처럼 쓸 수 있음
public interface NodeFactory {
    Node create(Map<String, Object> config);
}
