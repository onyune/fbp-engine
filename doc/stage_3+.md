# FBP IoT Rule Engine — 종합 과제: MQTT 브릿지 기반 분산 FBP 엔진

## 1. 과제 개요

### 배경

지금까지 구현한 FBP 엔진에서 노드 간 메시지 전달은 **JVM 내부의 `BlockingQueue` 기반 `Connection`**으로 이루어졌다. 모든 노드가 같은 프로세스 안에서 동작하므로, 노드 수가 늘어나면 단일 JVM의 메모리와 CPU에 제약을 받는다.

이 과제에서는 노드 간 연결의 **전송 계층(Transport Layer)**을 추상화하여, 기존 `BlockingQueue` 대신 **외부 MQTT 브로커**를 통해 메시지를 주고받는 구조로 확장한다. 이렇게 하면 노드를 서로 다른 프로세스, 서로 다른 머신에 분산 배치할 수 있는 기반이 된다.

### 핵심 원칙

```
┌─────────────────────────────────────────────────────────────┐
│  노드는 아무것도 모른다                                       │
│                                                             │
│  노드는 여전히 InputPort에서 메시지를 받고,                     │
│  OutputPort로 메시지를 보낸다.                                │
│  그 메시지가 BlockingQueue를 통해 전달되는지,                   │
│  MQTT 브로커를 경유하는지 노드는 알지 못한다.                    │
│                                                             │
│  전송 방식의 결정은 엔진(FlowManager)이 플로우 설정을 읽어       │
│  Connection 구현체를 선택하는 것으로 이루어진다.                 │
└─────────────────────────────────────────────────────────────┘
```

### 브로커 구분

| 구분 | 용도 | 예시 |
|------|------|------|
| **데이터 브로커** | IoT 센서 데이터 수집용. 2단계에서 구현한 `MqttSubscriberNode`, `MqttPublisherNode`가 연결하는 브로커 | `tcp://data-broker:1883` |
| **시스템 브로커** | FBP 엔진 내부 노드 간 메시지 전달용. Connection의 전송 계층으로 사용 | `tcp://system-broker:1884` |

두 브로커는 **반드시 분리**한다. 데이터 브로커의 트래픽이 엔진 내부 통신에 영향을 주지 않아야 하며, 보안 정책도 다를 수 있다.

---

## 2. 아키텍처

### 기존 구조 (로컬)

```
[NodeA] ──OutputPort──→ Connection(BlockingQueue) ──InputPort──→ [NodeB]
                            │
                      동일 JVM, 동일 스레드 풀
```

### 목표 구조 (MQTT 브릿지)

```
[NodeA] ──OutputPort──→ MqttBridgeConnection ──→ System Broker ──→ MqttBridgeConnection ──InputPort──→ [NodeB]
                            │                    (tcp://system    │
                            │  publish to          -broker:1884)  │  subscribe from
                            │  topic: fbp/flow-1/                 │  topic: fbp/flow-1/
                            │         nodeA.out→nodeB.in          │         nodeA.out→nodeB.in
                            │                                     │
                       동일 또는 다른 JVM                      동일 또는 다른 JVM
```

### Connection 추상화

```
                        «interface»
                        Connection
                     ┌──────────────┐
                     │ deliver(msg) │
                     │ poll(): msg  │
                     │ getBufferSize│
                     └──────┬───────┘
                            │
              ┌─────────────┼─────────────┐
              │                           │
   LocalConnection              MqttBridgeConnection
   (BlockingQueue 기반)          (MQTT Pub/Sub 기반)
   - 기존 구현 그대로             - deliver() → MQTT publish
                                 - poll() → MQTT subscribe → 내부 큐
```

노드 입장에서는 `Connection` 인터페이스만 사용하므로, 어떤 구현체가 주입되었는지 알 수 없다. **Strategy 패턴**의 전형적인 적용이다.

### 플로우 설정을 통한 전송 계층 선택

```json
{
  "id": "temperature-monitoring",
  "transport": {
    "type": "mqtt",
    "broker": "tcp://system-broker:1884",
    "qos": 1
  },
  "nodes": [
    {"id": "sensor", "type": "MqttSubscriber", "config": {"broker": "tcp://data-broker:1883", "topic": "sensor/temp"}},
    {"id": "rule",   "type": "ThresholdFilter", "config": {"field": "value", "threshold": 30}},
    {"id": "alert",  "type": "MqttPublisher",   "config": {"broker": "tcp://data-broker:1883", "topic": "alert/temp"}}
  ],
  "connections": [
    {"from": "sensor:out", "to": "rule:in"},
    {"from": "rule:out",   "to": "alert:in"}
  ]
}
```

- `transport` 섹션이 **없으면** → 기존 `LocalConnection`(BlockingQueue) 사용
- `transport.type`이 `"mqtt"`이면 → `MqttBridgeConnection`으로 모든 연결 구성
- 연결 단위로 override 가능: 특정 연결만 local, 나머지는 mqtt (선택 구현)

### 토픽 네이밍 규칙

시스템 브로커에서 노드 간 메시지를 구분하기 위한 토픽 규칙:

```
fbp/{flow-id}/{sourceNodeId}.{sourcePort}→{targetNodeId}.{targetPort}
```

예시:
```
fbp/temperature-monitoring/sensor.out→rule.in
fbp/temperature-monitoring/rule.out→alert.in
```

---

## 3. 구현 파트

### Part A — Connection 추상화 및 MqttBridgeConnection

#### 목표

1. 기존 `Connection` 클래스를 `Connection` **인터페이스**로 추출
2. 기존 BlockingQueue 구현을 `LocalConnection`으로 분리
3. MQTT 기반 `MqttBridgeConnection` 구현
4. 노드 코드는 **한 줄도 변경하지 않는다**

#### 구현할 클래스

| 패키지 | 클래스/인터페이스 | 역할 |
|--------|------------------|------|
| `core/` | `Connection` (인터페이스) | `deliver()`, `poll()`, `getBufferSize()`, `getId()` 계약 |
| `core/` | `LocalConnection` | 기존 BlockingQueue 기반 구현 (리네이밍) |
| `bridge/` | `MqttBridgeConnection` | MQTT Pub/Sub 기반 Connection 구현 |
| `bridge/` | `MqttBridgeConfig` | 시스템 브로커 접속 정보 (broker URL, QoS, clientId 접두사 등) |
| `bridge/` | `BridgeConnectionFactory` | 플로우 설정의 transport 섹션을 읽어 적절한 Connection 생성 |
| `bridge/` | `MessageSerializer` | `Message` ↔ `byte[]` 직렬화/역직렬화 (JSON) |
| `bridge/` | `TopicResolver` | 연결 정의 → MQTT 토픽 문자열 변환 |
| `bridge/` | `BridgeException` | 브릿지 관련 예외 |

#### MqttBridgeConnection 핵심 동작

```java
public class MqttBridgeConnection implements Connection {

    private final MqttClient publisher;     // deliver() 시 publish
    private final MqttClient subscriber;    // 수신 메시지를 내부 큐에 적재
    private final BlockingQueue<Message> internalQueue;  // poll()용 내부 버퍼
    private final String topic;
    private final MessageSerializer serializer;

    @Override
    public void deliver(Message message) {
        byte[] payload = serializer.serialize(message);
        publisher.publish(topic, payload, qos);
        // 노드 입장: deliver()만 호출하면 끝. MQTT는 모른다.
    }

    @Override
    public Message poll() throws InterruptedException {
        return internalQueue.take();
        // subscriber 콜백이 internalQueue에 넣어둔 메시지를 꺼냄
        // 노드 입장: poll()만 호출하면 끝. MQTT는 모른다.
    }
}
```

#### 스스로 답해야 할 질문

- `MqttBridgeConnection` 하나당 MqttClient를 두 개(pub용, sub용) 만들 것인가, 하나로 공유할 것인가?
- 하나의 플로우에 연결이 10개면 MqttClient가 20개 생기는 문제를 어떻게 해결할 것인가? → 공유 클라이언트 풀?
- `deliver()` 시 QoS 0(최대 1회)과 QoS 1(최소 1회)의 트레이드오프는?
- 시스템 브로커가 다운되면 `deliver()`와 `poll()`은 어떻게 동작해야 하는가?
- `Message`의 직렬화 포맷은? JSON이 가장 범용적이지만, 성능이 중요하면 바이너리(MessagePack 등)도 고려할 것인가?
- 토픽명에 특수문자(`→`)를 쓸 수 있는가? MQTT 사양에서 허용되지 않는 문자는?

> **테스트 항목 — Connection 인터페이스 리팩토링**
>
> | # | 테스트명 | 검증 내용 |
> |---|---------|----------|
> | 1 | LocalConnection 호환성 | 기존 BlockingQueue 테스트가 `LocalConnection`으로 전환 후 모두 통과 |
> | 2 | 인터페이스 타입 사용 | 노드가 `Connection` 인터페이스 타입으로 참조하고 있으며, 구현체 교체 시 노드 코드 변경 없음 |
> | 3 | 기존 플로우 동작 유지 | 1~3단계 모든 기존 테스트가 리팩토링 후에도 통과 |

> **테스트 항목 — MessageSerializer**
>
> | # | 테스트명 | 검증 내용 |
> |---|---------|----------|
> | 1 | 직렬화→역직렬화 | `Message` → `byte[]` → `Message` 왕복 후 원본과 동일 |
> | 2 | 다양한 타입 | String, Integer, Double, Boolean, List, 중첩 Map 포함 메시지 직렬화 |
> | 3 | 빈 페이로드 | 빈 Map의 Message 직렬화/역직렬화 |
> | 4 | null 값 | 값이 null인 엔트리 포함 시 처리 |
> | 5 | 대용량 페이로드 | 100KB 이상의 메시지 직렬화 성능 (타임아웃 내 완료) |

> **테스트 항목 — TopicResolver**
>
> | # | 테스트명 | 검증 내용 |
> |---|---------|----------|
> | 1 | 정상 변환 | flowId="flow-1", from="sensor:out", to="rule:in" → `"fbp/flow-1/sensor.out-rule.in"` |
> | 2 | 특수문자 치환 | 토픽에 MQTT 금지 문자(`+`, `#`, `\0`)가 포함되지 않음 |
> | 3 | 고유성 | 서로 다른 연결은 서로 다른 토픽을 생성 |
> | 4 | 역변환 | 토픽 문자열 → 원래의 flowId, sourceNode, sourcePort, targetNode, targetPort 복원 |

> **테스트 항목 — MqttBridgeConnection**
>
> | # | 테스트명 | 검증 내용 |
> |---|---------|----------|
> | 1 | deliver→poll 기본 | deliver()한 메시지가 브로커를 경유하여 poll()로 수신됨 |
> | 2 | 메시지 순서 | 3개 메시지 deliver 후 poll 순서가 FIFO (QoS 1 기준) |
> | 3 | 메시지 내용 보존 | 직렬화→브로커→역직렬화 후 모든 필드와 값이 원본과 동일 |
> | 4 | 멀티스레드 | 생산자/소비자 스레드 분리 시 메시지 정상 전달 |
> | 5 | 브로커 연결 실패 | 브로커 미실행 시 `BridgeException` 발생 |
> | 6 | 재연결 | 브로커 일시 중단 후 복구 시 메시지 전달 재개 |
> | 7 | 종료 (close) | close() 호출 시 MQTT 연결 해제, 리소스 정리 |
> | 8 | getBufferSize | 내부 큐에 쌓인 미소비 메시지 수 반환 |
> | 9 | 다중 연결 독립성 | 같은 브로커의 서로 다른 토픽을 사용하는 두 Connection이 독립적으로 동작 |

> **테스트 항목 — BridgeConnectionFactory**
>
> | # | 테스트명 | 검증 내용 |
> |---|---------|----------|
> | 1 | transport 없음 → Local | transport 섹션이 없는 플로우 정의 → `LocalConnection` 생성 |
> | 2 | transport=mqtt → Bridge | transport.type="mqtt" → `MqttBridgeConnection` 생성 |
> | 3 | 잘못된 type | transport.type="unknown" → 예외 |
> | 4 | broker URL 누락 | transport.type="mqtt"인데 broker가 없으면 예외 |
> | 5 | 연결별 override | 특정 연결만 local로 override 시 해당 연결만 `LocalConnection` |

---

### Part B — 엔진 통합: FlowManager 확장

#### 목표

1. `FlowManager`가 플로우 정의의 `transport` 섹션을 인식하여 적절한 Connection 구현체를 생성
2. 기존 로컬 플로우와 MQTT 브릿지 플로우가 **동일한 엔진**에서 공존
3. 플로우 생명주기(deploy/start/stop/remove) 시 브릿지 연결의 MQTT 세션도 관리

#### 변경 사항

```
기존 FlowManager.deploy(FlowDefinition)
  └── NodeRegistry로 노드 생성
  └── Connection(BlockingQueue)으로 연결     ← 여기만 변경
  └── Flow 객체 생성 → FlowEngine 등록

변경 후 FlowManager.deploy(FlowDefinition)
  └── NodeRegistry로 노드 생성
  └── BridgeConnectionFactory로 Connection 생성  ← transport에 따라 Local 또는 Bridge
  └── Flow 객체 생성 → FlowEngine 등록
```

#### 스스로 답해야 할 질문

- 하나의 플로우 안에서 일부 연결은 Local, 일부는 MQTT Bridge로 섞어 쓸 수 있는가?
- 브릿지 플로우를 stop할 때 MQTT 구독을 해지해야 하는가? 재시작 시 다시 구독?
- 플로우 remove 시 시스템 브로커에 남아있는 retained 메시지는 어떻게 처리할 것인가?
- 두 개의 플로우가 같은 시스템 브로커를 공유할 때, 토픽 네임스페이스 충돌은 없는가?

> **테스트 항목 — FlowManager 브릿지 통합**
>
> | # | 테스트명 | 검증 내용 |
> |---|---------|----------|
> | 1 | 로컬 플로우 호환 | transport 없는 기존 플로우가 변경 없이 정상 동작 |
> | 2 | 브릿지 플로우 배포 | transport=mqtt 플로우 배포 → 모든 Connection이 `MqttBridgeConnection` |
> | 3 | 브릿지 플로우 실행 | 배포 후 start → 노드 간 메시지가 시스템 브로커를 경유하여 전달됨 |
> | 4 | 혼합 플로우 | 동일 엔진에 로컬 플로우 1개 + 브릿지 플로우 1개 동시 실행 |
> | 5 | stop 시 구독 해지 | 브릿지 플로우 stop → MQTT 구독이 해지되고 메시지 수신 중단 |
> | 6 | restart 시 재구독 | stop 후 restart → 구독 재개, 메시지 전달 재개 |
> | 7 | remove 시 리소스 정리 | remove → MQTT 연결 종료, 내부 큐 비움 |
> | 8 | 브로커 URL 유효성 | 잘못된 broker URL → 배포 실패, 적절한 에러 메시지 |

---

### Part C — CLI를 통한 엔진 관리

#### 목표

1. CLI에서 플로우의 전체 생명주기를 관리 (배포, 시작, 정지, 재시작, 삭제)
2. 노드 상태 조회 및 실시간 메시지 모니터링
3. 와이어(Connection) 상태 조회 — 전송 타입(Local/MQTT), 토픽, 큐 상태 포함
4. 엔진 전체 통계 (활성 플로우, 처리량, 에러율)

#### CLI 명령어 전체

| 영역 | 명령어 | 설명 |
|------|--------|------|
| **플로우** | `flow list` | 등록된 플로우 목록 (id, 상태, transport 타입) |
| | `flow deploy <file>` | JSON 파일에서 플로우 배포 |
| | `flow start <id>` | 플로우 시작 |
| | `flow stop <id>` | 플로우 정지 |
| | `flow restart <id>` | 플로우 재시작 |
| | `flow remove <id>` | 플로우 삭제 |
| | `flow status <id>` | 플로우 상태 상세 (노드 수, 연결 수, transport, uptime) |
| **노드** | `node list <flow-id>` | 플로우 내 노드 목록 (id, 타입, 상태) |
| | `node info <node-id>` | 노드 상세 (타입, config, 포트 목록) |
| | `node stats <node-id>` | 노드 통계 (처리 건수, 에러 수, 평균 처리 시간) |
| **와이어** | `wire list <flow-id>` | 연결 목록 (id, from→to, transport 타입) |
| | `wire info <wire-id>` | 연결 상세 (transport, 토픽명, 큐 크기, QoS) |
| | `wire stats <wire-id>` | 연결 통계 (전달 건수, 큐 적체량, 드롭 수) |
| **모니터링** | `monitor flow <id>` | 플로우 실시간 메시지 흐름 (tail -f 방식) |
| | `monitor node <id>` | 노드 입출력 메시지 실시간 추적 |
| | `monitor data <id> --filter <expr>` | 조건 필터링 모니터링 |
| **시스템** | `stats` | 엔진 전체 통계 (활성 플로우, 노드 수, 처리량, 에러율) |
| | `broker status` | 시스템 브로커 연결 상태 |
| | `help [command]` | 명령어 도움말 |
| | `exit` | CLI 종료 |

#### CLI 출력 예시

```
fbp> flow list
ID                        STATUS   TRANSPORT  NODES  WIRES
temperature-monitoring    RUNNING  mqtt       3      2
humidity-check            RUNNING  local      4      3
data-aggregation          STOPPED  mqtt       5      4

fbp> flow status temperature-monitoring
Flow: temperature-monitoring
  Status:    RUNNING
  Transport: mqtt (tcp://system-broker:1884, QoS=1)
  Nodes:     3
  Wires:     2
  Uptime:    1h 23m 45s
  Processed: 45,231 messages
  Errors:    3 (0.007%)

fbp> wire list temperature-monitoring
ID    FROM            TO            TRANSPORT  TOPIC                                          QUEUE
w-1   sensor:out  →   rule:in       mqtt       fbp/temperature-monitoring/sensor.out-rule.in   2
w-2   rule:out    →   alert:in      mqtt       fbp/temperature-monitoring/rule.out-alert.in    0

fbp> wire info w-1
Wire: w-1
  From:       sensor:out
  To:         rule:in
  Transport:  MqttBridge
  Broker:     tcp://system-broker:1884
  Topic:      fbp/temperature-monitoring/sensor.out-rule.in
  QoS:        1
  Queue Size: 2 / 100
  Delivered:  45,231
  Dropped:    0

fbp> node stats rule
Node: rule (ThresholdFilter)
  Status:     RUNNING
  Processed:  45,231
  Output:     12,847 (28.4%)
  Filtered:   32,384 (71.6%)
  Errors:     0
  Avg Time:   0.3ms
  Max Time:   4.2ms

fbp> monitor node rule
[15:42:01.234] IN  ← sensor    {"topic":"sensor/temp","value":28.5}
[15:42:01.235] --- FILTERED    (value 28.5 <= threshold 30)
[15:42:02.234] IN  ← sensor    {"topic":"sensor/temp","value":31.2}
[15:42:02.235] OUT → alert     {"topic":"sensor/temp","value":31.2,"alert":true}
[15:42:03.234] IN  ← sensor    {"topic":"sensor/temp","value":29.8}
[15:42:03.235] --- FILTERED    (value 29.8 <= threshold 30)
^C (monitoring stopped)

fbp> broker status
System Broker: tcp://system-broker:1884
  Status:       CONNECTED
  Client ID:    fbp-engine-01
  Active Topics: 4
  Messages/sec:  1,247 (in) / 1,245 (out)

fbp> stats
╔══════════════════════════════════════════╗
║         FBP Engine Statistics            ║
╠══════════════════════════════════════════╣
║ Status:        RUNNING                   ║
║ Uptime:        2h 15m 30s               ║
║ Active Flows:  2 (local: 1, mqtt: 1)    ║
║ Stopped Flows: 1                        ║
║ Total Nodes:   12                       ║
║ Total Wires:   9 (local: 3, mqtt: 6)   ║
║ Throughput:    1,247 msg/s              ║
║ Total Errors:  3 (0.007%)              ║
║ Heap Used:     128 MB / 512 MB         ║
║ Active Threads: 18                      ║
╚══════════════════════════════════════════╝
```

#### 구현할 클래스

| 패키지 | 클래스/인터페이스 | 역할 |
|--------|------------------|------|
| `cli/` | `CliApplication` | 메인 CLI 루프 (Scanner 또는 JLine 기반) |
| `cli/` | `CommandParser` | 입력 문자열 → Command 객체 파싱 |
| `cli/` | `Command` | 명령어 데이터 (영역, 동작, 인자, 옵션) |
| `cli/` | `CommandExecutor` | Command를 받아 FlowManager/MetricsCollector 호출 |
| `cli/` | `FlowCommands` | flow 영역 명령어 핸들러 |
| `cli/` | `NodeCommands` | node 영역 명령어 핸들러 |
| `cli/` | `WireCommands` | wire 영역 명령어 핸들러 |
| `cli/` | `MonitorCommands` | monitor 영역 명령어 핸들러 (실시간 스트리밍) |
| `cli/` | `StatsCommands` | stats, broker status 핸들러 |
| `cli/` | `OutputFormatter` | 표, 통계, 실시간 로그 등 출력 포맷팅 |

#### 스스로 답해야 할 질문

- CLI 입력 처리는 메인 스레드에서, 모니터링 출력은 별도 스레드에서 해야 하는가?
- `monitor` 명령 실행 중 다른 명령을 어떻게 입력받을 것인가? (Ctrl+C로 중단?)
- CLI가 FlowManager에 직접 접근할 것인가, REST API를 통해 간접 접근할 것인가?
- 명령어 자동완성을 구현한다면 flow id, node id 목록을 어떻게 동적으로 제공할 것인가?
- 출력 포맷을 사람이 읽기 좋은 형태와 기계가 파싱하기 좋은 형태(JSON) 중 어떤 것으로 할 것인가? 둘 다?

> **테스트 항목 — CommandParser**
>
> | # | 테스트명 | 검증 내용 |
> |---|---------|----------|
> | 1 | 단순 명령 | `"flow list"` → Command(area=flow, action=list) |
> | 2 | 인자 포함 | `"flow deploy config/flow.json"` → args=["config/flow.json"] |
> | 3 | 옵션 포함 | `"monitor data sensor --filter \"value > 30\""` → filter 옵션 파싱 |
> | 4 | 빈 입력 | `""` → null 또는 빈 Command |
> | 5 | 잘못된 명령 | `"unknown cmd"` → 알 수 없는 명령 에러 |
> | 6 | 대소문자 무시 | `"Flow List"` → `"flow list"`와 동일 처리 |

> **테스트 항목 — FlowCommands**
>
> | # | 테스트명 | 검증 내용 |
> |---|---------|----------|
> | 1 | flow list | 등록된 플로우 목록 출력 (id, status, transport) |
> | 2 | flow deploy | JSON 파일 경로 → 플로우 배포 성공 메시지 |
> | 3 | flow deploy 실패 | 존재하지 않는 파일 → 에러 메시지 |
> | 4 | flow start | 정지 상태 플로우 시작 → RUNNING |
> | 5 | flow stop | 실행 중 플로우 정지 → STOPPED |
> | 6 | flow restart | 실행 중 플로우 → 정지 후 재시작 |
> | 7 | flow remove | 플로우 삭제 → 목록에서 제거 |
> | 8 | flow status | 플로우 상세 정보 (transport, 노드 수, uptime) 출력 |
> | 9 | 없는 id | 존재하지 않는 flow id → 에러 메시지 |

> **테스트 항목 — NodeCommands**
>
> | # | 테스트명 | 검증 내용 |
> |---|---------|----------|
> | 1 | node list | 플로우 내 노드 목록 (id, 타입, 상태) 출력 |
> | 2 | node info | 노드 상세 (타입, config, 포트) 출력 |
> | 3 | node stats | 노드 통계 (처리 건수, 에러, 평균 시간) 출력 |
> | 4 | 없는 node id | 에러 메시지 |

> **테스트 항목 — WireCommands**
>
> | # | 테스트명 | 검증 내용 |
> |---|---------|----------|
> | 1 | wire list | 연결 목록 (from→to, transport, 토픽) 출력 |
> | 2 | wire info — Local | LocalConnection 상세 (큐 크기) |
> | 3 | wire info — Bridge | MqttBridgeConnection 상세 (브로커, 토픽, QoS) |
> | 4 | wire stats | 연결 통계 (전달 건수, 큐 적체량) |
> | 5 | 없는 wire id | 에러 메시지 |

> **테스트 항목 — MonitorCommands**
>
> | # | 테스트명 | 검증 내용 |
> |---|---------|----------|
> | 1 | monitor flow | 메시지 흐름이 실시간으로 출력됨 |
> | 2 | monitor node | 특정 노드의 IN/OUT 메시지가 타임스탬프와 함께 출력 |
> | 3 | monitor data --filter | 필터 조건에 맞는 메시지만 출력 |
> | 4 | 없는 id | 에러 메시지 |

> **테스트 항목 — StatsCommands**
>
> | # | 테스트명 | 검증 내용 |
> |---|---------|----------|
> | 1 | stats | 엔진 전체 통계 (활성 플로우, 처리량, 에러율) 출력 |
> | 2 | broker status — 연결됨 | 시스템 브로커 상태 CONNECTED, 활성 토픽 수 |
> | 3 | broker status — 미사용 | 브릿지 플로우 없으면 "시스템 브로커 미사용" 출력 |

---

### Part D — 통합 시나리오 및 성능 검증

#### 목표

1. 로컬/브릿지 혼합 환경에서 전체 시스템 정상 동작 확인
2. 브릿지 기반 플로우의 성능 측정 (LocalConnection 대비)
3. 브로커 장애 시 복구 동작 확인
4. CLI를 통한 전체 운영 시나리오 시연

#### 통합 시나리오 구성

```
시나리오: 스마트 빌딩 환경 모니터링

[데이터 브로커: tcp://data-broker:1883]
     │
     ▼
Flow-1 (MQTT 브릿지, 시스템 브로커: tcp://system-broker:1884)
┌─────────────────────────────────────────────────────────────┐
│  MqttSubscriber ──(bridge)──→ DynamicRouter                 │
│       (data-broker)               │                         │
│                    ┌──────────────┼──────────────┐          │
│                    ▼              ▼              ▼          │
│              TempRule       HumidityRule    PressureRule     │
│                    │              │              │          │
│                    ▼              ▼              ▼          │
│              ModbusWriter   MqttPublisher   AlertNode       │
│              (Socket 기반)   (data-broker)                   │
└─────────────────────────────────────────────────────────────┘

Flow-2 (로컬, BlockingQueue)
┌─────────────────────────────────────────────────────────────┐
│  TimerNode ──(local)──→ HealthChecker ──(local)──→ Logger   │
└─────────────────────────────────────────────────────────────┘
```

#### CLI 운영 시나리오 (시연 스크립트)

```
# 1. 엔진 상태 확인
fbp> stats

# 2. 브릿지 플로우 배포
fbp> flow deploy config/smart-building.json
Flow 'smart-building' deployed successfully.

# 3. 로컬 플로우 배포
fbp> flow deploy config/health-check.json
Flow 'health-check' deployed successfully.

# 4. 전체 플로우 시작
fbp> flow start smart-building
fbp> flow start health-check

# 5. 현재 상태 확인
fbp> flow list
fbp> wire list smart-building

# 6. 실시간 모니터링
fbp> monitor node temp-rule
(... 실시간 메시지 확인 ...)
^C

# 7. 통계 확인
fbp> node stats temp-rule
fbp> wire stats w-1
fbp> stats

# 8. 브로커 상태
fbp> broker status

# 9. 플로우 제어
fbp> flow stop smart-building
fbp> flow status smart-building
fbp> flow restart smart-building

# 10. 정리
fbp> flow remove health-check
fbp> flow remove smart-building
fbp> exit
```

> **테스트 항목 — 통합 시나리오 (`@Tag("integration")`)**
>
> | # | 테스트명 | 검증 내용 |
> |---|---------|----------|
> | 1 | 브릿지 플로우 E2E | MQTT 수신 → 브릿지 경유 → 규칙 처리 → 출력 노드까지 메시지 도달 |
> | 2 | 로컬+브릿지 공존 | 두 종류의 플로우가 동일 엔진에서 동시 실행, 상호 간섭 없음 |
> | 3 | 데이터/시스템 브로커 분리 | MqttSubscriberNode는 데이터 브로커, Connection은 시스템 브로커 사용 확인 |
> | 4 | 노드 투명성 | ThresholdFilter 등 일반 노드가 브릿지 환경에서도 코드 변경 없이 동작 |
> | 5 | 플로우 생명주기 | deploy → start → stop → restart → remove 전체 순환 |
> | 6 | 브로커 장애 복구 | 시스템 브로커 일시 중단 → 복구 후 메시지 전달 재개 |
> | 7 | 다중 플로우 | 3개 이상의 브릿지 플로우를 동시 실행, 토픽 간섭 없음 |

> **테스트 항목 — 성능 비교 (`@Tag("performance")`)**
>
> | # | 테스트명 | 검증 내용 |
> |---|---------|----------|
> | 1 | Local 처리량 | LocalConnection 기반 10,000건 처리 시간 측정 (기준선) |
> | 2 | Bridge 처리량 | MqttBridgeConnection 기반 10,000건 처리 시간 측정 |
> | 3 | 처리량 비교 | Bridge의 처리량이 Local 대비 비율 기록 (오버헤드 측정) |
> | 4 | Bridge 지연 시간 | 메시지 deliver~poll 간 지연 시간 평균 및 99th percentile |
> | 5 | 장시간 안정성 | Bridge 플로우 3분 연속 실행 시 메모리/큐 안정성 |

> **테스트 항목 — CLI 통합 (`@Tag("integration")`)**
>
> | # | 테스트명 | 검증 내용 |
> |---|---------|----------|
> | 1 | CLI→deploy→start | CLI로 플로우 배포 및 시작 → 메시지 처리 확인 |
> | 2 | CLI→monitor | CLI 모니터링 시작 → 메시지 흐름이 출력됨 |
> | 3 | CLI→stats 정확성 | CLI stats 출력값과 MetricsCollector 직접 조회 값이 일치 |
> | 4 | CLI→wire info | 브릿지 와이어의 토픽, QoS 정보가 정확히 표시 |
> | 5 | CLI 에러 처리 | 잘못된 명령, 없는 id 등에 대해 사용자 친화적 에러 메시지 |

---

## 4. 전체 테스트 현황

### 테스트 패키지 구조

```
src/test/java/com/example/fbp/
├── core/
│   └── ConnectionInterfaceTest.java         (3개)
├── bridge/
│   ├── MessageSerializerTest.java           (5개)
│   ├── TopicResolverTest.java               (4개)
│   ├── MqttBridgeConnectionTest.java        (9개)
│   └── BridgeConnectionFactoryTest.java     (5개)
├── engine/
│   └── FlowManagerBridgeTest.java           (8개)
├── cli/
│   ├── CommandParserTest.java               (6개)
│   ├── FlowCommandsTest.java               (9개)
│   ├── NodeCommandsTest.java               (4개)
│   ├── WireCommandsTest.java               (5개)
│   ├── MonitorCommandsTest.java            (4개)
│   └── StatsCommandsTest.java              (3개)
└── integration/
    ├── BridgeScenarioTest.java              (7개)
    ├── BridgePerformanceTest.java           (5개)
    └── CliIntegrationTest.java              (5개)
```

### 테스트 요약

| Part | 테스트 클래스 | 단위 테스트 | 통합 테스트 |
|------|-------------|:---------:|:---------:|
| A — Connection 추상화 | ConnectionInterfaceTest, MessageSerializerTest, TopicResolverTest, MqttBridgeConnectionTest, BridgeConnectionFactoryTest | 26 | — |
| B — 엔진 통합 | FlowManagerBridgeTest | 8 | — |
| C — CLI | CommandParserTest, FlowCommandsTest, NodeCommandsTest, WireCommandsTest, MonitorCommandsTest, StatsCommandsTest | 31 | — |
| D — 통합/성능 | BridgeScenarioTest, BridgePerformanceTest, CliIntegrationTest | — | 17 |
| **합계** | **14개 클래스** | **65** | **17** |
| | | **총 82개 테스트** | |

---

## 5. 전체 패키지 구조 (추가분)

```
src/main/java/com/example/fbp/
├── core/
│   ├── Connection          (인터페이스로 변경)
│   └── LocalConnection     (기존 BlockingQueue 구현 — 리네이밍)
├── bridge/                 ← 종합 과제 신규
│   ├── MqttBridgeConnection
│   ├── MqttBridgeConfig
│   ├── BridgeConnectionFactory
│   ├── MessageSerializer
│   ├── TopicResolver
│   └── BridgeException
├── cli/                    ← 종합 과제 신규
│   ├── CliApplication
│   ├── CommandParser
│   ├── Command
│   ├── CommandExecutor
│   ├── FlowCommands
│   ├── NodeCommands
│   ├── WireCommands
│   ├── MonitorCommands
│   ├── StatsCommands
│   └── OutputFormatter
└── (기존 패키지는 변경 없음)
```

---

## 6. 설계 참고 사항

### Connection 인터페이스 리팩토링 체크리스트

기존 `Connection` 클래스를 인터페이스로 변경할 때, 기존 코드가 깨지지 않도록 아래 순서를 따른다:

1. `Connection` 인터페이스 정의 (`deliver`, `poll`, `getBufferSize`, `getId`, `close`)
2. 기존 `Connection` 클래스를 `LocalConnection`으로 리네이밍, `Connection` 인터페이스 구현
3. 기존 코드에서 `new Connection(...)` → `new LocalConnection(...)` 변경
4. 노드 코드에서 `Connection` 타입 참조는 이미 인터페이스이므로 변경 불필요
5. 기존 테스트 모두 실행 → 전부 통과 확인
6. `MqttBridgeConnection` 구현

### MQTT 클라이언트 풀링

`MqttBridgeConnection`마다 별도의 `MqttClient`를 만들면 연결 수가 폭발한다. 해결 방안:

```
MqttClientPool
 ├── getPublisher(brokerUrl) → 공유 MqttClient (모든 Connection이 publish에 사용)
 ├── getSubscriber(brokerUrl, topic, callback) → 공유 MqttClient에 topic 구독 추가
 └── release(brokerUrl) → 참조 카운트 0이면 연결 종료
```

하나의 시스템 브로커에 대해 publish용 1개, subscribe용 1개 클라이언트만 유지하면, 연결 10개인 플로우도 MqttClient 2개로 처리할 수 있다.

### 메시지 직렬화 포맷

```json
{
  "v": 1,
  "ts": 1717401782345,
  "src": "sensor",
  "port": "out",
  "payload": {
    "topic": "sensor/temp",
    "value": 31.2,
    "unit": "celsius"
  }
}
```

- `v`: 프로토콜 버전 (향후 포맷 변경 시 하위 호환)
- `ts`: 직렬화 시점 타임스탬프
- `src`, `port`: 디버깅/모니터링용 메타데이터
- `payload`: 원본 `Message`의 페이로드

---

## 7. 평가 기준

| 항목 | 비중 | 세부 |
|------|:----:|------|
| **Connection 추상화** | 25% | 인터페이스 설계, LocalConnection 호환성, 기존 테스트 통과 |
| **MqttBridgeConnection** | 25% | 브로커 경유 메시지 전달, 직렬화, 재연결, 리소스 관리 |
| **CLI 완성도** | 25% | 명령어 커버리지, 출력 품질, 에러 처리, 모니터링 |
| **통합 및 성능** | 15% | E2E 시나리오, 성능 비교, 장애 복구 |
| **설계 문서** | 10% | 아키텍처 다이어그램, 설계 결정 근거, 토픽 규칙 문서화 |

---

## 8. 환경 구성: Docker Compose로 MQTT 브로커 2대 실행

종합 과제를 수행하려면 **데이터 브로커**(포트 1883)와 **시스템 브로커**(포트 1884) 두 대의 MQTT 브로커가 필요하다. Docker Compose를 사용하면 한 줄의 명령으로 두 브로커를 동시에 실행할 수 있다.

### 사전 준비

- Docker 및 Docker Compose 설치: [docs.docker.com/get-docker](https://docs.docker.com/get-docker/)
- 설치 확인:
  ```bash
  docker --version
  docker compose version
  ```

### 디렉토리 구조

프로젝트 루트에 아래 파일들을 생성한다.

```
project-root/
├── docker/
│   ├── docker-compose.yml
│   ├── mosquitto-data/
│   │   └── mosquitto.conf
│   └── mosquitto-system/
│       └── mosquitto.conf
├── src/
└── pom.xml
```

### docker-compose.yml

```yaml
version: "3.8"

services:
  # ─────────────────────────────────────────────
  # 데이터 브로커: IoT 센서 데이터 수집용
  # MqttSubscriberNode, MqttPublisherNode가 연결하는 브로커
  # ─────────────────────────────────────────────
  mqtt-data-broker:
    image: eclipse-mosquitto:2.0
    container_name: fbp-data-broker
    ports:
      - "1883:1883"
    volumes:
      - ./mosquitto-data/mosquitto.conf:/mosquitto/config/mosquitto.conf
      - mqtt-data-data:/mosquitto/data
      - mqtt-data-log:/mosquitto/log
    restart: unless-stopped
    networks:
      - fbp-network

  # ─────────────────────────────────────────────
  # 시스템 브로커: FBP 엔진 내부 노드 간 메시지 전달용
  # MqttBridgeConnection이 사용하는 브로커
  # ─────────────────────────────────────────────
  mqtt-system-broker:
    image: eclipse-mosquitto:2.0
    container_name: fbp-system-broker
    ports:
      - "1884:1883"
    volumes:
      - ./mosquitto-system/mosquitto.conf:/mosquitto/config/mosquitto.conf
      - mqtt-system-data:/mosquitto/data
      - mqtt-system-log:/mosquitto/log
    restart: unless-stopped
    networks:
      - fbp-network

volumes:
  mqtt-data-data:
  mqtt-data-log:
  mqtt-system-data:
  mqtt-system-log:

networks:
  fbp-network:
    driver: bridge
```

### mosquitto-data/mosquitto.conf (데이터 브로커)

```
# 데이터 브로커 설정 — 포트 1883
listener 1883
allow_anonymous true
persistence true
persistence_location /mosquitto/data/
log_dest file /mosquitto/log/mosquitto.log
log_type all
```

### mosquitto-system/mosquitto.conf (시스템 브로커)

```
# 시스템 브로커 설정 — 컨테이너 내부 포트 1883, 호스트에서는 1884로 접근
listener 1883
allow_anonymous true
persistence true
persistence_location /mosquitto/data/
log_dest file /mosquitto/log/mosquitto.log
log_type all

# 시스템 브로커는 메시지 크기 제한을 넉넉하게 설정
# (직렬화된 FBP Message가 클 수 있음)
max_packet_size 1048576
```

### 실행 및 관리

```bash
# docker/ 디렉토리로 이동
cd docker

# 두 브로커 동시 시작 (백그라운드)
docker compose up -d

# 실행 상태 확인
docker compose ps
# NAME                 STATUS    PORTS
# fbp-data-broker      Up        0.0.0.0:1883->1883/tcp
# fbp-system-broker    Up        0.0.0.0:1884->1883/tcp

# 데이터 브로커 로그 확인
docker compose logs mqtt-data-broker

# 시스템 브로커 로그 확인
docker compose logs mqtt-system-broker

# 실시간 로그 (tail -f 방식)
docker compose logs -f
```

### 연결 테스트

브로커가 정상 동작하는지 `mosquitto_pub`/`mosquitto_sub` 명령으로 확인한다. (mosquitto-clients 설치 필요)

```bash
# --- 데이터 브로커 (1883) 테스트 ---
# 터미널 1: 구독
mosquitto_sub -h localhost -p 1883 -t "sensor/temp"

# 터미널 2: 발행
mosquitto_pub -h localhost -p 1883 -t "sensor/temp" -m '{"value":25.3}'
# → 터미널 1에서 메시지 수신 확인

# --- 시스템 브로커 (1884) 테스트 ---
# 터미널 1: 구독
mosquitto_sub -h localhost -p 1884 -t "fbp/test/nodeA.out-nodeB.in"

# 터미널 2: 발행
mosquitto_pub -h localhost -p 1884 -t "fbp/test/nodeA.out-nodeB.in" -m '{"v":1,"payload":{"data":"hello"}}'
# → 터미널 1에서 메시지 수신 확인
```

mosquitto-clients가 없으면 Docker 내부에서 직접 테스트할 수도 있다:

```bash
# 데이터 브로커 컨테이너에 접속하여 테스트
docker exec -it fbp-data-broker mosquitto_pub -t "test" -m "hello from data broker"
docker exec -it fbp-system-broker mosquitto_pub -t "test" -m "hello from system broker"
```

### Java 코드에서 브로커 접속

```java
// 데이터 브로커 — MqttSubscriberNode, MqttPublisherNode 용
String dataBrokerUrl = "tcp://localhost:1883";

// 시스템 브로커 — MqttBridgeConnection 용
String systemBrokerUrl = "tcp://localhost:1884";
```

플로우 정의 JSON에서의 사용:

```json
{
  "id": "temperature-monitoring",
  "transport": {
    "type": "mqtt",
    "broker": "tcp://localhost:1884",
    "qos": 1
  },
  "nodes": [
    {
      "id": "sensor",
      "type": "MqttSubscriber",
      "config": {
        "broker": "tcp://localhost:1883",
        "topic": "sensor/temp"
      }
    }
  ],
  "connections": [...]
}
```

`transport.broker`(시스템 브로커, 1884)와 노드 config의 `broker`(데이터 브로커, 1883)가 **서로 다른 주소**임에 주의한다.

### 종료 및 정리

```bash
# 두 브로커 정지
docker compose down

# 볼륨까지 포함하여 완전 삭제 (로그, 데이터 포함)
docker compose down -v
```

### 통합 테스트에서의 활용

JUnit 테스트에서 Docker 브로커를 자동으로 시작/종료하려면 **Testcontainers**를 활용할 수 있다.

```java
@Testcontainers
class MqttBridgeConnectionTest {

    @Container
    static GenericContainer<?> systemBroker = new GenericContainer<>("eclipse-mosquitto:2.0")
        .withExposedPorts(1883)
        .withCopyFileToContainer(
            MountableFile.forClasspathResource("mosquitto-system.conf"),
            "/mosquitto/config/mosquitto.conf"
        );

    static String systemBrokerUrl;

    @BeforeAll
    static void setUp() {
        systemBrokerUrl = "tcp://localhost:" + systemBroker.getMappedPort(1883);
    }

    @Test
    void testDeliverAndPoll() throws Exception {
        MqttBridgeConfig config = new MqttBridgeConfig(systemBrokerUrl, 1);
        MqttBridgeConnection conn = new MqttBridgeConnection(
            "conn-1", "fbp/test/a.out-b.in", config
        );
        // ...
    }
}
```

이렇게 하면 Docker가 설치된 환경에서 `mvn test`만으로 브로커 기동부터 테스트 실행, 정리까지 자동화된다.

---

## 9. 참고 자료

### MQTT

- Eclipse Paho MQTT v5 Client: [eclipse.org/paho](https://www.eclipse.org/paho/)
- MQTT 사양 v5.0: [docs.oasis-open.org](https://docs.oasis-open.org/mqtt/mqtt/v5.0/mqtt-v5.0.html)
- MQTT 토픽 네이밍 규칙: [HiveMQ Blog](https://www.hivemq.com/blog/mqtt-essentials-part-5-mqtt-topics-best-practices/)

### 설계 패턴

- Strategy Pattern — GoF *Design Patterns*
- Abstract Factory Pattern — 연결 타입에 따른 Connection 생성
- Observer Pattern — CLI 모니터링 (메시지 이벤트 구독)

### CLI 구현

- JLine 3: [github.com/jline/jline3](https://github.com/jline/jline3)
- Picocli (CLI 파서): [picocli.info](https://picocli.info/)

### 메시지 직렬화

- Jackson Databind: [github.com/FasterXML/jackson-databind](https://github.com/FasterXML/jackson-databind)
- MessagePack (고성능 바이너리): [msgpack.org](https://msgpack.org/)
