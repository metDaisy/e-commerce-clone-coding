---
name: service-planning
description: "Business policy, 권한·오류 의미, 사용자 흐름 또는 UI 방향을 requirement에 반영하기 전에 사용자 결정이 필요할 때 사용한다."
version: 0.1.0
license: MIT
metadata:
  hermes:
    tags: [project-management, service-planning, requirements, user-decision]
    related_skills: [create-triage, sync-docs, build-task-graph]
---

# 서비스 기획

확정된 사실과 선택지를 근거와 함께 제시하고 사용자의 product decision을 받는다. PM은 추천할 수
있지만 policy를 확정하지 않는다.

## 사용 조건

- 새 기능·business policy·권한·공개 API 의미·오류 의미를 정해야 한다.
- 사용자 흐름, 화면 목적, 우선순위 또는 UX trade-off를 정해야 한다.
- Triage나 Review finding이 `decision-required`이고 source 조사만으로 해결할 수 없다.

단순한 source 위치·구현 범위 조사에는 사용하지 않는다.

## 절차

1. 승인 requirement와 fresh current-state에서 확정 사실, 관찰 사실과 미결정 질문을 분리한다.
2. 질문별 선택지를 좁히고 필요할 때만 공개 서비스와 신뢰 가능한 자료를 조사한다.
3. 각 선택지의 사용자 가치, 구현·운영 영향, 기존 requirement와의 차이를 기록하고 추천을 별도로 표시한다.
4. 사용자 결정을 받는다. 결정 전에는 Triage 또는 Decision card를 `blocked`로 유지한다.
5. 승인 결정을 requirement에 먼저 반영하고 read-back한다. 파생 문서나 Issue가 영향을 받으면 `sync-docs`로 넘긴다.
6. requirement와 외부 상태가 동기화된 뒤 원래 Triage 또는 graph 작업을 재개한다.

완료 기준: 결정, requirement locator와 read-back, 영향을 받은 문서/Issue 상태, 다음 transition이
분리되어 기록된다. 결정이 보류되면 blocker와 decision owner가 명시된다.

Source·test·migration을 구현하거나 승인 전 선택지를 확정 사실로 기록하지 않는다.
