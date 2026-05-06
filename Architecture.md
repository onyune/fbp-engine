# FBP 기반 IoT Rule Engine

## 1. 프로젝트 개요
본 프로젝트는 Flow-Based Programming (FBP) 패러다임을 기반으로 구축된 고성능 IoT 룰 엔진입니다. 센서 데이터를 수집(MQTT, Modbus)하고, 사용자가 정의한 파이프라인(Flow)에 따라 데이터를 변환, 필터링, 라우팅하여 실시간 알림 및 제어를 수행합니다.

특히 런타임 중 무중단 플로우 배포, 커스텀 플러그인 확장, 극한의 트래픽을 견디는 백프레셔(Backpressure) 제어 기능을 갖춘 엔터프라이즈급 아키텍처를 지향합니다.

---

## 2. 시스템 아키텍처 (System Architecture)

엔진은 크게 5가지 핵심 계층으로 구성되어 있습니다.

1. Core Engine 계층: FlowEngine, Flow, Connection, Port 등 데이터가 흘러가는 근간 인프라를 제공합니다.
2. Node 계층: 실제 로직을 수행하는 작은 단위입니다. (Protocol, Filter, Rule, Transform 등)
3. Registry & Plugin 계층: ServiceLoader SPI를 활용하여 외부 JAR 파일(플러그인)로부터 커스텀 노드를 동적으로 로드하고 등록합니다.
4. Flow Manager 계층: JSON 형태의 플로우 정의를 파싱하여 인스턴스로 조립하고, 엔진의 생명주기를 관리합니다.
5. API & Metrics 계층: 내장 HttpServer를 통해 엔진을 제어하고, 각 노드의 처리량(TPS), 지연시간, 에러율 등의 메트릭을 실시간으로 수집합니다.

---

## 3. 핵심 설계 결정 사항 (Architecture Decision Records)

### 1) 불변 객체(Immutable Object) 기반 메시지 처리
* 결정: 데이터가 담기는 Message 객체를 Collections.unmodifiableMap을 활용한 불변 객체로 설계했습니다.
* 이유: FBP 특성상 여러 노드(스레드)가 동시에 데이터를 주고받습니다. 메시지 불변성을 보장함으로써 동시성(Concurrency) 이슈와 Data Race를 원천 차단하여 스레드 안전성을 확보했습니다.

### 2) 백프레셔(Backpressure) 패턴 도입
* 결정: 큐가 가득 찼을 때 Block, DropOldest, DropNewest 등의 전략을 동적으로 선택할 수 있는 BackpressureConnection을 구현했습니다.
* 이유: 생산자(예: MQTT)의 속도가 소비자(예: DB 저장)보다 빠를 때 발생하는 OOM(Out Of Memory) 사태를 방지하고 시스템의 전체적인 가용성을 보호하기 위함입니다.

### 3) 독립적인 SubFlow 캡슐화
* 결정: SubFlowNode 내부에 별도의 FlowEngine과 Flow를 통째로 품게 하고 브릿지 노드로 연결했습니다.
* 이유: 복잡한 내부 파이프라인을 부모 플로우로부터 완벽히 격리하여 재사용성을 극대화하는 진정한 MSA(Microservice Architecture) 방식의 캡슐화를 달성했습니다.

### 4) 에러 포워딩 및 데드 레터 큐(DLQ) 패턴
* 결정: 모든 노드의 최상단(AbstractNode)에 ErrorPort를 내장하고, 예외 발생 시 플로우를 중단하는 대신 ErrorHandler로 메시지를 우회시켰습니다.
* 이유: 단일 노드의 에러가 전체 시스템 장애로 번지는 것을 막고(Fault Tolerance), 처리 실패한 데이터를 DeadLetterNode에 보관하여 추후 재처리가 가능하도록 설계했습니다.

---

## 4. 복합 시나리오 데이터 흐름 (Data Flow)

엔진이 처리하는 대표적인 복합 데이터 흐름은 다음과 같습니다.

```text
[MQTT Subscriber] ──(센서 데이터)──> [Dynamic Router]
                                     │
           ┌─────────────────────────┼─────────────────────────┐
     [조건: 온도 > 30]           [조건: 에러 발생]         [조건: 기본 (Default)]
           ▼                         ▼                         ▼
      [SubFlow: 알림]           [ErrorHandlerNode]      [CollectorNode (정상로그)]
 (내부: Filter -> Rule)              │
           ▼                         ▼
    [ModbusWriter]           [DeadLetterQueue]
    
```

---

## 5. REST API 명세서 

Method,Endpoint,설명
GET,/health,엔진 정상 가동 여부 및 Uptime 확인
GET,/flows,현재 엔진에 등록되어 실행 중인 Flow 목록 반환
POST,/flows,JSON 기반의 새로운 Flow 배포 및 동적 실행
DELETE,/flows/{id},실행 중인 Flow 동적 중지 및 삭제
GET,/flows/{id}/metrics,"특정 Flow의 노드별 실시간 TPS, 레이턴시, 에러 통계 반환"
GET,/nodes/{id}/stats,개별 노드의 상세 처리 통계 조회

---

## 6. 성능 및 부하 테스트 결과 

- 목표 부하: 10,000 메시지 고속 주입

- 초당 처리량 (Throughput): > 1,000 TPS 유지

- 지연 시간 (Latency): 평균 < 10ms

- 안정성: 스레드 풀 최적화(ThreadPoolConfig) 적용 및 메모리 누수 방지 검증 완료.