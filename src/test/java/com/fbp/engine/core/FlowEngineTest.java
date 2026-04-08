package com.fbp.engine.core;

import static org.junit.jupiter.api.Assertions.*;

import com.fbp.engine.core.Flow.FlowState;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.AbstractNode;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FlowEngineTest {
    private final PrintStream standardOut = System.out;
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

    @BeforeEach
    void setUp(){
        System.setOut(new PrintStream(outputStreamCaptor));
    }

    @AfterEach
    void tearDown(){
        System.setOut(standardOut);
    }

    static class DummyNode extends AbstractNode{
        public DummyNode(String id){
            super(id);
            addInputPort("in");
            addOutputPort("out");
        }

        @Override
        protected void onProcess(Message message) {

        }
    }

    private Flow createValidFlow(String id){
        Flow flow = new Flow(id);
        flow.addNode(new DummyNode(id+"-n1"))
                .addNode(new DummyNode(id + "-n2"))
                .connect(id + "-n1", "out", id+"-n2", "in");
        return flow;
    }

    @Test
    @DisplayName("초기 상태")
    void test1_initialState(){
        FlowEngine engine = new FlowEngine();
        assertEquals("INITIALIZED", engine.getState().name());
    }

    @Test
    @DisplayName("플로우 등록")
    void test2_register(){
        FlowEngine engine = new FlowEngine();
        Flow flow = createValidFlow("test-flow");

        engine.register(flow);
        assertTrue(engine.getFlows().containsKey("test-flow"));
        assertEquals(flow, engine.getFlows().get("test-flow"));
    }

    @Test
    @DisplayName("startFlow 정상")
    void test3_startFlow(){
        FlowEngine engine = new FlowEngine();
        Flow flow = createValidFlow("test-flow");
        engine.register(flow);

        engine.startFlow("test-flow");

        assertEquals("RUNNING", engine.getState().name());
        assertEquals(FlowState.RUNNING, flow.getState());
    }

    @Test
    @DisplayName("startFlow — 없는 ID")
    void test4_startFlowInvalidId(){
        FlowEngine engine = new FlowEngine();

        assertThrows(IllegalArgumentException.class, ()->{
            engine.startFlow("ghost-flow");
        });
    }

    @Test
    @DisplayName("startFlow — 유효성 실패")
    void test5_startFlowValidationFail(){
        FlowEngine engine = new FlowEngine();
        Flow invalidFlow = new Flow("invalid-flow");
        engine.register(invalidFlow);

        assertThrows(IllegalStateException.class, ()->{
            engine.startFlow("invalid-flow");
        });
    }

    @Test
    @DisplayName("stopFlow 정상")
    void test6_stopFlow(){
        FlowEngine engine = new FlowEngine();
        Flow flow = createValidFlow("test-flow");

        engine.register(flow);
        engine.startFlow("test-flow");
        engine.stopFlow("test-flow");

        assertEquals(FlowState.STOPPED, flow.getState());
    }

    @Test
    @DisplayName("shutdown 전체")
    void test7_shutdown(){
        FlowEngine engine = new FlowEngine();
        Flow flow1 = createValidFlow("flow1");
        Flow flow2 = createValidFlow("flow2");

        engine.register(flow1);
        engine.register(flow2);

        engine.startFlow("flow1");
        engine.startFlow("flow2");

        engine.shutdown();

        assertEquals("STOPPED", engine.getState().name());
        assertEquals(FlowState.STOPPED, flow1.getState());
        assertEquals(FlowState.STOPPED, flow2.getState());
    }

    @Test
    @DisplayName("다중 플로우 독립 동작")
    void test8_independentFlowOperation(){
        FlowEngine engine = new FlowEngine();
        Flow flow1 = createValidFlow("flow1");
        Flow flow2 = createValidFlow("flow2");

        engine.register(flow1);
        engine.register(flow2);

        engine.startFlow("flow1");
        engine.startFlow("flow2");

        engine.stopFlow("flow1");

        assertEquals(FlowState.STOPPED, flow1.getState());
        assertEquals(FlowState.RUNNING, flow2.getState());
    }

    @Test
    @DisplayName("listFlows 출력")
    void test9_listFlows(){
        FlowEngine engine =new FlowEngine();
        engine.register(createValidFlow("flowA"));
        engine.register(createValidFlow("flowB"));

        assertDoesNotThrow(()-> engine.listFlows());

        String output = outputStreamCaptor.toString();
        if(!output.isEmpty()){
            assertTrue(output.contains("flowA"));
            assertTrue(output.contains("flowB"));
        }
    }
}