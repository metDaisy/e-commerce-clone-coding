# Triage·Decision 협업과 안전한 Graph Activation

이 문서는 새 Issue의 **planning Decision**을 Triage와 함께 다루는 방식과, 승인 뒤 실행 graph를
fail-closed로 활성화하는 기준을 고정한다. persisted Triage schema는 `triage-contract.md`, executable
graph schema는 `build-task-graph/references/board-contract.md`, native lifecycle은 Hermes Kanban이 소유한다.

## 역할 분리

| 항목 | 역할 | Graph wrapper 포함 | scheduling parent |
|---|---|---:|---:|
| Triage | PM의 mutable planning record | 예 | 초기 Impl/Review의 parent |
| Planning Decision | 사용자와 PM의 policy 협의·승인 record | 아니오 | 아니오 |
| Impl / Review / Summary | 승인된 실행 계약 | 예 | 예 |
| Review-rework Decision | completed Review finding에서 발생한 후속 결정 | 예 | 필요 시 예 |

Planning Decision은 모든 Issue의 필수 card가 아니다. 요구사항과 실제 public contract가 충분히 명확하면
만들지 않는다. 반대로 authorization, consistency, error meaning, public cross-domain contract처럼 PM이나
Coder가 독자적으로 정하면 안 되는 질문이 있으면 Triage와 함께 하나를 만든다.

## Native lifecycle

```text
Triage (native triage)
  ├─ no decision: 조사 → freeze → graph materialize → Triage done → one ready
  └─ Decision 필요
       ├─ Decision blocked: 사용자 입력 요청
       ├─ 사용자가 comment 작성 + Decision running
       ├─ PM이 다음 workflow run에서 comment를 read-back하고 reply comment 작성
       └─ approved: Triage update/freeze → graph materialize
                       → Decision done + Triage done → one ready
```

- Triage는 planning 내내 native `triage` 상태를 유지한다. PM은 claim하거나 block하지 않는다.
- Planning Decision은 처음 `blocked`로 만들고 body에 질문, 선택지, evidence, `decision_owner: user`를
  기록한다.
- 사용자는 Decision card에 comment를 남긴 뒤 `running`으로 바꾼다. 이것은 **PM의 응답이 필요함**을
  나타내는 native signal이다.
- Kanban status change 자체가 PM을 자동 실행하지는 않는다. PM은 다음 workflow invocation에서 `running`
  Decision의 newest comment를 read-back한 뒤 comment로 응답한다.
- 의견이 충분하면 PM이 Decision body와 Triage의 동일 `decision_request_id`를 `approved`로 맞춘다.
  approved change, minimum capability, producer/consumer/integration follow-up도 둘에 일치해야 한다.
- required document/Issue synchronization을 read-back한 뒤에만 Triage를 freeze한다.

`running` Decision을 완료할 때 PM은 `--force`를 사용하지 않는다. live claim이 있으면 native complete가
실패하고, PM은 user/worker run을 강제로 닫지 않은 채 failure를 read-back하고 중단한다.

## Graph materialization과 activation gate

Decision이 approved된 뒤 PM은 Impl, Review, Summary를 모두 `todo`로 생성하고 native card/link/body를
read-back한다. 이 시점에는 아직 Coder card를 `ready`로 만들지 않는다.

`run-workflow/scripts/activate_graph.py`가 PM의 유일한 graph activation 경로다.

```text
1. graph draft validator
2. native graph read-back validator
3. Triage native status/body/frozen digest validator
4. 모든 Planning Decision의 running + approved + Triage alignment validator
5. candidate graph를 Triage done + target ready 상태로 검증
6. 모두 통과한 경우에만 native complete(Decision..., Triage)를 시도
7. 각 done status read-back
8. `recompute_ready()`가 target을 자동 ready로 만들었는지 확인하고, 아직 `todo`일 때만 target 하나를 promote
9. graph native phase와 native envelopes 재검증
```

기본 실행은 preflight-only다. `--apply`가 있을 때만 6~9를 수행한다. 어느 단계든 실패하면 뒤 mutation은
실행하지 않는다. 예를 들어 native `complete`가 실패하면 `promote`는 실행되지 않는다.

### Native Triage 종료 전이 — 2026-10-01 적용

Hermes core의 `complete`는 frozen native `triage` task도 non-empty result와 충족된 parent 조건에서
`done`으로 전이한다. 완료는 native event/run으로 기록되고 `recompute_ready()`가 후속 card의 readiness를
다시 계산한다. `activate_graph.py`는 이 전이를 사용해 Planning Decision과 Triage를 완료한 뒤 target 하나만
promotion한다.

이 전이는 PM graph activation의 명시적 종료 경로다. `specify`를 임시 우회로 사용하지 않는다. 이는 triage
specifier가 body를 자동 변경하고 `todo`로 promotion하는 별도 workflow이므로, PM이 승인한 frozen planning
record를 보존한다는 이 계약과 다르다.

```text
python .hermes/profiles/project-manager/skills/run-workflow/scripts/activate_graph.py \
  --board <board> --graph <graph.json> --native-readback <native-readback.json> \
  --triage-task-id <triage-id> --decision-task-id <decision-id> \
  --activation-key <impl-or-review-key>

# preflight report가 valid=true인 경우에만
python .hermes/profiles/project-manager/skills/run-workflow/scripts/activate_graph.py \
  --board <board> --graph <graph.json> --native-readback <native-readback.json> \
  --triage-task-id <triage-id> --decision-task-id <decision-id> \
  --activation-key <impl-or-review-key> --apply
```

Script는 CLI invocation을 `--profile project-manager`로 고정한다. 이는 PM workflow의 capability/routing
boundary다. 운영체제 ACL은 아니므로 profile directory와 Kanban 권한을 별도로 제한하지 않은 사용자가
직접 파일을 실행하는 것까지 막지는 않는다.

## Seller → Catalog와 Cart 예시

Seller → Catalog는 최소 producer capability의 예다.

```text
CatalogSellerAdapter → SellerQueryApi.isActiveSeller(userId)
```

Catalog가 필요한 사실은 “활성 Seller인지”뿐이다. 이를 실제로 제공하려면 Seller domain state,
application API, repository/JPA adapter가 함께 필요할 수 있다. 그러나 Seller의 모든 관리 기능이나 UI를
선행 구현할 필요는 없다.

같은 원리로 Cart가 다음을 요구한다고 가정한다.

| Cart 필요 사실 | owner | 최소 capability |
|---|---|---|
| 가격, 판매 상태, 구매 제한, 가용 재고, unavailable 의미 | Offer/P9 | Offer-owned public query/projection |
| 상품명, thumbnail, variant 표시 정보 | Catalog/P2 | Catalog-owned display query/projection |
| 장바구니 상태와 수량 규칙 | Cart | Cart-owned domain/application behavior |

Triage는 P9/P2 capability가 실제 public seam으로 published인지, 아니면 `absent-or-partial`인지를
기록한다. 후자이고 error/consistency/public response shape를 정해야 하면 Planning Decision을 만든다.
승인 뒤 graph에는 producer bridge, Cart consumer adapter, integration verification을 필요한 만큼만
materialize한다. Planning validator는 ownership과 follow-up completeness를 확인하고, Review/integration
단계는 실제 source seam과 tests를 확인한다.

## 완료 조건

Planning Decision이 있는 Issue는 다음이 모두 참일 때만 activation할 수 있다.

- Triage는 frozen이며 native `triage` 상태다.
- 모든 linked Planning Decision은 native `running` 또는 이미 성공적으로 완료된 `done`이고, body
  `approved`이며 Triage request와 정합하다. `done`은 terminal successful run을 read-back해야 한다.
- 실행 graph에는 Planning Decision을 card/parent로 복제하지 않는다.
- Impl/Review/Summary의 body, parent, assignee, status가 native read-back과 일치한다.
- `activate_graph.py` preflight가 valid다.
- `--apply` 뒤 Triage/Decision은 `done`, 정확히 한 Impl 또는 Review만 `ready`다. activation 중 이미
  완료된 Decision은 successful run과 approved payload가 정합하면 재시도에서 다시 complete하지 않는다.
