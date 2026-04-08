# Flow-based Programming을 이용한 IoT Rule Engine 개발 교육 과정

## 1. 과정 개요 (Syllabus)

### 교육 목표

Flow-based Programming(FBP) 패러다임을 이해하고, Java로 IoT 환경에서 동작하는 Rule Engine을 직접 설계·구현한다. 수준별로 도달 목표가 다르지만, 기본 학습 과정(Step 1~10)은 동일한 구조를 공유한다.

| 구분 | 도달 목표 |
|------|-----------|
| 1단계 | FBP 기본 엔진 구현 (노드 정의, 포트 연결, 메시지 전달) |
| 2단계 | 1단계 완료 후, 외부 통신 프로토콜 연결 (MQTT, MODBUS) |
| 3단계 | 자유로운 플로우 구성이 가능한 확장형 엔진 설계 |

### 교육 기간 및 구성

- **총 기간**: 15일 (주 5일 기준)
- **Step 1~10 (10일)**: 개별 기술 습득 — 팀별 학습 후 미팅을 통해 학습 내용 검토
- **Step 11~15 (5일)**: 팀 프로젝트 — 수준별 최종 산출물 개발 및 발표

### 교육 방식

Step 1~10은 엔진 구현에 필요한 개별 기술을 습득하는 기간이다. 팀별로 학습한 뒤 정기 미팅에서 학습 내용을 검토하고 서로 피드백한다.

- **1단계**: 일별 학습 가이드를 제공한다. 무엇을 해야 하는지, 구조를 어떻게 설계해야 하는지 단계별로 안내한다.
- **2단계**: 주제와 참고 자료를 제시하고, 구현 방향은 팀 내에서 자율적으로 결정한다.
- **3단계**: 목표만 제시하고, 설계·구현·검증 전 과정을 자율적으로 수행한다.

### 공통 기반 기술 (전 수준 동일)

모든 수준의 학생이 아래 기반 기술을 학습한다.

1. **FBP 핵심 개념**: 컴포넌트(Node), 포트(Port), 연결(Connection/Edge), 정보 패킷(IP)
2. **Java 심화**: 인터페이스, 추상 클래스, 제네릭, 컬렉션, 스레드 (기본 문법·클래스·생성자·메서드는 습득 전제)
3. **설계 패턴**: Observer, Strategy, Builder, Graph 자료구조
4. **개발 환경**: JDK 21, Maven, Git, JUnit 5

---

## 2. 공통 일과 구조

### Step별 일과 구조 (Step 1~10)

| 시간 | 활동 |
|------|------|
| 오전 (6h) | 개별/팀별 학습 및 구현 |
| 오후 전반 (2h) | 팀별 학습 내용 정리 및 코드 리뷰 |

---

## 3. 1단계 커리큘럼

> **목표**: Step 10 완료 후 FBP 기본 엔진(노드 생성, 포트 연결, 메시지 흐름)을 Java로 구현할 수 있다.
>
> **특징**: Step별 상세 가이드 제공. 무엇을 학습하고, 어떤 코드를 작성해야 하는지 단계별로 안내한다.
>
> **전제**: Java 기본 문법(변수, 조건문, 반복문, 배열), 클래스, 생성자, 메서드 개념은 이미 습득한 상태로 시작한다.

#### Step 1 — 개발 환경 구축 & FBP 개념 도입

- **학습 목표**: 개발 환경을 세팅하고, FBP의 핵심 개념을 이해한다.
- **할 일**:
  - JDK 21, IntelliJ IDEA (또는 VS Code) 설치
  - Maven 프로젝트 생성 (`fbp-engine`)
  - FBP 정의 학습: J. Paul Morrison의 FBP 개념
  - 핵심 요소 정리: Component(Node), Port(In/Out), Connection(Edge), Information Packet(IP)
  - 기존 FBP 시스템 사례 조사 (Node-RED, Apache NiFi 등의 아키텍처 분석)
- **실습 과제**: FBP 엔진의 전체 아키텍처를 다이어그램으로 그리고, 핵심 클래스 목록을 정리한다.
- **설계 가이드**: 프로젝트 패키지 구조를 아래와 같이 생성한다.
  ```
  src/main/java/
  └── com.fbp.engine/
      ├── core/       ← 엔진 핵심 (Node, Port, Flow 등)
      ├── node/       ← 구체적 노드 구현체
      ├── message/    ← 메시지(IP) 정의
      └── runner/     ← 엔진 실행기
  ```
  아래 핵심 클래스를 식별한다.
  - `Node` — 처리 단위
  - `Port` — 노드의 입출력 지점
  - `Connection` — 포트 간 연결 (메시지 전달 통로)
  - `Flow` — 노드와 연결의 집합 (실행 단위)
  - `FlowEngine` — 플로우를 로드하고 실행

#### Step 2 — 인터페이스·제네릭 복습 & Node/Message 설계

- **학습 목표**: 인터페이스와 제네릭을 복습하고, FBP의 기본 구성요소인 Node와 Message를 설계한다.
- **할 일**:
  - 인터페이스 정의와 구현 (`implements`), 제네릭 타입 복습
  - `Node` 인터페이스 설계 및 `PrintNode` 구현
  - `Message` 클래스 설계 (key-value 페이로드)
- **실습 과제**: `Node` 인터페이스와 `Message` 클래스를 구현하고, `PrintNode`가 Message를 받아 콘솔에 출력하는 코드 작성
- **설계 가이드**:
  ```java
  public interface Node {
      String getId();
      void process(Message message);
  }

  public class Message {
      private final String id;
      private final Map<String, Object> payload;
      private final long timestamp;
      // ...
  }
  ```

#### Step 3 — Port와 Connection 설계

- **학습 목표**: 노드 간 데이터 전달의 핵심인 Port와 Connection을 설계한다.
- **할 일**:
  - `InputPort`, `OutputPort` 인터페이스 정의
  - `Connection` 클래스 설계 (큐 기반 메시지 버퍼)
  - OutputPort → Connection → InputPort 연결 구조 이해
- **실습 과제**: 두 노드(`GeneratorNode` → `PrintNode`)를 Connection으로 연결하고, 메시지를 수동으로 전달하는 코드 작성
- **설계 가이드**:
  ```java
  public interface InputPort {
      void receive(Message message);
  }

  public interface OutputPort {
      void send(Message message);
      void connect(Connection connection);
  }

  public class Connection {
      private final Queue<Message> buffer;
      private InputPort target;
      // ...
  }
  ```

#### Step 4 — 스레드 기초와 동시성

- **학습 목표**: Java 스레드의 기본 개념과 동시성 제어를 익힌다.
- **할 일**:
  - `Thread`, `Runnable`, `ExecutorService` 이해
  - `synchronized`, `BlockingQueue` 학습
  - Connection 버퍼를 `LinkedBlockingQueue`로 변경
- **실습 과제**: 두 노드를 별도 스레드에서 실행하고 Connection을 통해 메시지를 비동기 전달한다.

#### Step 5 — 추상 클래스·설계 패턴과 AbstractNode

- **학습 목표**: 추상 클래스와 설계 패턴을 이해하고, 노드의 공통 로직을 추출한다.
- **할 일**:
  - 추상 클래스 개념 복습, Observer/Strategy 패턴 학습
  - `AbstractNode` 추상 클래스 작성 (공통 포트 관리, 생명주기)
  - `Node` 인터페이스 확장: `initialize()`, `process()`, `shutdown()`
- **실습 과제**: `AbstractNode`를 상속하여 `TimerNode`(주기적 메시지 생성)와 `FilterNode`(조건 필터링)을 구현한다.
- **설계 가이드**:
  ```java
  public abstract class AbstractNode implements Node {
      private String id;
      private Map<String, InputPort> inputPorts;
      private Map<String, OutputPort> outputPorts;

      public abstract void onProcess(Message message);

      @Override
      public void process(Message message) {
          // 공통 전처리
          onProcess(message);
          // 공통 후처리
      }
  }
  ```

#### Step 6 — 기본 노드 구현 및 플로우 연결

- **학습 목표**: 다양한 기본 노드를 구현하고, 노드 간 연결을 통해 데이터가 흐르는 것을 확인한다.
- **할 일**:
  - 기본 노드 3종 구현: `TimerNode`(주기적 메시지 생성), `FilterNode`(조건 필터링), `LogNode`(로깅)
  - 노드 간 Connection 연결 및 메시지 흐름 확인
- **실습 과제**: TimerNode → FilterNode → LogNode를 연결하여 조건에 맞는 메시지만 로깅하는 플로우 작성

#### Step 7 — Flow 클래스와 그래프 구조

- **학습 목표**: 복수의 노드와 연결을 하나의 실행 단위(Flow)로 관리한다.
- **할 일**:
  - `Flow` 클래스 설계: 노드 등록, 연결 정의, 실행 순서 결정
  - 방향 그래프(DAG) 개념 이해
  - 플로우 유효성 검증: 순환 참조 탐지, 미연결 포트 검출
- **실습 과제**: 3개 이상의 노드를 포함하는 Flow를 구성하고, 그래프 순회로 실행 순서를 출력한다.

#### Step 8 — FlowEngine 구현

- **학습 목표**: Flow를 로드하고 실행하는 엔진을 구현한다.
- **할 일**:
  - `FlowEngine` 클래스: Flow 등록, 시작(`start`), 정지(`stop`)
  - 각 노드를 별도 스레드로 실행하는 `ExecutorService` 활용
  - 엔진의 상태 관리: `INITIALIZED`, `RUNNING`, `STOPPED`
- **실습 과제**: FlowEngine을 통해 플로우를 실행하고, 콘솔에서 메시지 흐름을 확인한다.

#### Step 9 — IoT 시나리오 적용 및 추가 노드 구현

- **학습 목표**: IoT 도메인에 맞는 노드를 추가 구현하고 실제 시나리오를 시뮬레이션한다.
- **할 일**:
  - `TemperatureSensorNode`(랜덤 온도 생성), `ThresholdFilterNode`(임계값 필터), `AlertNode`(경고 출력) 구현
  - `TransformNode`(데이터 변환), `SplitNode`(분기) 등 유틸리티 노드 추가
  - IoT 시나리오 플로우 구성 및 실행
- **실습 과제**: "온도 모니터링" 플로우를 구성하여 시연한다.
  ```
  [TimerNode] → [TemperatureSensorNode] → [ThresholdFilterNode] → [AlertNode]
       (1초 주기)    (랜덤 온도 생성)       (30도 초과 필터링)    (콘솔 경고 출력)
  ```

#### Step 10 — 통합 테스트와 리팩토링

- **학습 목표**: 구현한 엔진을 통합 테스트하고, 코드 품질을 개선한다.
- **할 일**:
  - JUnit 5를 이용한 단위 테스트 작성 (Node, Connection, Flow, Engine)
  - 전체 플로우 End-to-End 테스트
  - 코드 리팩토링 및 문서화
- **실습 과제**: 모든 테스트를 통과하는 안정적인 기본 FBP 엔진을 완성한다.

---

## 4. 2단계 커리큘럼

> **목표**: 기본 FBP 엔진에 MQTT, MODBUS 등 외부 통신 프로토콜을 연결하여 실제 IoT 장비와 통신할 수 있는 Rule Engine을 구현한다.
>
> **특징**: 주제와 참고 자료를 제시하며, 구현 방향은 팀 자율 결정. 1단계 완료 후 진행한다.
>
> **전제**: 1단계(Step 1~10)를 완료하여 기본 FBP 엔진이 구현된 상태에서 시작한다.

#### Step 1 — 네트워크 프로그래밍 기초

- **주제**: Java NIO, Socket 프로그래밍, 비동기 I/O 패턴
- **참고**: `java.nio.channels`, Netty 프레임워크 개요
- **팀 토의**: 프로토콜 노드의 인터페이스를 어떻게 설계할 것인가?

#### Step 2 — MQTT 프로토콜 이해와 연결

- **주제**: MQTT 프로토콜 구조 (Broker, Topic, QoS, Publish/Subscribe)
- **참고**: Eclipse Paho MQTT Client 라이브러리
- **구현 목표**:
  - `MqttSubscriberNode`: 특정 Topic을 구독하여 메시지를 수신하고 FBP 메시지로 변환
  - `MqttPublisherNode`: FBP 메시지를 받아 MQTT Broker로 발행
- **팀 토의**: QoS 레벨 선택 기준, 재연결 전략

#### Step 3 — MODBUS 프로토콜 이해와 연결

- **주제**: MODBUS RTU/TCP 프로토콜, 레지스터 읽기/쓰기
- **참고**: j2mod 또는 EasyModbus 라이브러리
- **구현 목표**:
  - `ModbusReaderNode`: MODBUS 디바이스에서 레지스터 값을 읽어 FBP 메시지로 변환
  - `ModbusWriterNode`: FBP 메시지의 값을 MODBUS 디바이스 레지스터에 기록
- **팀 토의**: 폴링 주기 설정, 에러 핸들링 전략

#### Step 4 — 프로토콜 노드 통합 및 Rule 처리

- **주제**: MQTT/MODBUS 노드를 엔진에 통합하고, 규칙 기반 처리 플로우 구성
- **구현 목표**:
  - 프로토콜 노드를 기존 엔진에 등록하고 실행
  - `RuleNode`: 조건식을 평가하여 분기 처리 (예: 온도 > 30 → 알림, 아니면 무시)
  - 통합 플로우 예시:
    ```
    [MqttSubscriber] → [RuleNode] → [ModbusWriter]
                                  → [MqttPublisher (알림)]
    ```
- **팀 토의**: 규칙 표현 방식 (하드코딩 vs 설정 파일 vs 스크립트)

#### Step 5 — 통합 테스트 및 시뮬레이션

- **주제**: Mosquitto 브로커와 MODBUS 시뮬레이터를 이용한 End-to-End 테스트
- **구현 목표**:
  - 로컬 MQTT Broker(Mosquitto) 구동 및 연결 테스트
  - MODBUS TCP 시뮬레이터를 이용한 레지스터 읽기/쓰기 테스트
  - 전체 플로우의 동작 검증 및 성능 측정
  - 에러 상황 시뮬레이션 (연결 끊김, 타임아웃)

---

## 5. 3단계 커리큘럼

> **목표**: 사용자가 JSON/YAML 등의 설정만으로 자유롭게 플로우를 구성하고, 커스텀 노드를 플러그인 형태로 추가할 수 있는 확장형 FBP 엔진을 설계·구현한다.
>
> **특징**: 목표만 제시. 설계, 구현, 검증 전 과정을 자율 수행한다.

#### Step 1~3 — 기본 엔진 고속 구현 + 아키텍처 설계

- **목표**:
  - 1단계 Step 1~10 분량의 기본 엔진을 3일 내 구현 완료
  - 확장 가능한 아키텍처 설계: 플러그인 시스템, 동적 플로우 구성, 설정 기반 실행
- **핵심 설계 과제**:
  - **노드 레지스트리**: 노드 타입을 동적으로 등록/조회하는 시스템
  - **플로우 정의 포맷**: JSON 또는 YAML로 플로우를 선언적으로 정의
  - **플러그인 아키텍처**: ServiceLoader 또는 커스텀 ClassLoader를 활용한 노드 플러그인

#### Step 4~5 — 설정 기반 플로우 엔진

- **목표**:
  - JSON/YAML 파일에서 플로우 정의를 읽어 자동으로 노드를 생성하고 연결하는 `FlowParser` 구현
  - 플로우 정의 예시:
    ```json
    {
      "id": "temperature-monitoring",
      "nodes": [
        {"id": "sensor", "type": "MqttSubscriber", "config": {"topic": "sensor/temp"}},
        {"id": "rule", "type": "ThresholdFilter", "config": {"field": "value", "threshold": 30}},
        {"id": "alert", "type": "MqttPublisher", "config": {"topic": "alert/temp"}}
      ],
      "connections": [
        {"from": "sensor:out", "to": "rule:in"},
        {"from": "rule:out", "to": "alert:in"}
      ]
    }
    ```
  - 런타임에 플로우 추가/수정/삭제가 가능한 관리 인터페이스

#### Step 6~7 — 플러그인 시스템과 커스텀 노드

- **목표**:
  - Java `ServiceLoader`를 활용한 플러그인 메커니즘 구현
  - 외부 JAR 파일로 커스텀 노드를 추가할 수 있는 구조
  - 노드 개발 SDK 정의: 개발자가 인터페이스만 구현하면 엔진에 등록 가능

#### Step 8 — 모니터링과 관리 API

- **목표**:
  - 엔진 상태 모니터링: 노드별 처리량, 큐 크기, 에러 수 등 메트릭 수집
  - REST API (또는 간단한 HTTP 서버)를 통한 엔진 제어
    - `GET /flows` — 현재 실행 중인 플로우 목록
    - `POST /flows` — 새 플로우 배포
    - `DELETE /flows/{id}` — 플로우 중지/삭제
    - `GET /flows/{id}/metrics` — 플로우 메트릭 조회

#### Step 9 — 고급 플로우 패턴

- **목표**:
  - **서브플로우(Sub-flow)**: 플로우 안에 플로우를 내포하여 재사용
  - **동적 라우팅**: 메시지 내용에 따라 런타임에 경로 결정
  - **에러 핸들링 플로우**: 에러 발생 시 별도 플로우로 분기
  - **백프레셔(Backpressure)**: 처리 속도 차이에 따른 흐름 제어

#### Step 10 — 통합 검증 및 성능 최적화

- **목표**:
  - 복합 시나리오 테스트: MQTT 입력 → 다중 규칙 분기 → MODBUS 출력 + 알림
  - 부하 테스트: 초당 1,000건 이상의 메시지 처리 성능 검증
  - 메모리 누수 점검, 스레드 풀 최적화
  - 아키텍처 문서화

---

## 6. 팀 프로젝트 (Step 11~15)

### 프로젝트 개요

Step 1~10에서 학습한 내용을 바탕으로 수준별 팀 프로젝트를 수행한다. 각 팀은 실제 IoT 시나리오를 선정하고, FBP Rule Engine을 활용하여 해결한다.

### 공통 요구사항: CLI 기반 엔진 관리

모든 수준의 프로젝트에 **CLI(Command Line Interface)를 통한 엔진 관리 기능**을 포함한다. 수준에 따라 구현 범위가 다르다.

#### CLI 기능 목록

| 기능 영역 | 명령어 예시 | 설명 | 1단계 | 2단계 | 3단계 |
|-----------|------------|------|:----:|:----:|:----:|
| **플로우 관리** | `flow list` | 등록된 플로우 목록 조회 | ● | ● | ● |
| | `flow deploy <file>` | JSON/YAML 파일에서 플로우 로드 및 배포 | — | ● | ● |
| | `flow start <id>` | 플로우 시작 | ● | ● | ● |
| | `flow stop <id>` | 플로우 정지 | ● | ● | ● |
| | `flow restart <id>` | 플로우 재시작 | — | ● | ● |
| | `flow remove <id>` | 플로우 삭제 | — | ● | ● |
| | `flow status <id>` | 플로우 상태 조회 (RUNNING, STOPPED 등) | ● | ● | ● |
| **노드 상태** | `node list <flow-id>` | 플로우 내 노드 목록 및 상태 조회 | ● | ● | ● |
| | `node info <node-id>` | 노드 상세 정보 (타입, 설정, 포트 정보) | — | ● | ● |
| | `node stats <node-id>` | 노드 처리 통계 (처리 건수, 에러 수, 평균 처리 시간) | — | — | ● |
| **와이어(연결) 상태** | `wire list <flow-id>` | 플로우 내 연결(Connection) 목록 조회 | ● | ● | ● |
| | `wire info <wire-id>` | 연결 상세 정보 (소스/타겟 노드, 큐 크기) | — | ● | ● |
| | `wire stats <wire-id>` | 연결 통계 (전달 건수, 큐 적체량, 드롭 수) | — | — | ● |
| **모니터링** | `monitor flow <id>` | 플로우 실시간 메시지 흐름 모니터링 (tail -f 방식) | — | ● | ● |
| | `monitor node <id>` | 특정 노드의 입출력 메시지 실시간 추적 | — | ● | ● |
| | `monitor data <id> --filter <expr>` | 조건에 맞는 데이터만 필터링하여 모니터링 | — | — | ● |
| | `stats` | 엔진 전체 통계 요약 (활성 플로우 수, 전체 처리량 등) | — | — | ● |

#### 수준별 CLI 구현 가이드

- **1단계**: `Scanner` 기반의 대화형 CLI. 기본적인 플로우 시작/정지, 노드·와이어 목록 확인 기능 구현에 집중한다.
  ```
  fbp> flow list
  [1] temperature-monitoring  (RUNNING)

  fbp> node list 1
  [timer-1]   TimerNode           RUNNING
  [sensor-1]  TemperatureSensor    RUNNING
  [filter-1]  ThresholdFilter      RUNNING
  [alert-1]   AlertNode           RUNNING

  fbp> flow stop 1
  Flow 'temperature-monitoring' stopped.
  ```

- **2단계**: 명령어 파서를 구현하여 인자(argument)와 옵션(option)을 처리한다. 플로우 배포/삭제, 노드·와이어 상세 정보, 실시간 모니터링까지 구현한다.
  ```
  fbp> flow deploy config/mqtt-flow.json
  Flow 'mqtt-gateway' deployed successfully.

  fbp> monitor node mqtt-sub-1
  [14:23:01.234] IN  ← (external) {"topic":"sensor/temp","value":28.5}
  [14:23:01.235] OUT → filter-1   {"topic":"sensor/temp","value":28.5}
  [14:23:02.234] IN  ← (external) {"topic":"sensor/temp","value":31.2}
  [14:23:02.236] OUT → filter-1   {"topic":"sensor/temp","value":31.2}
  ^C (monitoring stopped)
  ```

- **3단계**: JLine 등의 라이브러리를 활용하여 자동완성, 히스토리, 컬러 출력을 지원하는 고급 CLI 구현. 필터 기반 데이터 모니터링, 엔진 전체 통계, 노드·와이어별 성능 메트릭을 제공한다.
  ```
  fbp> monitor data sensor-1 --filter "value > 30"
  [14:23:02.234] {"topic":"sensor/temp","value":31.2,"ts":1717401782}
  [14:23:05.891] {"topic":"sensor/temp","value":35.8,"ts":1717401786}

  fbp> stats
  Engine Status: RUNNING (uptime: 2h 15m)
  Active Flows: 3 | Total Nodes: 14 | Total Wires: 18
  Throughput: 1,247 msg/s | Errors: 2 (0.01%)
  Queue Pressure: 12% avg | Peak: 45% (wire mqtt-sub→filter)
  ```

### 수준별 프로젝트 과제

#### 1단계 프로젝트: "스마트 환경 모니터링 시스템"

- **요구사항**:
  - 센서 시뮬레이터 노드(온도, 습도) 구현
  - 임계값 기반 Rule 노드 구현 (예: 온도 > 30도이면 경고)
  - 알림 출력 노드 (콘솔 로그 또는 파일 저장)
  - 최소 4개 노드로 구성된 플로우
  - CLI: 플로우 시작/정지/상태 조회, 노드·와이어 목록 확인
- **산출물**: 동작하는 FBP 엔진 + CLI 관리 도구 + 시연 영상 + 발표 자료

#### 2단계 프로젝트: "IoT 게이트웨이 Rule Engine"

- **요구사항**:
  - MQTT Broker와 연동하여 실시간 센서 데이터 수집
  - MODBUS 디바이스(시뮬레이터) 제어
  - 복합 규칙 처리: 다중 조건 조합 (AND/OR), 시간 기반 규칙
  - 에러 발생 시 재시도 로직
  - CLI: 플로우 배포/삭제/재시작, 노드·와이어 상세 정보, 실시간 모니터링
- **산출물**: 동작하는 엔진 + CLI 관리 도구 + MQTT/MODBUS 연동 시연 + 아키텍처 문서 + 발표 자료

#### 3단계 프로젝트: "확장형 IoT Rule Engine 플랫폼"

- **요구사항**:
  - JSON/YAML 기반 플로우 정의 및 런타임 배포
  - 최소 1개의 외부 플러그인(JAR) 형태의 커스텀 노드
  - REST API를 통한 플로우 관리 및 모니터링
  - 서브플로우 또는 동적 라우팅 중 1개 이상 구현
  - CLI: 전체 기능 (자동완성, 필터 모니터링, 성능 메트릭, 엔진 통계)
  - 부하 테스트 결과 포함
- **산출물**: 동작하는 플랫폼 + CLI 관리 도구 + API 문서 + 성능 보고서 + 발표 자료

### 프로젝트 일정

| 일차 | 활동 |
|------|------|
| Step 11 | 프로젝트 기획: 시나리오 선정, 요구사항 정의, 역할 분담 |
| Step 12 | 설계: 아키텍처 설계, 노드 정의, 플로우 설계 |
| Step 13 | 구현: 핵심 기능 개발 |
| Step 14 | 통합 및 테스트: 전체 시스템 통합, 버그 수정, 시연 준비 |
| Step 15 | 발표 및 평가: 프로젝트 시연, 코드 리뷰, 상호 평가 |

---

## 7. 평가 기준

### 학습 과정 평가 (Step 1~10) — 40%

| 항목 | 비중 | 설명 |
|------|------|------|
| 일일 학습 결과물 | 15% | 매일 구현한 코드와 학습 정리 |
| 팀 미팅 참여도 | 10% | 발표, 질문, 피드백의 질과 양 |
| 코드 품질 | 15% | 네이밍, 구조, 테스트 코드 유무 |

### 프로젝트 평가 (Step 11~15) — 60%

| 항목 | 비중 | 설명 |
|------|------|------|
| 기능 완성도 | 20% | 요구사항 충족 여부 |
| 설계 품질 | 15% | FBP 패러다임 적용도, 확장성, 구조 |
| 시연 및 발표 | 10% | 동작 시연의 안정성, 발표의 명확성 |
| 팀워크 | 10% | 역할 분담, 협업 과정, 코드 리뷰 기록 |
| 문서화 | 5% | 아키텍처 문서, README, 코드 주석 |

---

## 8. 3단계+ 종합 과제 — MQTT 브릿지 기반 분산 FBP 엔진

> 팀 프로젝트(Step 11~15)를 완료한 3단계 팀 중, 추가 심화가 필요한 팀에게 제시하는 확장 과제다.
> 상세 내용은 별도 문서(`종합과제_분산FBP엔진.md`)에 기술한다.

### 과제 목적

팀 프로젝트에서 구현한 확장형 FBP 엔진을 **분산 환경**으로 발전시킨다. 노드 간 메시지 전달의 전송 계층(Transport Layer)을 추상화하여, 기존 `BlockingQueue` 기반 연결 외에 **외부 MQTT 브로커**를 경유하는 연결을 추가한다. 이를 통해 노드를 서로 다른 프로세스, 서로 다른 머신에 분산 배치할 수 있는 기반을 마련한다.

### 핵심 원칙

- **노드 투명성**: 노드는 기존과 동일하게 `InputPort`/`OutputPort`만 사용한다. 메시지가 `BlockingQueue`를 통해 전달되는지, MQTT 브로커를 경유하는지 노드는 알지 못한다. 전송 방식은 엔진이 플로우 설정의 `transport` 섹션을 읽어 결정한다.
- **브로커 분리**: IoT 센서 데이터 수집용 **데이터 브로커**(포트 1883)와 엔진 내부 노드 간 통신용 **시스템 브로커**(포트 1884)를 분리 운영한다. Docker Compose로 Mosquitto 2대를 실행한다.
- **CLI 통합 관리**: 플로우·노드·와이어의 동작 제어, 상태 모니터링, 통계 조회를 CLI로 수행한다. 브릿지 연결의 토픽, QoS, 브로커 상태까지 조회할 수 있다.

### 구현 범위

| Part | 내용 | 핵심 클래스 |
|------|------|------------|
| **A. Connection 추상화** | `Connection`을 인터페이스로 추출, `LocalConnection`(기존)과 `MqttBridgeConnection`(신규) 분리 | `Connection`(I), `LocalConnection`, `MqttBridgeConnection`, `MessageSerializer`, `TopicResolver`, `BridgeConnectionFactory` |
| **B. 엔진 통합** | `FlowManager`가 플로우 정의의 `transport` 섹션에 따라 Connection 구현체를 선택 | `FlowManager` 확장 |
| **C. CLI 관리** | 플로우·노드·와이어의 동작 제어, 상태 모니터링, 통계 조회를 CLI로 수행 | `CliApplication`, `CommandParser`, `FlowCommands`, `NodeCommands`, `WireCommands`, `MonitorCommands`, `StatsCommands` |
| **D. 통합 검증** | 로컬/브릿지 혼합 E2E 시나리오, 성능 비교(Local vs Bridge), 브로커 장애 복구 | 통합 테스트, 성능 테스트 |

### 테스트 규모

- 14개 테스트 클래스, 총 82개 테스트 (단위 65 + 통합 17)

### 평가 기준

| 항목 | 비중 | 세부 |
|------|:----:|------|
| Connection 추상화 | 25% | 인터페이스 설계, LocalConnection 호환성, 기존 테스트 통과 |
| MqttBridgeConnection | 25% | 브로커 경유 메시지 전달, 직렬화, 재연결, 리소스 관리 |
| CLI 완성도 | 25% | 명령어 커버리지, 출력 품질, 에러 처리, 모니터링 |
| 통합 및 성능 | 15% | E2E 시나리오, 성능 비교, 장애 복구 |
| 설계 문서 | 10% | 아키텍처 다이어그램, 설계 결정 근거, 토픽 규칙 문서화 |

---

## 9. 참고 자료

### FBP 이론

- J. Paul Morrison, *Flow-Based Programming: A New Approach to Application Development* (2nd Edition)
- [FBP 공식 사이트](https://jpaulmorrison.com/fbp/)

### Java 관련

- Oracle Java Documentation (JDK 21)
- Baeldung — Java Concurrency Tutorials
- *Effective Java* (3rd Edition), Joshua Bloch

### IoT 프로토콜

- MQTT 공식 사양: [mqtt.org](https://mqtt.org/)
- Eclipse Paho MQTT Client: [eclipse.org/paho](https://www.eclipse.org/paho/)
- MODBUS 프로토콜 사양: [modbus.org](https://modbus.org/)
- j2mod 라이브러리: [github.com/steveohara/j2mod](https://github.com/steveohara/j2mod)

### 설계 패턴

- *Design Patterns: Elements of Reusable Object-Oriented Software* (GoF)
- Refactoring.Guru — [refactoring.guru/design-patterns](https://refactoring.guru/design-patterns)
