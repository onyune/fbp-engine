# FBP Engine CLI 사용 가이드

이 문서는 FBP Engine CLI를 사용하여 플로우를 배포하고, 모니터링하며, 실시간으로 수정하는 모든 단계와 명령어에 대해 설명합니다.

## 1. 빌드 및 실행

먼저 프로젝트를 빌드하고 CLI 애플리케이션을 실행합니다.

```bash
mvn clean install
java -jar target/fbp-engine-1.0-SNAPSHOT.jar
```

---

## 2. 플로우 관리 (flow)

플로우의 생명주기와 전반적인 상태를 관리합니다.

### 플로우 목록 조회
현재 엔진에 등록된 모든 플로우의 상태를 확인합니다.
```
fbp> flow list
```

### 플로우 배포
JSON 정의 파일을 사용하여 새로운 플로우를 배포합니다.
```
fbp> flow deploy flow-deploy.json
```

### 플로우 상태 조회
특정 플로우의 상세 실행 정보(전송 방식, 처리량, 에러율, 리비전 등)를 확인합니다.
```
fbp> flow status flow-1
```

### 플로우 시작/정지/제거
```
fbp> flow start flow-1
fbp> flow stop flow-1
fbp> flow remove flow-1
```

### 동적 패치 (Flow Patch)
기존 플로우를 정지시키지 않고 새로운 정의 파일로 업데이트합니다.
```
fbp> flow patch flow-1 patch-file.json
```

---

## 3. 노드 관리 (node)

플로우 내의 개별 노드를 관리하고 상세 통계를 확인합니다.

### 노드 목록 조회
특정 플로우에 포함된 노드 리스트를 확인합니다.
```
fbp> node list flow-1
```

### 노드 상세 정보
노드의 타입, 설정(Config), 입출력 포트 정보를 확인합니다.
```
fbp> node info node-1
```

### 노드 통계 (Stats)
메시지 처리량(In/Out), 필터링 비율, 처리 지연 시간(Avg, P99)을 확인합니다.
```
fbp> node stats node-1
```

---

## 4. 연결 관리 (wire)

노드 간의 메시지 통로인 와이어(Wire)를 관리합니다.

### 연결 목록 조회
특정 플로우 내의 모든 연결 상태와 전송 방식을 확인합니다.
```
fbp> wire list flow-1
```

### 연결 상세 정보
특정 연결의 소스/타겟, 브로커 정보, 토픽, 큐 상태 및 누적 전송량을 확인합니다.
```
fbp> wire info w-1
```

---

## 5. 동적 플로우 수정

실행 중인 플로우에 노드를 추가하거나 설정을 변경할 수 있습니다.

### 노드 추가
플로우에 새로운 노드를 동적으로 추가합니다.
```
fbp> flow add-node flow-1 node-3:LogNode
```

### 연결 추가 (Wire 연결)
노드와 노드를 연결합니다.
```
fbp> flow add-wire flow-1 node-1:out node-3:in
```

### 노드 설정 변경
실행 중인 노드의 설정을 JSON 형식으로 즉시 변경합니다.
```
fbp> flow update-config flow-1 node-1 {"message":"Hello Dynamic!","intervalMs":"500"}
```

### 변경 이력 조회
해당 플로우의 모든 변경 리비전(Revision) 목록을 조회합니다.
```
fbp> flow history flow-1
```

### 롤백 (이전 버전 복구)
특정 리비전 번호로 플로우 상태를 되돌립니다.
```
fbp> flow rollback flow-1 0
```

---

## 6. 센서 및 도메인 통계 (sensor)

도메인 레벨에서 집계된 센서 데이터를 조회합니다.

### 센서 목록 조회
현재 수집 중인 센서 목록을 확인합니다.
```
fbp> sensor list
```

### 센서 통계 조회
특정 기간 및 윈도우 크기별 센서 통계(평균, 최소, 최대)를 확인합니다.
```
fbp> sensor stats temperature --window 1h --range 24h
```

---

## 7. 시스템 상태 및 모니터링 (stats, influx, broker)

### 전역 엔진 통계
엔진 전체의 처리량, 에러율, 메모리 사용량 등을 요약하여 보여줍니다.
```
fbp> stats
```

### InfluxDB 상태 조회
메트릭 저장소인 InfluxDB의 연결 상태와 배치 큐 적체량을 확인합니다.
```
fbp> influx status
```

### 브로커 상태 조회
MQTT 브로커의 연결 상태와 활성 토픽 수를 확인합니다.
```
fbp> broker status
```

### 실시간 모니터링 (Monitor)
플로우나 노드의 메시지 흐름을 실시간으로 추적합니다.
```
fbp> monitor flow flow-1
fbp> monitor node node-1
```
