# 에이전트 공통 가이드라인 (인수인계 문서)

이 문서는 이 저장소에서 작업하는 모든 AI 에이전트(Claude, Grok, Codex 등)가 **공통으로** 지켜야 할 규칙을 정리한 인수인계 문서입니다.
`.claude/`, `.grok/`, `.codex/` 폴더의 에이전트별 문서는 이 문서를 기본으로 삼고, 도구별 특이사항만 추가로 기술합니다.

## 1. 저장소 개요

- Spring Boot 학습 기록 저장소입니다.
- `study0.md`, `study1.md`, `study2.md`, ... 순서로 학습 진도가 진행됩니다. (숫자가 클수록 최신 진도)
- `demo/`: study1~study10 내용을 실습하는 Spring Boot(Gradle) 프로젝트입니다.
- `project/client/`: studyFinal의 React 화면입니다. `demo` API를 Vite 프록시로 부릅니다.

## 2. 작업 전 확인 사항

- 새 작업을 시작하기 전에 `studyN.md` 파일들을 번호 순서대로 확인하여 지금까지의 학습 진도와 맥락을 파악합니다.
- `demo/` 프로젝트 코드가 study 문서 설명과 어긋나지 않는지 확인합니다.
- 새 studyN.md 문서를 추가/수정할 때는 이전 study 문서와 이어지는 흐름을 유지합니다.

## 3. 커밋 규칙

- 커밋 관련 규칙은 [COMMIT_GUIDE.md](./COMMIT_GUIDE.md) 문서를 따릅니다.

## 4. 언어 규칙

- 문서 내용, 커밋 설명: 한글
- 커밋 타입(prefix), 코드, 식별자, 파일명: 영어

## 5. 에이전트별 문서

- Claude: [.claude/CLAUDE.md](../.claude/CLAUDE.md)
- Grok: [.grok/GROK.md](../.grok/GROK.md)
- Codex: [.codex/AGENTS.md](../.codex/AGENTS.md)

## 6. 인수인계 원칙

- 작업을 마칠 때는 다음 에이전트/세션이 이어받을 수 있도록 변경 내용과 다음 단계 후보를 간단히 남깁니다.
- 불확실한 결정은 추측하지 말고 study 문서 내용을 근거로 삼습니다.
- 이 문서나 커밋 가이드에 없는 새로운 컨벤션이 필요해지면, 추측해서 각자 다르게 처리하지 말고 이 문서를 먼저 갱신합니다.
